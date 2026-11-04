package com.example.ui.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionEntity
import com.example.data.model.NodeEntity
import com.example.domain.layout.TreeLayoutEngine
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardBorderSelected
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.RelWhy
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevatedNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun NodeCard(
  node: NodeEntity,
  incomingConnection: ConnectionEntity?,
  childCount: Int,
  isSelected: Boolean,
  isDimmed: Boolean,
  isFocused: Boolean,
  zoom: Float,
  panX: Float,
  panY: Float,
  onSelect: () -> Unit,
  onDragDelta: (dx: Float, dy: Float) -> Unit,
  onDragEnd: () -> Unit,
  onAddChild: () -> Unit,
  onEdit: () -> Unit,
  onToggleCollapse: () -> Unit,
  onDuplicate: () -> Unit,
  onDelete: () -> Unit,
  onToggleFocus: () -> Unit,
  modifier: Modifier = Modifier
) {
  // Screen coordinate calculation
  val screenX = (node.posX * zoom + panX)
  val screenY = (node.posY * zoom + panY)

  val cardWidthDp = (TreeLayoutEngine.CARD_WIDTH * zoom).dp
  val cardMinHeightDp = (TreeLayoutEngine.CARD_HEIGHT * zoom).dp

  val borderColor = when {
    isSelected -> CardBorderSelected
    node.isRoot -> SoftGold.copy(alpha = 0.8f)
    else -> CardBorder
  }

  val borderWidth = if (isSelected || node.isRoot) 2.dp else 1.dp
  val cardAlpha = if (isDimmed) 0.3f else 1.0f

  Box(
    modifier = modifier
      .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
      .width(cardWidthDp)
      .pointerInput(node.id) {
        detectDragGestures(
          onDragStart = { onSelect() },
          onDragEnd = { onDragEnd() },
          onDrag = { change, dragAmount ->
            change.consume()
            // Convert screen drag delta to world delta
            onDragDelta(dragAmount.x / zoom, dragAmount.y / zoom)
          }
        )
      }
      .testTag("node_card_${node.id}")
  ) {
    Column {
      // Floating toolbar above card when selected
      AnimatedVisibility(
        visible = isSelected && !isDimmed,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = SurfaceElevatedNavy,
          shadowElevation = 8.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
          modifier = Modifier
            .padding(bottom = 6.dp)
            .align(Alignment.CenterHorizontally)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
          ) {
            IconButton(
              onClick = onAddChild,
              modifier = Modifier
                .size(36.dp)
                .testTag("card_action_add_child")
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Child",
                tint = SoftGold,
                modifier = Modifier.size(20.dp)
              )
            }
            IconButton(
              onClick = onEdit,
              modifier = Modifier
                .size(36.dp)
                .testTag("card_action_edit")
            ) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Card",
                tint = TextPrimary,
                modifier = Modifier.size(18.dp)
              )
            }
            IconButton(
              onClick = onToggleFocus,
              modifier = Modifier
                .size(36.dp)
                .testTag("card_action_focus")
            ) {
              Icon(
                imageVector = if (isFocused) Icons.Default.CenterFocusWeak else Icons.Default.CenterFocusStrong,
                contentDescription = "Focus Branch",
                tint = if (isFocused) SoftGold else TextSecondary,
                modifier = Modifier.size(18.dp)
              )
            }
            IconButton(
              onClick = onDuplicate,
              modifier = Modifier
                .size(36.dp)
                .testTag("card_action_duplicate")
            ) {
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Duplicate Card",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
              )
            }
            if (!node.isRoot) {
              IconButton(
                onClick = onDelete,
                modifier = Modifier
                  .size(36.dp)
                  .testTag("card_action_delete")
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = "Delete Card",
                  tint = Color(0xFFEF4444),
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }

      // Main Card Surface
      Surface(
        shape = RoundedCornerShape((12 * zoom).coerceIn(8f, 16f).dp),
        color = SurfaceCard.copy(alpha = cardAlpha),
        shadowElevation = if (isSelected) 10.dp else 4.dp,
        modifier = Modifier
          .fillMaxWidth()
          .border(
            width = borderWidth,
            color = borderColor.copy(alpha = cardAlpha),
            shape = RoundedCornerShape((12 * zoom).coerceIn(8f, 16f).dp)
          )
          .clip(RoundedCornerShape((12 * zoom).coerceIn(8f, 16f).dp))
          .clickable { onSelect() }
      ) {
        Column(
          modifier = Modifier.padding(
            horizontal = (14 * zoom).coerceIn(8f, 18f).dp,
            vertical = (12 * zoom).coerceIn(8f, 16f).dp
          )
        ) {
          // Top Badges Row
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            if (node.isRoot) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                  .background(SoftGold.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Star,
                  contentDescription = "Root Problem",
                  tint = SoftGold,
                  modifier = Modifier.size((12 * zoom).coerceIn(10f, 16f).dp)
                )
                Text(
                  text = "ROOT PROBLEM",
                  color = SoftGold,
                  fontSize = (10 * zoom).coerceIn(8f, 12f).sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                )
              }
            } else if (incomingConnection != null) {
              val relColor = incomingConnection.relType.defaultColor
              Box(
                modifier = Modifier
                  .background(relColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                  .border(1.dp, relColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = incomingConnection.displayLabel,
                  color = relColor,
                  fontSize = (10 * zoom).coerceIn(8f, 12f).sp,
                  fontWeight = FontWeight.Bold
                )
              }
            } else {
              Spacer(modifier = Modifier.width(1.dp))
            }

            // Child Count / Collapse Toggle
            if (childCount > 0) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (node.isCollapsed) SoftGold.copy(alpha = 0.2f) else DeepNavy,
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (node.isCollapsed) SoftGold else CardBorder
                ),
                modifier = Modifier.clickable { onToggleCollapse() }
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(2.dp),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = if (node.isCollapsed) "+$childCount hidden" else "$childCount",
                    color = if (node.isCollapsed) SoftGold else TextSecondary,
                    fontSize = (10 * zoom).coerceIn(8f, 11f).sp,
                    fontWeight = FontWeight.SemiBold
                  )
                  Icon(
                    imageVector = if (node.isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = if (node.isCollapsed) "Expand branches" else "Collapse branches",
                    tint = if (node.isCollapsed) SoftGold else TextSecondary,
                    modifier = Modifier.size((12 * zoom).coerceIn(10f, 14f).dp)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height((8 * zoom).coerceIn(4f, 10f).dp))

          // Text / Title
          Text(
            text = node.text,
            color = TextPrimary.copy(alpha = cardAlpha),
            fontSize = if (node.isRoot) (15 * zoom).coerceIn(12f, 18f).sp else (13 * zoom).coerceIn(10f, 15f).sp,
            fontWeight = if (node.isRoot) FontWeight.Bold else FontWeight.Medium,
            lineHeight = (18 * zoom).coerceIn(14f, 22f).sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
          )

          // Notes preview if present
          if (node.notes.isNotBlank() && zoom >= 0.7f) {
            Spacer(modifier = Modifier.height((4 * zoom).coerceIn(2f, 6f).dp))
            Text(
              text = node.notes,
              color = TextMuted.copy(alpha = cardAlpha),
              fontSize = (11 * zoom).coerceIn(8f, 12f).sp,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
  }
}
