package com.example.domain.layout

import com.example.data.model.BranchDirection
import com.example.data.model.ConnectionEntity
import com.example.data.model.NodeEntity
import com.example.data.model.TreeWithDetails
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object TreeLayoutEngine {

  const val CARD_WIDTH = 240f
  const val CARD_HEIGHT = 110f
  const val HORIZONTAL_GAP = 90f
  const val VERTICAL_GAP = 50f

  data class RectF(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun intersects(other: RectF): Boolean {
      return left < other.right && right > other.left && top < other.bottom && bottom > other.top
    }
  }

  fun getNodeBounds(node: NodeEntity): RectF {
    return RectF(
      left = node.posX,
      top = node.posY,
      right = node.posX + CARD_WIDTH,
      bottom = node.posY + CARD_HEIGHT
    )
  }

  /**
   * Suggests a collision-free position for a new child node based on parent position,
   * branch direction, and existing nodes.
   */
  fun calculateNewChildPosition(
    parent: NodeEntity,
    direction: BranchDirection,
    existingNodes: List<NodeEntity>,
    siblingIndex: Int = 0
  ): Pair<Float, Float> {
    val baseDistanceX = CARD_WIDTH + HORIZONTAL_GAP
    val baseDistanceY = CARD_HEIGHT + VERTICAL_GAP

    var targetX: Float
    var targetY: Float

    when (direction) {
      BranchDirection.RIGHT -> {
        targetX = parent.posX + baseDistanceX
        targetY = parent.posY + (siblingIndex * (CARD_HEIGHT + VERTICAL_GAP))
      }
      BranchDirection.LEFT -> {
        targetX = parent.posX - baseDistanceX
        targetY = parent.posY + (siblingIndex * (CARD_HEIGHT + VERTICAL_GAP))
      }
      BranchDirection.DOWN -> {
        targetX = parent.posX + (siblingIndex * (CARD_WIDTH + HORIZONTAL_GAP))
        targetY = parent.posY + baseDistanceY
      }
      BranchDirection.UP -> {
        targetX = parent.posX + (siblingIndex * (CARD_WIDTH + HORIZONTAL_GAP))
        targetY = parent.posY - baseDistanceY
      }
    }

    // Collision avoidance: nudge position until it doesn't overlap existing nodes
    var candidate = RectF(targetX, targetY, targetX + CARD_WIDTH, targetY + CARD_HEIGHT)
    var attempts = 0
    val maxAttempts = 50

    while (attempts < maxAttempts && existingNodes.any { candidate.intersects(getNodeBounds(it)) }) {
      when (direction) {
        BranchDirection.RIGHT, BranchDirection.LEFT -> {
          targetY += (CARD_HEIGHT + VERTICAL_GAP)
        }
        BranchDirection.DOWN, BranchDirection.UP -> {
          targetX += (CARD_WIDTH + HORIZONTAL_GAP)
        }
      }
      candidate = RectF(targetX, targetY, targetX + CARD_WIDTH, targetY + CARD_HEIGHT)
      attempts++
    }

    return Pair(targetX, targetY)
  }

  /**
   * Automatically re-arranges the entire tree hierarchically based on branch directions.
   */
  fun autoArrangeTree(treeDetails: TreeWithDetails): Map<String, Pair<Float, Float>> {
    val newPositions = mutableMapOf<String, Pair<Float, Float>>()
    val root = treeDetails.rootNode ?: return newPositions

    // Root is placed at origin (0, 0)
    newPositions[root.id] = Pair(0f, 0f)

    // Group outgoing connections by direction from root
    val rootConnections = treeDetails.outgoingConnections[root.id] ?: emptyList()
    val byDirection = rootConnections.groupBy { it.branchDir }

    // Position each branch group from root
    for ((direction, conns) in byDirection) {
      layoutBranch(
        parentPos = Pair(0f, 0f),
        direction = direction,
        connections = conns,
        treeDetails = treeDetails,
        outPositions = newPositions
      )
    }

    return newPositions
  }

  private fun layoutBranch(
    parentPos: Pair<Float, Float>,
    direction: BranchDirection,
    connections: List<ConnectionEntity>,
    treeDetails: TreeWithDetails,
    outPositions: MutableMap<String, Pair<Float, Float>>
  ) {
    if (connections.isEmpty()) return

    val totalCount = connections.size
    val stepSecondary = when (direction) {
      BranchDirection.RIGHT, BranchDirection.LEFT -> CARD_HEIGHT + VERTICAL_GAP
      BranchDirection.DOWN, BranchDirection.UP -> CARD_WIDTH + HORIZONTAL_GAP
    }

    val startOffset = -((totalCount - 1) * stepSecondary) / 2f

    connections.forEachIndexed { index, conn ->
      val childId = conn.toNodeId
      val currentOffset = startOffset + index * stepSecondary

      val childPos = when (direction) {
        BranchDirection.RIGHT -> Pair(
          parentPos.first + CARD_WIDTH + HORIZONTAL_GAP,
          parentPos.second + currentOffset
        )
        BranchDirection.LEFT -> Pair(
          parentPos.first - (CARD_WIDTH + HORIZONTAL_GAP),
          parentPos.second + currentOffset
        )
        BranchDirection.DOWN -> Pair(
          parentPos.first + currentOffset,
          parentPos.second + CARD_HEIGHT + VERTICAL_GAP
        )
        BranchDirection.UP -> Pair(
          parentPos.first + currentOffset,
          parentPos.second - (CARD_HEIGHT + VERTICAL_GAP)
        )
      }

      outPositions[childId] = childPos

      // Recurse to children of this child
      val subConns = treeDetails.outgoingConnections[childId] ?: emptyList()
      if (subConns.isNotEmpty()) {
        layoutBranch(
          parentPos = childPos,
          direction = direction, // maintain primary propagation direction
          connections = subConns,
          treeDetails = treeDetails,
          outPositions = outPositions
        )
      }
    }
  }

  /**
   * Computes the bounding box enclosing all visible nodes in the tree.
   */
  fun calculateBoundingBox(nodes: List<NodeEntity>): RectF? {
    if (nodes.isEmpty()) return null
    var minX = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE

    for (node in nodes) {
      minX = min(minX, node.posX)
      maxX = max(maxX, node.posX + CARD_WIDTH)
      minY = min(minY, node.posY)
      maxY = max(maxY, node.posY + CARD_HEIGHT)
    }

    return RectF(minX, minY, maxX, maxY)
  }

  /**
   * Finds all ancestors and descendants of a node for Focus Mode.
   */
  fun getLineageNodeIds(selectedNodeId: String, treeDetails: TreeWithDetails): Set<String> {
    val lineage = mutableSetOf(selectedNodeId)

    // Ancestors
    var curr: String? = selectedNodeId
    while (curr != null) {
      lineage.add(curr)
      curr = treeDetails.incomingConnections[curr]?.fromNodeId
    }

    // Descendants
    lineage.addAll(treeDetails.getAllDescendantIds(selectedNodeId))
    return lineage
  }
}
