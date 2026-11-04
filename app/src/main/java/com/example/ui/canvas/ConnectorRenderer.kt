package com.example.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BranchDirection
import com.example.data.model.ConnectionEntity
import com.example.data.model.NodeEntity
import com.example.domain.layout.TreeLayoutEngine
import com.example.ui.theme.MidnightNavy
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class ConnectorRenderData(
  val connection: ConnectionEntity,
  val start: Offset,
  val end: Offset,
  val control1: Offset,
  val control2: Offset,
  val midpoint: Offset,
  val color: Color,
  val isDimmed: Boolean
)

@Composable
fun ConnectorLayer(
  connections: List<ConnectionEntity>,
  nodeMap: Map<String, NodeEntity>,
  hiddenNodeIds: Set<String>,
  dimmedNodeIds: Set<String>,
  zoom: Float,
  panX: Float,
  panY: Float,
  onRelationshipClick: (ConnectionEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  val cardW = TreeLayoutEngine.CARD_WIDTH * zoom
  val cardH = TreeLayoutEngine.CARD_HEIGHT * zoom

  val renderItems = mutableListOf<ConnectorRenderData>()

  for (conn in connections) {
    if (hiddenNodeIds.contains(conn.fromNodeId) || hiddenNodeIds.contains(conn.toNodeId)) {
      continue
    }

    val parent = nodeMap[conn.fromNodeId] ?: continue
    val child = nodeMap[conn.toNodeId] ?: continue

    val pLeft = parent.posX * zoom + panX
    val pTop = parent.posY * zoom + panY
    val pRight = pLeft + cardW
    val pBottom = pTop + cardH
    val pCenterX = (pLeft + pRight) / 2f
    val pCenterY = (pTop + pBottom) / 2f

    val cLeft = child.posX * zoom + panX
    val cTop = child.posY * zoom + panY
    val cRight = cLeft + cardW
    val cBottom = cTop + cardH
    val cCenterX = (cLeft + cRight) / 2f
    val cCenterY = (cTop + cBottom) / 2f

    val (start, end, c1, c2) = when (conn.branchDir) {
      BranchDirection.RIGHT -> {
        val s = Offset(pRight, pCenterY)
        val e = Offset(cLeft, cCenterY)
        val midX = s.x + (e.x - s.x) * 0.5f
        Quad(s, e, Offset(midX, s.y), Offset(midX, e.y))
      }
      BranchDirection.LEFT -> {
        val s = Offset(pLeft, pCenterY)
        val e = Offset(cRight, cCenterY)
        val midX = s.x + (e.x - s.x) * 0.5f
        Quad(s, e, Offset(midX, s.y), Offset(midX, e.y))
      }
      BranchDirection.DOWN -> {
        val s = Offset(pCenterX, pBottom)
        val e = Offset(cCenterX, cTop)
        val midY = s.y + (e.y - s.y) * 0.5f
        Quad(s, e, Offset(s.x, midY), Offset(e.x, midY))
      }
      BranchDirection.UP -> {
        val s = Offset(pCenterX, pTop)
        val e = Offset(cCenterX, cBottom)
        val midY = s.y + (e.y - s.y) * 0.5f
        Quad(s, e, Offset(s.x, midY), Offset(e.x, midY))
      }
    }

    // Cubic bezier midpoint calculation: t = 0.5
    val mid = Offset(
      x = 0.125f * start.x + 0.375f * c1.x + 0.375f * c2.x + 0.125f * end.x,
      y = 0.125f * start.y + 0.375f * c1.y + 0.375f * c2.y + 0.125f * end.y
    )

    val isDimmed = dimmedNodeIds.isNotEmpty() &&
      (!dimmedNodeIds.contains(conn.fromNodeId) || !dimmedNodeIds.contains(conn.toNodeId))

    renderItems.add(
      ConnectorRenderData(
        connection = conn,
        start = start,
        end = end,
        control1 = c1,
        control2 = c2,
        midpoint = mid,
        color = conn.relType.defaultColor,
        isDimmed = isDimmed
      )
    )
  }

  // Draw lines layer
  Canvas(modifier = modifier.fillMaxSize()) {
    for (item in renderItems) {
      val lineColor = if (item.isDimmed) item.color.copy(alpha = 0.2f) else item.color.copy(alpha = 0.75f)
      val strokeW = (2.5f * zoom).coerceIn(1.5f, 4f)

      val path = Path().apply {
        moveTo(item.start.x, item.start.y)
        cubicTo(
          item.control1.x, item.control1.y,
          item.control2.x, item.control2.y,
          item.end.x, item.end.y
        )
      }

      drawPath(
        path = path,
        color = lineColor,
        style = Stroke(width = strokeW, cap = StrokeCap.Round)
      )

      // Arrow head at target
      val angle = atan2(item.end.y - item.control2.y, item.end.x - item.control2.x)
      val arrowSize = (10f * zoom).coerceIn(6f, 14f)
      val arrowAngle = Math.PI / 6.0 // 30 deg

      val p1 = Offset(
        (item.end.x - arrowSize * cos(angle - arrowAngle)).toFloat(),
        (item.end.y - arrowSize * sin(angle - arrowAngle)).toFloat()
      )
      val p2 = Offset(
        (item.end.x - arrowSize * cos(angle + arrowAngle)).toFloat(),
        (item.end.y - arrowSize * sin(angle + arrowAngle)).toFloat()
      )

      val arrowPath = Path().apply {
        moveTo(item.end.x, item.end.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
      }
      drawPath(arrowPath, color = lineColor)
    }
  }

  // Interactive Relationship Badges along curves
  for (item in renderItems) {
    if (zoom >= 0.5f) {
      val badgeColor = item.color
      val alpha = if (item.isDimmed) 0.3f else 1.0f

      Box(
        modifier = Modifier
          .offset {
            IntOffset(
              (item.midpoint.x - 30).roundToInt(),
              (item.midpoint.y - 12).roundToInt()
            )
          }
          .background(
            color = MidnightNavy.copy(alpha = 0.95f * alpha),
            shape = RoundedCornerShape(10.dp)
          )
          .border(
            width = 1.dp,
            color = badgeColor.copy(alpha = 0.7f * alpha),
            shape = RoundedCornerShape(10.dp)
          )
          .clickable { onRelationshipClick(item.connection) }
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Text(
          text = item.connection.displayLabel,
          color = badgeColor.copy(alpha = alpha),
          fontSize = (9 * zoom).coerceIn(8f, 11f).sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

private data class Quad(
  val start: Offset,
  val end: Offset,
  val c1: Offset,
  val c2: Offset
)
