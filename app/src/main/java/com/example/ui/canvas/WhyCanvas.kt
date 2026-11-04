package com.example.ui.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionEntity
import com.example.data.model.NodeEntity
import com.example.data.model.TreeWithDetails
import com.example.domain.layout.TreeLayoutEngine
import com.example.ui.theme.CardBorder
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.RelWhy
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevatedNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun WhyCanvas(
  treeDetails: TreeWithDetails,
  canUndo: Boolean,
  canRedo: Boolean,
  onNodeMoved: (nodeId: String, newX: Float, newY: Float) -> Unit,
  onNodeAddChild: (node: NodeEntity) -> Unit,
  onNodeEdit: (node: NodeEntity) -> Unit,
  onNodeToggleCollapse: (node: NodeEntity) -> Unit,
  onNodeDuplicate: (node: NodeEntity) -> Unit,
  onNodeDelete: (node: NodeEntity) -> Unit,
  onRelationshipClick: (ConnectionEntity) -> Unit,
  onAutoArrange: () -> Unit,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onViewportChanged: (zoom: Float, panX: Float, panY: Float) -> Unit,
  modifier: Modifier = Modifier
) {
  val coroutineScope = rememberCoroutineScope()

  var zoom by remember { mutableFloatStateOf(treeDetails.tree.zoom.coerceIn(0.25f, 2.5f)) }
  var panX by remember { mutableFloatStateOf(treeDetails.tree.panX) }
  var panY by remember { mutableFloatStateOf(treeDetails.tree.panY) }

  var selectedNodeId by remember { mutableStateOf<String?>(null) }
  var focusedNodeId by remember { mutableStateOf<String?>(null) }
  var isSearchOpen by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var showMinimap by remember { mutableStateOf(true) }

  // Dragging cache for instant 60fps card movement
  val localNodePositions = remember { mutableStateMapOf<String, Pair<Float, Float>>() }

  // Sync positions when tree details update from DB
  LaunchedEffect(treeDetails.nodes) {
    for (node in treeDetails.nodes) {
      localNodePositions[node.id] = Pair(node.posX, node.posY)
    }
  }

  val hiddenNodeIds = remember(treeDetails.nodes, treeDetails.connections) {
    treeDetails.getHiddenNodeIds()
  }

  val dimmedNodeIds = remember(focusedNodeId, treeDetails) {
    if (focusedNodeId == null) emptySet()
    else {
      val lineage = TreeLayoutEngine.getLineageNodeIds(focusedNodeId!!, treeDetails)
      treeDetails.nodes.map { it.id }.filterNot { lineage.contains(it) }.toSet()
    }
  }

  // Search matches
  val searchMatches = remember(searchQuery, treeDetails.nodes) {
    if (searchQuery.isBlank()) emptyList()
    else treeDetails.nodes.filter { it.text.contains(searchQuery, ignoreCase = true) }
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(MidnightNavy)
      .pointerInput(Unit) {
        detectTransformGestures { _, pan, gestureZoom, _ ->
          selectedNodeId = null // Tap outside deselects
          zoom = (zoom * gestureZoom).coerceIn(0.25f, 2.5f)
          panX += pan.x
          panY += pan.y
          onViewportChanged(zoom, panX, panY)
        }
      }
      .testTag("why_canvas")
  ) {
    val canvasWidth = constraints.maxWidth.toFloat()
    val canvasHeight = constraints.maxHeight.toFloat()

    // 1. Grid Background (spatial dots)
    Canvas(modifier = Modifier.fillMaxSize()) {
      val gridSize = (40f * zoom).coerceIn(20f, 80f)
      val dotRadius = (1.5f * zoom).coerceIn(1f, 2.5f)
      val gridColor = Color(0xFF16233B).copy(alpha = 0.5f)

      val startX = (panX % gridSize)
      val startY = (panY % gridSize)

      var x = startX
      while (x < size.width) {
        var y = startY
        while (y < size.height) {
          drawCircle(color = gridColor, radius = dotRadius, center = Offset(x, y))
          y += gridSize
        }
        x += gridSize
      }
    }

    // Nodes with local animated / dragged positions
    val renderedNodes = treeDetails.nodes.map { origNode ->
      val localPos = localNodePositions[origNode.id]
      if (localPos != null) {
        origNode.copy(posX = localPos.first, posY = localPos.second)
      } else {
        origNode
      }
    }
    val renderedNodeMap = renderedNodes.associateBy { it.id }

    // 2. Curved Connections Layer
    ConnectorLayer(
      connections = treeDetails.connections,
      nodeMap = renderedNodeMap,
      hiddenNodeIds = hiddenNodeIds,
      dimmedNodeIds = dimmedNodeIds,
      zoom = zoom,
      panX = panX,
      panY = panY,
      onRelationshipClick = onRelationshipClick
    )

    // 3. Node Cards Layer
    for (node in renderedNodes) {
      if (hiddenNodeIds.contains(node.id)) continue

      val childNodes = treeDetails.getChildren(node.id)
      val incoming = treeDetails.getParentConnection(node.id)
      val isSelected = selectedNodeId == node.id
      val isDimmed = dimmedNodeIds.contains(node.id)
      val isFocused = focusedNodeId == node.id

      NodeCard(
        node = node,
        incomingConnection = incoming,
        childCount = childNodes.size,
        isSelected = isSelected,
        isDimmed = isDimmed,
        isFocused = isFocused,
        zoom = zoom,
        panX = panX,
        panY = panY,
        onSelect = { selectedNodeId = node.id },
        onDragDelta = { dx, dy ->
          val current = localNodePositions[node.id] ?: Pair(node.posX, node.posY)
          val newPos = Pair(current.first + dx, current.second + dy)
          localNodePositions[node.id] = newPos
        },
        onDragEnd = {
          val finalPos = localNodePositions[node.id] ?: Pair(node.posX, node.posY)
          onNodeMoved(node.id, finalPos.first, finalPos.second)
        },
        onAddChild = { onNodeAddChild(node) },
        onEdit = { onNodeEdit(node) },
        onToggleCollapse = { onNodeToggleCollapse(node) },
        onDuplicate = { onNodeDuplicate(node) },
        onDelete = { onNodeDelete(node) },
        onToggleFocus = {
          focusedNodeId = if (focusedNodeId == node.id) null else node.id
        }
      )
    }

    // 4. Focus Mode Banner (if active)
    AnimatedVisibility(
      visible = focusedNodeId != null,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 16.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceElevatedNavy,
        border = androidx.compose.foundation.BorderStroke(1.dp, SoftGold),
        shadowElevation = 8.dp
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.CenterFocusStrong,
            contentDescription = "Focus Active",
            tint = SoftGold,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Focus Mode Active",
            color = SoftGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
            onClick = { focusedNodeId = null },
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Exit Focus",
              tint = TextSecondary,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }

    // 5. In-Canvas Search Bar
    AnimatedVisibility(
      visible = isSearchOpen,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = if (focusedNodeId != null) 60.dp else 16.dp)
        .fillMaxWidth(0.9f)
    ) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceElevatedNavy,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        shadowElevation = 12.dp
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search",
              tint = SoftGold,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("Search nodes in tree...", color = TextMuted) },
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("canvas_search_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
              )
            )
            IconButton(onClick = { isSearchOpen = false; searchQuery = "" }) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Search",
                tint = TextSecondary
              )
            }
          }

          if (searchMatches.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Matches (${searchMatches.size}): tap to jump",
              color = SoftGold,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              searchMatches.take(4).forEach { matchedNode ->
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = SurfaceCard,
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      // Pan and center camera on this node
                      selectedNodeId = matchedNode.id
                      panX = canvasWidth / 2f - (matchedNode.posX + TreeLayoutEngine.CARD_WIDTH / 2f) * zoom
                      panY = canvasHeight / 2f - (matchedNode.posY + TreeLayoutEngine.CARD_HEIGHT / 2f) * zoom
                      onViewportChanged(zoom, panX, panY)
                    }
                ) {
                  Text(
                    text = matchedNode.text,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                  )
                }
              }
            }
          }
        }
      }
    }

    // 6. Floating Canvas HUD (Bottom Left & Right)
    // Left: Undo / Redo / Auto-arrange / Search
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = SurfaceElevatedNavy.copy(alpha = 0.95f),
      border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
      shadowElevation = 8.dp,
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(start = 16.dp, bottom = 24.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(4.dp)
      ) {
        IconButton(
          onClick = onUndo,
          enabled = canUndo,
          modifier = Modifier.testTag("hud_undo")
        ) {
          Icon(
            imageVector = Icons.Default.Undo,
            contentDescription = "Undo",
            tint = if (canUndo) TextPrimary else TextMuted
          )
        }
        IconButton(
          onClick = onRedo,
          enabled = canRedo,
          modifier = Modifier.testTag("hud_redo")
        ) {
          Icon(
            imageVector = Icons.Default.Redo,
            contentDescription = "Redo",
            tint = if (canRedo) TextPrimary else TextMuted
          )
        }
        Box(
          modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(CardBorder)
        )
        IconButton(
          onClick = onAutoArrange,
          modifier = Modifier.testTag("hud_auto_arrange")
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "Auto Arrange Tree",
            tint = SoftGold
          )
        }
        IconButton(
          onClick = { isSearchOpen = !isSearchOpen },
          modifier = Modifier.testTag("hud_search_toggle")
        ) {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search in tree",
            tint = if (isSearchOpen) SoftGold else TextSecondary
          )
        }
      }
    }

    // Right: Zoom Controls & Fit to Tree
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = SurfaceElevatedNavy.copy(alpha = 0.95f),
      border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
      shadowElevation = 8.dp,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 16.dp, bottom = 24.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(4.dp)
      ) {
        IconButton(
          onClick = {
            zoom = (zoom * 1.25f).coerceAtMost(2.5f)
            onViewportChanged(zoom, panX, panY)
          },
          modifier = Modifier.testTag("hud_zoom_in")
        ) {
          Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", tint = TextPrimary)
        }

        Box(
          modifier = Modifier
            .clickable {
              zoom = 1.0f
              onViewportChanged(zoom, panX, panY)
            }
            .padding(horizontal = 4.dp)
        ) {
          Text(
            text = "${(zoom * 100).roundToInt()}%",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
        }

        IconButton(
          onClick = {
            zoom = (zoom / 1.25f).coerceAtLeast(0.25f)
            onViewportChanged(zoom, panX, panY)
          },
          modifier = Modifier.testTag("hud_zoom_out")
        ) {
          Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", tint = TextPrimary)
        }

        Box(
          modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(CardBorder)
        )

        // Fit To Tree button
        IconButton(
          onClick = {
            val visibleNodes = renderedNodes.filterNot { hiddenNodeIds.contains(it.id) }
            val bounds = TreeLayoutEngine.calculateBoundingBox(visibleNodes)
            if (bounds != null) {
              val pad = 120f
              val treeW = bounds.width + pad * 2
              val treeH = bounds.height + pad * 2

              val zoomX = canvasWidth / treeW
              val zoomY = canvasHeight / treeH
              val fitZoom = min(zoomX, zoomY).coerceIn(0.35f, 1.2f)

              val centerX = bounds.centerX
              val centerY = bounds.centerY

              zoom = fitZoom
              panX = canvasWidth / 2f - centerX * fitZoom
              panY = canvasHeight / 2f - centerY * fitZoom
              onViewportChanged(zoom, panX, panY)
            }
          },
          modifier = Modifier.testTag("hud_fit_to_tree")
        ) {
          Icon(
            imageVector = Icons.Default.CropFree,
            contentDescription = "Fit to Tree",
            tint = SoftGold
          )
        }
      }
    }
  }
}
