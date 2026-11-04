package com.example.data.model

data class TreeWithDetails(
  val tree: TreeEntity,
  val nodes: List<NodeEntity>,
  val connections: List<ConnectionEntity>
) {
  val nodeMap: Map<String, NodeEntity> by lazy {
    nodes.associateBy { it.id }
  }

  val incomingConnections: Map<String, ConnectionEntity> by lazy {
    connections.associateBy { it.toNodeId }
  }

  val outgoingConnections: Map<String, List<ConnectionEntity>> by lazy {
    connections.groupBy { it.fromNodeId }
  }

  val rootNode: NodeEntity?
    get() = nodes.find { it.isRoot } ?: nodes.find { it.id == tree.rootNodeId }

  fun getChildren(nodeId: String): List<NodeEntity> {
    val childIds = outgoingConnections[nodeId]?.map { it.toNodeId } ?: emptyList()
    return childIds.mapNotNull { nodeMap[it] }
  }

  fun getParentConnection(nodeId: String): ConnectionEntity? {
    return incomingConnections[nodeId]
  }

  fun getParentNode(nodeId: String): NodeEntity? {
    val parentId = incomingConnections[nodeId]?.fromNodeId ?: return null
    return nodeMap[parentId]
  }

  /**
   * Returns all descendant node IDs recursively for a given node.
   */
  fun getAllDescendantIds(nodeId: String): Set<String> {
    val result = mutableSetOf<String>()
    val queue = ArrayDeque<String>()
    queue.add(nodeId)
    while (queue.isNotEmpty()) {
      val curr = queue.removeFirst()
      val children = outgoingConnections[curr]?.map { it.toNodeId } ?: emptyList()
      for (child in children) {
        if (result.add(child)) {
          queue.add(child)
        }
      }
    }
    return result
  }

  /**
   * Returns the set of collapsed or hidden descendant node IDs.
   * If a node is collapsed, all its descendants are hidden.
   */
  fun getHiddenNodeIds(): Set<String> {
    val hidden = mutableSetOf<String>()
    for (node in nodes) {
      if (node.isCollapsed) {
        hidden.addAll(getAllDescendantIds(node.id))
      }
    }
    return hidden
  }
}
