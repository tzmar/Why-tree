package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.BranchDirection
import com.example.data.model.ConnectionEntity
import com.example.data.model.NodeEntity
import com.example.data.model.RelationshipType
import com.example.data.model.TreeEntity
import com.example.data.model.TreeWithDetails
import com.example.domain.layout.TreeLayoutEngine
import com.example.domain.undo.UndoAction
import com.example.domain.undo.UndoRedoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class TreeRepository(
  private val database: AppDatabase,
  val undoRedoManager: UndoRedoManager = UndoRedoManager()
) {
  private val treeDao = database.treeDao()
  private val nodeDao = database.nodeDao()
  private val connectionDao = database.connectionDao()

  val activeTrees: Flow<List<TreeEntity>> = treeDao.getAllActiveTrees()
  val trashTrees: Flow<List<TreeEntity>> = treeDao.getTrashTrees()

  fun getTreeDetailsFlow(treeId: String): Flow<TreeWithDetails?> {
    val treeFlow = treeDao.getTreeByIdFlow(treeId)
    val nodesFlow = nodeDao.getNodesForTree(treeId)
    val connectionsFlow = connectionDao.getConnectionsForTree(treeId)

    return combine(treeFlow, nodesFlow, connectionsFlow) { tree, nodes, connections ->
      if (tree == null) null
      else TreeWithDetails(tree, nodes, connections)
    }
  }

  suspend fun cleanExpiredTrash() = withContext(Dispatchers.IO) {
    val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
    treeDao.deleteExpiredTrash(thirtyDaysAgo)
  }

  /**
   * Creates a new tree with a centered root node.
   */
  suspend fun createTree(
    title: String,
    rootQuestion: String,
    description: String = ""
  ): String = withContext(Dispatchers.IO) {
    val treeId = UUID.randomUUID().toString()
    val rootNodeId = UUID.randomUUID().toString()
    val now = System.currentTimeMillis()

    val rootNode = NodeEntity(
      id = rootNodeId,
      treeId = treeId,
      text = if (rootQuestion.isNotBlank()) rootQuestion else title,
      notes = "",
      posX = 0f,
      posY = 0f,
      isRoot = true,
      isCollapsed = false,
      createdAt = now,
      updatedAt = now
    )

    val tree = TreeEntity(
      id = treeId,
      title = title,
      description = description,
      rootNodeId = rootNodeId,
      createdAt = now,
      updatedAt = now,
      zoom = 1.0f,
      panX = 0f,
      panY = 0f
    )

    treeDao.insertTree(tree)
    nodeDao.insertNode(rootNode)
    treeId
  }

  /**
   * Adds a child branch to a parent node with layout calculation and collision avoidance.
   */
  suspend fun addChildBranch(
    treeId: String,
    parentId: String,
    relationshipType: RelationshipType,
    customLabel: String,
    text: String,
    notes: String = "",
    direction: BranchDirection = BranchDirection.RIGHT
  ): String = withContext(Dispatchers.IO) {
    val parentNode = nodeDao.getNodeById(parentId) ?: return@withContext ""
    val existingNodes = nodeDao.getNodesListForTree(treeId)
    val existingConns = connectionDao.getConnectionsListForTree(treeId)

    val siblingCount = existingConns.count { it.fromNodeId == parentId && it.direction == direction.name }
    val (posX, posY) = TreeLayoutEngine.calculateNewChildPosition(
      parent = parentNode,
      direction = direction,
      existingNodes = existingNodes,
      siblingIndex = siblingCount
    )

    val childNodeId = UUID.randomUUID().toString()
    val connectionId = UUID.randomUUID().toString()
    val now = System.currentTimeMillis()

    val childNode = NodeEntity(
      id = childNodeId,
      treeId = treeId,
      text = text,
      notes = notes,
      posX = posX,
      posY = posY,
      isRoot = false,
      isCollapsed = false,
      createdAt = now,
      updatedAt = now
    )

    val connection = ConnectionEntity(
      id = connectionId,
      treeId = treeId,
      fromNodeId = parentId,
      toNodeId = childNodeId,
      relationshipType = relationshipType.name,
      customLabel = customLabel,
      direction = direction.name,
      createdAt = now
    )

    nodeDao.insertNode(childNode)
    connectionDao.insertConnection(connection)

    // Touch tree timestamp
    treeDao.getTreeById(treeId)?.let {
      treeDao.updateTree(it.copy(updatedAt = now))
    }

    undoRedoManager.record(UndoAction.NodeAdded(childNode, connection))
    childNodeId
  }

  suspend fun updateNode(
    nodeId: String,
    text: String,
    notes: String,
    colorTag: String = ""
  ) = withContext(Dispatchers.IO) {
    val node = nodeDao.getNodeById(nodeId) ?: return@withContext
    val oldText = node.text
    val oldNotes = node.notes
    val now = System.currentTimeMillis()

    val updated = node.copy(
      text = text,
      notes = notes,
      colorTag = colorTag,
      updatedAt = now
    )
    nodeDao.updateNode(updated)

    treeDao.getTreeById(node.treeId)?.let {
      treeDao.updateTree(it.copy(updatedAt = now))
    }

    undoRedoManager.record(UndoAction.NodeEdited(nodeId, oldText, oldNotes, text, notes))
  }

  suspend fun moveNode(
    nodeId: String,
    newX: Float,
    newY: Float,
    recordUndo: Boolean = true
  ) = withContext(Dispatchers.IO) {
    val node = nodeDao.getNodeById(nodeId) ?: return@withContext
    val oldX = node.posX
    val oldY = node.posY

    nodeDao.updateNodePosition(nodeId, newX, newY)

    if (recordUndo) {
      undoRedoManager.record(UndoAction.NodeMoved(nodeId, oldX, oldY, newX, newY))
    }
  }

  suspend fun setNodeCollapsed(nodeId: String, isCollapsed: Boolean) = withContext(Dispatchers.IO) {
    nodeDao.setNodeCollapsed(nodeId, isCollapsed)
  }

  suspend fun updateConnection(
    connectionId: String,
    relationshipType: RelationshipType,
    customLabel: String,
    direction: BranchDirection? = null
  ) = withContext(Dispatchers.IO) {
    val conn = connectionDao.getConnectionById(connectionId) ?: return@withContext
    val oldRel = conn.relationshipType
    val oldCustom = conn.customLabel

    val updated = conn.copy(
      relationshipType = relationshipType.name,
      customLabel = customLabel,
      direction = direction?.name ?: conn.direction
    )
    connectionDao.updateConnection(updated)
    undoRedoManager.record(
      UndoAction.RelationshipChanged(
        connectionId,
        oldRel,
        oldCustom,
        relationshipType.name,
        customLabel
      )
    )
  }

  /**
   * Deletes a node.
   * If [deleteSubtree] is true, deletes the node and all its descendants.
   * If false, reparents direct children to this node's parent (or root).
   */
  suspend fun deleteNode(
    treeId: String,
    nodeId: String,
    deleteSubtree: Boolean
  ) = withContext(Dispatchers.IO) {
    val treeDetails = getTreeSnapshot(treeId) ?: return@withContext
    val targetNode = treeDetails.nodeMap[nodeId] ?: return@withContext
    if (targetNode.isRoot) return@withContext // Cannot delete root node

    if (deleteSubtree) {
      val descendantIds = treeDetails.getAllDescendantIds(nodeId) + nodeId
      val nodesToDelete = descendantIds.mapNotNull { treeDetails.nodeMap[it] }
      val connectionsToDelete = treeDetails.connections.filter {
        descendantIds.contains(it.fromNodeId) || descendantIds.contains(it.toNodeId)
      }

      connectionDao.deleteConnectionsForNodes(descendantIds.toList())
      nodeDao.deleteNodes(descendantIds.toList())

      undoRedoManager.record(UndoAction.NodeDeleted(nodesToDelete, connectionsToDelete))
    } else {
      // Reparent children to this node's parent
      val parentConnection = treeDetails.incomingConnections[nodeId]
      val parentId = parentConnection?.fromNodeId ?: treeDetails.tree.rootNodeId
      val childrenConns = treeDetails.outgoingConnections[nodeId] ?: emptyList()

      // Reconnect children to parentId
      for (conn in childrenConns) {
        connectionDao.updateConnection(conn.copy(fromNodeId = parentId))
      }

      val incomingList = listOfNotNull(parentConnection)
      if (parentConnection != null) {
        connectionDao.deleteConnection(parentConnection.id)
      }
      nodeDao.deleteNode(nodeId)

      undoRedoManager.record(
        UndoAction.NodeDeleted(
          listOf(targetNode),
          incomingList
        )
      )
    }
  }

  suspend fun duplicateNode(treeId: String, nodeId: String) = withContext(Dispatchers.IO) {
    val treeDetails = getTreeSnapshot(treeId) ?: return@withContext
    val sourceNode = treeDetails.nodeMap[nodeId] ?: return@withContext
    val incomingConn = treeDetails.incomingConnections[nodeId]
    val parentId = incomingConn?.fromNodeId ?: treeDetails.tree.rootNodeId

    addChildBranch(
      treeId = treeId,
      parentId = parentId,
      relationshipType = incomingConn?.relType ?: RelationshipType.WHY,
      customLabel = incomingConn?.customLabel ?: "",
      text = "${sourceNode.text} (Copy)",
      notes = sourceNode.notes,
      direction = incomingConn?.branchDir ?: BranchDirection.RIGHT
    )
  }

  suspend fun autoArrange(treeId: String) = withContext(Dispatchers.IO) {
    val details = getTreeSnapshot(treeId) ?: return@withContext
    val oldPositions = details.nodes.associate { it.id to Pair(it.posX, it.posY) }
    val newPositions = TreeLayoutEngine.autoArrangeTree(details)

    for ((nodeId, pos) in newPositions) {
      nodeDao.updateNodePosition(nodeId, pos.first, pos.second)
    }

    undoRedoManager.record(UndoAction.TreeRearranged(oldPositions, newPositions))
  }

  suspend fun updateViewport(treeId: String, zoom: Float, panX: Float, panY: Float) = withContext(Dispatchers.IO) {
    treeDao.updateViewport(treeId, zoom, panX, panY)
  }

  suspend fun moveToTrash(treeId: String) = withContext(Dispatchers.IO) {
    treeDao.moveToTrash(treeId)
  }

  suspend fun restoreFromTrash(treeId: String) = withContext(Dispatchers.IO) {
    treeDao.restoreFromTrash(treeId)
  }

  suspend fun deletePermanently(treeId: String) = withContext(Dispatchers.IO) {
    connectionDao.deleteConnectionsForTree(treeId)
    nodeDao.deleteNodesForTree(treeId)
    treeDao.deletePermanently(treeId)
  }

  suspend fun updateTreeMetadata(treeId: String, title: String, description: String) = withContext(Dispatchers.IO) {
    treeDao.getTreeById(treeId)?.let {
      treeDao.updateTree(it.copy(title = title, description = description, updatedAt = System.currentTimeMillis()))
    }
  }

  suspend fun duplicateTree(treeId: String): String = withContext(Dispatchers.IO) {
    val details = getTreeSnapshot(treeId) ?: return@withContext ""
    val newTreeId = UUID.randomUUID().toString()
    val idMap = mutableMapOf<String, String>()

    // Map each old node ID to new ID
    for (node in details.nodes) {
      idMap[node.id] = UUID.randomUUID().toString()
    }

    val newRootId = idMap[details.tree.rootNodeId] ?: UUID.randomUUID().toString()
    val now = System.currentTimeMillis()

    val newTree = details.tree.copy(
      id = newTreeId,
      title = "${details.tree.title} (Copy)",
      rootNodeId = newRootId,
      createdAt = now,
      updatedAt = now,
      isDeleted = false,
      deletedAt = null
    )

    val newNodes = details.nodes.map { oldNode ->
      oldNode.copy(
        id = idMap[oldNode.id] ?: UUID.randomUUID().toString(),
        treeId = newTreeId,
        createdAt = now,
        updatedAt = now
      )
    }

    val newConnections = details.connections.map { oldConn ->
      oldConn.copy(
        id = UUID.randomUUID().toString(),
        treeId = newTreeId,
        fromNodeId = idMap[oldConn.fromNodeId] ?: oldConn.fromNodeId,
        toNodeId = idMap[oldConn.toNodeId] ?: oldConn.toNodeId,
        createdAt = now
      )
    }

    treeDao.insertTree(newTree)
    nodeDao.insertNodes(newNodes)
    connectionDao.insertConnections(newConnections)

    newTreeId
  }

  suspend fun undo(treeId: String) = withContext(Dispatchers.IO) {
    val action = undoRedoManager.popUndo() ?: return@withContext
    when (action) {
      is UndoAction.NodeAdded -> {
        action.connection?.let { connectionDao.deleteConnection(it.id) }
        nodeDao.deleteNode(action.node.id)
      }
      is UndoAction.NodeMoved -> {
        nodeDao.updateNodePosition(action.nodeId, action.oldX, action.oldY)
      }
      is UndoAction.NodeEdited -> {
        nodeDao.getNodeById(action.nodeId)?.let {
          nodeDao.updateNode(it.copy(text = action.oldText, notes = action.oldNotes))
        }
      }
      is UndoAction.NodeDeleted -> {
        nodeDao.insertNodes(action.deletedNodes)
        connectionDao.insertConnections(action.deletedConnections)
      }
      is UndoAction.RelationshipChanged -> {
        connectionDao.updateRelationship(action.connectionId, action.oldRel, action.oldCustom)
      }
      is UndoAction.TreeRearranged -> {
        for ((nodeId, pos) in action.oldPositions) {
          nodeDao.updateNodePosition(nodeId, pos.first, pos.second)
        }
      }
    }
  }

  suspend fun redo(treeId: String) = withContext(Dispatchers.IO) {
    val action = undoRedoManager.popRedo() ?: return@withContext
    when (action) {
      is UndoAction.NodeAdded -> {
        nodeDao.insertNode(action.node)
        action.connection?.let { connectionDao.insertConnection(it) }
      }
      is UndoAction.NodeMoved -> {
        nodeDao.updateNodePosition(action.nodeId, action.newX, action.newY)
      }
      is UndoAction.NodeEdited -> {
        nodeDao.getNodeById(action.nodeId)?.let {
          nodeDao.updateNode(it.copy(text = action.newText, notes = action.newNotes))
        }
      }
      is UndoAction.NodeDeleted -> {
        val ids = action.deletedNodes.map { it.id }
        connectionDao.deleteConnectionsForNodes(ids)
        nodeDao.deleteNodes(ids)
      }
      is UndoAction.RelationshipChanged -> {
        connectionDao.updateRelationship(action.connectionId, action.newRel, action.newCustom)
      }
      is UndoAction.TreeRearranged -> {
        for ((nodeId, pos) in action.newPositions) {
          nodeDao.updateNodePosition(nodeId, pos.first, pos.second)
        }
      }
    }
  }

  private suspend fun getTreeSnapshot(treeId: String): TreeWithDetails? {
    val tree = treeDao.getTreeById(treeId) ?: return null
    val nodes = nodeDao.getNodesListForTree(treeId)
    val connections = connectionDao.getConnectionsListForTree(treeId)
    return TreeWithDetails(tree, nodes, connections)
  }

  // --- Export and Import (.why JSON format) ---

  suspend fun exportToJson(treeId: String): String = withContext(Dispatchers.IO) {
    val details = getTreeSnapshot(treeId) ?: return@withContext "{}"
    val rootObj = JSONObject()
    rootObj.put("format", "whytree")
    rootObj.put("version", 1)
    rootObj.put("exportedAt", System.currentTimeMillis())

    val treeObj = JSONObject()
    treeObj.put("title", details.tree.title)
    treeObj.put("description", details.tree.description)
    treeObj.put("rootNodeId", details.tree.rootNodeId)
    treeObj.put("zoom", details.tree.zoom.toDouble())
    treeObj.put("panX", details.tree.panX.toDouble())
    treeObj.put("panY", details.tree.panY.toDouble())
    rootObj.put("tree", treeObj)

    val nodesArray = JSONArray()
    for (node in details.nodes) {
      val n = JSONObject()
      n.put("id", node.id)
      n.put("text", node.text)
      n.put("notes", node.notes)
      n.put("posX", node.posX.toDouble())
      n.put("posY", node.posY.toDouble())
      n.put("isRoot", node.isRoot)
      n.put("isCollapsed", node.isCollapsed)
      n.put("colorTag", node.colorTag)
      nodesArray.put(n)
    }
    rootObj.put("nodes", nodesArray)

    val connsArray = JSONArray()
    for (conn in details.connections) {
      val c = JSONObject()
      c.put("id", conn.id)
      c.put("fromNodeId", conn.fromNodeId)
      c.put("toNodeId", conn.toNodeId)
      c.put("relationshipType", conn.relationshipType)
      c.put("customLabel", conn.customLabel)
      c.put("direction", conn.direction)
      connsArray.put(c)
    }
    rootObj.put("connections", connsArray)

    rootObj.toString(2)
  }

  suspend fun importFromJson(jsonString: String): Result<String> = withContext(Dispatchers.IO) {
    try {
      val rootObj = JSONObject(jsonString)
      val format = rootObj.optString("format", "")
      if (format != "whytree") {
        return@withContext Result.failure(IllegalArgumentException("Invalid file format. Expected a .why file."))
      }

      val treeObj = rootObj.getJSONObject("tree")
      val rawTitle = treeObj.optString("title", "Imported Tree")
      val description = treeObj.optString("description", "")
      val oldRootNodeId = treeObj.optString("rootNodeId", "")

      val nodesArray = rootObj.getJSONArray("nodes")
      val connsArray = rootObj.getJSONArray("connections")

      val newTreeId = UUID.randomUUID().toString()
      val idMap = mutableMapOf<String, String>()

      for (i in 0 until nodesArray.length()) {
        val n = nodesArray.getJSONObject(i)
        val oldId = n.getString("id")
        idMap[oldId] = UUID.randomUUID().toString()
      }

      val newRootId = idMap[oldRootNodeId] ?: UUID.randomUUID().toString()
      val now = System.currentTimeMillis()

      val newTree = TreeEntity(
        id = newTreeId,
        title = "$rawTitle (Imported)",
        description = description,
        rootNodeId = newRootId,
        createdAt = now,
        updatedAt = now,
        zoom = treeObj.optDouble("zoom", 1.0).toFloat(),
        panX = treeObj.optDouble("panX", 0.0).toFloat(),
        panY = treeObj.optDouble("panY", 0.0).toFloat()
      )

      val newNodes = mutableListOf<NodeEntity>()
      for (i in 0 until nodesArray.length()) {
        val n = nodesArray.getJSONObject(i)
        val oldId = n.getString("id")
        val newId = idMap[oldId] ?: UUID.randomUUID().toString()
        newNodes.add(
          NodeEntity(
            id = newId,
            treeId = newTreeId,
            text = n.optString("text", "Node"),
            notes = n.optString("notes", ""),
            posX = n.optDouble("posX", 0.0).toFloat(),
            posY = n.optDouble("posY", 0.0).toFloat(),
            isRoot = n.optBoolean("isRoot", false),
            isCollapsed = n.optBoolean("isCollapsed", false),
            colorTag = n.optString("colorTag", ""),
            createdAt = now,
            updatedAt = now
          )
        )
      }

      val newConnections = mutableListOf<ConnectionEntity>()
      for (i in 0 until connsArray.length()) {
        val c = connsArray.getJSONObject(i)
        val oldFrom = c.getString("fromNodeId")
        val oldTo = c.getString("toNodeId")
        val newFrom = idMap[oldFrom] ?: continue
        val newTo = idMap[oldTo] ?: continue

        newConnections.add(
          ConnectionEntity(
            id = UUID.randomUUID().toString(),
            treeId = newTreeId,
            fromNodeId = newFrom,
            toNodeId = newTo,
            relationshipType = c.optString("relationshipType", "WHY"),
            customLabel = c.optString("customLabel", ""),
            direction = c.optString("direction", "RIGHT"),
            createdAt = now
          )
        )
      }

      treeDao.insertTree(newTree)
      nodeDao.insertNodes(newNodes)
      connectionDao.insertConnections(newConnections)

      Result.success(newTreeId)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Generates a clean Markdown outline representing the tree hierarchy.
   */
  suspend fun exportToMarkdown(treeId: String): String = withContext(Dispatchers.IO) {
    val details = getTreeSnapshot(treeId) ?: return@withContext ""
    val root = details.rootNode ?: return@withContext ""
    val sb = StringBuilder()
    sb.append("# ${details.tree.title}\n\n")
    if (details.tree.description.isNotBlank()) {
      sb.append("> ${details.tree.description}\n\n")
    }

    fun renderBranch(nodeId: String, depth: Int) {
      val node = details.nodeMap[nodeId] ?: return
      val indent = "  ".repeat(depth)
      val incoming = details.incomingConnections[nodeId]
      val relTag = if (incoming != null) "[${incoming.displayLabel}] " else ""
      sb.append("$indent- $relTag**${node.text}**\n")
      if (node.notes.isNotBlank()) {
        sb.append("$indent  _${node.notes}_\n")
      }
      val children = details.outgoingConnections[nodeId]?.map { it.toNodeId } ?: emptyList()
      for (childId in children) {
        renderBranch(childId, depth + 1)
      }
    }

    renderBranch(root.id, 0)
    sb.toString()
  }

  /**
   * Seeds rich starter trees if database is empty so user immediately experiences
   * WHY TREE's power on first launch.
   */
  suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
    val existing = nodeDao.getNodesListForTree("dummy")
    // check if any trees exist
    // Using a fast query or check
    val trees = treeDao.getTreeById("seed_check")
    // Let's check count of active trees
    val activeSnapshot = treeDao.getTreeById("seed_customer_churn")
    if (activeSnapshot != null) return@withContext // already seeded

    // 1. "Customer Churn Spike (5 Whys Analysis)"
    val churnTreeId = "seed_customer_churn"
    val churnRootId = "churn_root"
    val now = System.currentTimeMillis()

    val churnTree = TreeEntity(
      id = churnTreeId,
      title = "5 Whys: Customer Churn Spike",
      description = "Root-cause investigation into Q3 enterprise customer retention decline",
      rootNodeId = churnRootId,
      createdAt = now,
      updatedAt = now,
      zoom = 0.95f,
      panX = 0f,
      panY = 0f
    )

    val n0 = NodeEntity(churnRootId, churnTreeId, "Why is Q3 customer churn up 24%?", "Customer survey reports higher cancellation rate", 0f, 0f, isRoot = true, isCollapsed = false, createdAt = now, updatedAt = now)
    
    // Level 1: Why 1 (Right) & How (Down)
    val n1 = NodeEntity("churn_w1", churnTreeId, "Onboarding completion dropped from 85% to 52%", "Users abandon setup within first 48 hours", 330f, -80f, createdAt = now, updatedAt = now)
    val n1b = NodeEntity("churn_e1", churnTreeId, "NPS dropped 18 points among new signups", "CSAT tickets cite confusion in account provisioning", 330f, 100f, createdAt = now, updatedAt = now)
    
    // Level 2: Why 2
    val n2 = NodeEntity("churn_w2", churnTreeId, "New auth flow requires mandatory SSO configuration", "Small teams don't have Google Workspace admin access", 660f, -80f, createdAt = now, updatedAt = now)
    
    // Level 3: Why 3
    val n3 = NodeEntity("churn_w3", churnTreeId, "Security update made SSO default without fallback email login", "Shipped in v2.4 sprint without user toggle", 990f, -80f, createdAt = now, updatedAt = now)
    
    // Level 4: Why 4
    val n4 = NodeEntity("churn_w4", churnTreeId, "Product spec prioritized Enterprise compliance over self-serve UX", "Targeted Fortune 500 checklist requirements", 1320f, -80f, createdAt = now, updatedAt = now)

    // Level 5: Why 5 & Result
    val n5 = NodeEntity("churn_w5", churnTreeId, "Root Cause: No tiered onboarding for SMB vs Enterprise tiers", "A single rigid onboarding path was enforced for all plan levels", 1650f, -80f, createdAt = now, updatedAt = now)
    val nResult = NodeEntity("churn_res", churnTreeId, "Action: Add graceful email invite fallback & split onboarding flows", "Expected retention recovery: +15%", 1650f, 80f, createdAt = now, updatedAt = now)

    val churnNodes = listOf(n0, n1, n1b, n2, n3, n4, n5, nResult)

    val c1 = ConnectionEntity("c1", churnTreeId, churnRootId, "churn_w1", "WHY", "", "RIGHT", now)
    val c1b = ConnectionEntity("c1b", churnTreeId, churnRootId, "churn_e1", "EVIDENCE", "", "RIGHT", now)
    val c2 = ConnectionEntity("c2", churnTreeId, "churn_w1", "churn_w2", "WHY", "", "RIGHT", now)
    val c3 = ConnectionEntity("c3", churnTreeId, "churn_w2", "churn_w3", "WHY", "", "RIGHT", now)
    val c4 = ConnectionEntity("c4", churnTreeId, "churn_w3", "churn_w4", "WHY", "", "RIGHT", now)
    val c5 = ConnectionEntity("c5", churnTreeId, "churn_w4", "churn_w5", "WHY", "ROOT CAUSE", "RIGHT", now)
    val cRes = ConnectionEntity("cRes", churnTreeId, "churn_w5", "churn_res", "RESULT", "SOLUTION", "DOWN", now)

    val churnConns = listOf(c1, c1b, c2, c3, c4, c5, cRes)

    // 2. "Why Learn Jetpack Compose & Offline-First?"
    val learnTreeId = "seed_compose_offline"
    val learnRootId = "learn_root"
    val learnTree = TreeEntity(
      id = learnTreeId,
      title = "Modern Android Architecture Decisions",
      description = "Evaluating offline-first local Room storage and declarative Compose",
      rootNodeId = learnRootId,
      createdAt = now + 1,
      updatedAt = now + 1,
      zoom = 1.0f,
      panX = 0f,
      panY = 0f
    )

    val l0 = NodeEntity(learnRootId, learnTreeId, "Why build offline-first with Jetpack Compose?", "Core architectural philosophy", 0f, 0f, isRoot = true, isCollapsed = false, createdAt = now, updatedAt = now)
    val l1 = NodeEntity("l1", learnTreeId, "Instant UI response with zero network latency", "Room SQLite delivers sub-millisecond local reads", 330f, -100f, createdAt = now, updatedAt = now)
    val l2 = NodeEntity("l2", learnTreeId, "Declarative state automatically reconciles canvas transforms", "No messy manual view hierarchies or canvas invalidations", 330f, 100f, createdAt = now, updatedAt = now)
    val l3 = NodeEntity("l3", learnTreeId, "User data privacy and total ownership", "Data never leaves device unless explicitly exported", 660f, -100f, createdAt = now, updatedAt = now)
    val l4 = NodeEntity("l4", learnTreeId, "Custom gesture modifiers: zoom, pan, drag-and-drop", "Fluid 60fps interactive touch canvas", 660f, 100f, createdAt = now, updatedAt = now)

    val learnNodes = listOf(l0, l1, l2, l3, l4)

    val lc1 = ConnectionEntity("lc1", learnTreeId, learnRootId, "l1", "WHY", "", "RIGHT", now)
    val lc2 = ConnectionEntity("lc2", learnTreeId, learnRootId, "l2", "HOW", "", "RIGHT", now)
    val lc3 = ConnectionEntity("lc3", learnTreeId, "l1", "l3", "RESULT", "", "RIGHT", now)
    val lc4 = ConnectionEntity("lc4", learnTreeId, "l2", "l4", "EVIDENCE", "", "RIGHT", now)

    val learnConns = listOf(lc1, lc2, lc3, lc4)

    treeDao.insertTree(churnTree)
    nodeDao.insertNodes(churnNodes)
    connectionDao.insertConnections(churnConns)

    treeDao.insertTree(learnTree)
    nodeDao.insertNodes(learnNodes)
    connectionDao.insertConnections(learnConns)
  }
}
