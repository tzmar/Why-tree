package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BranchDirection
import com.example.data.model.ConnectionEntity
import com.example.data.model.NodeEntity
import com.example.data.model.RelationshipType
import com.example.data.model.TreeWithDetails
import com.example.ui.canvas.WhyCanvas
import com.example.ui.dialogs.AddChildDialog
import com.example.ui.dialogs.ChangeRelationshipDialog
import com.example.ui.dialogs.ConfirmDeleteDialog
import com.example.ui.dialogs.EditNodeDialog
import com.example.ui.dialogs.ExportDialog
import com.example.ui.dialogs.TreeSettingsDialog
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SurfaceElevatedNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.TreeBitmapExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
  treeDetails: TreeWithDetails?,
  canUndo: Boolean,
  canRedo: Boolean,
  onBack: () -> Unit,
  onNodeMoved: (nodeId: String, x: Float, y: Float) -> Unit,
  onAddChild: (
    parentId: String,
    rel: RelationshipType,
    customLabel: String,
    text: String,
    notes: String,
    direction: BranchDirection
  ) -> Unit,
  onEditNode: (nodeId: String, text: String, notes: String) -> Unit,
  onToggleCollapse: (nodeId: String, currentCollapsed: Boolean) -> Unit,
  onDuplicateNode: (nodeId: String) -> Unit,
  onDeleteNode: (nodeId: String, deleteSubtree: Boolean) -> Unit,
  onUpdateConnection: (connId: String, rel: RelationshipType, customLabel: String, dir: BranchDirection) -> Unit,
  onAutoArrange: () -> Unit,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onViewportChanged: (zoom: Float, panX: Float, panY: Float) -> Unit,
  onUpdateMetadata: (title: String, desc: String) -> Unit,
  onMoveToTrash: () -> Unit,
  getJsonExport: suspend () -> String,
  getMarkdownExport: suspend () -> String,
  modifier: Modifier = Modifier
) {
  BackHandler { onBack() }

  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val coroutineScope = rememberCoroutineScope()

  var nodeForAddChild by remember { mutableStateOf<NodeEntity?>(null) }
  var nodeForEdit by remember { mutableStateOf<NodeEntity?>(null) }
  var connectionForEdit by remember { mutableStateOf<ConnectionEntity?>(null) }
  var nodeForDelete by remember { mutableStateOf<NodeEntity?>(null) }
  var showExportDialog by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showTopMenu by remember { mutableStateOf(false) }

  if (treeDetails == null) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MidnightNavy),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator(color = SoftGold)
    }
    return
  }

  Scaffold(
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = DeepNavy,
          titleContentColor = TextPrimary
        ),
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("canvas_back_btn")) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Trees",
              tint = TextPrimary
            )
          }
        },
        title = {
          Column(
            modifier = Modifier.clickable { showSettingsDialog = true }
          ) {
            Text(
              text = treeDetails.tree.title,
              color = TextPrimary,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${treeDetails.nodes.size} nodes · ${treeDetails.connections.size} connections",
              color = TextSecondary,
              fontSize = 11.sp
            )
          }
        },
        actions = {
          IconButton(
            onClick = { showExportDialog = true },
            modifier = Modifier.testTag("canvas_export_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = "Export Tree",
              tint = SoftGold
            )
          }

          Box {
            IconButton(
              onClick = { showTopMenu = true },
              modifier = Modifier.testTag("canvas_more_btn")
            ) {
              Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More Options",
                tint = TextSecondary
              )
            }

            DropdownMenu(
              expanded = showTopMenu,
              onDismissRequest = { showTopMenu = false },
              modifier = Modifier.background(SurfaceElevatedNavy)
            ) {
              DropdownMenuItem(
                text = { Text("Tree Settings / Rename", color = TextPrimary) },
                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = TextSecondary) },
                onClick = {
                  showTopMenu = false
                  showSettingsDialog = true
                }
              )
              DropdownMenuItem(
                text = { Text("Move to Trash", color = Color(0xFFEF4444)) },
                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444)) },
                onClick = {
                  showTopMenu = false
                  onMoveToTrash()
                }
              )
            }
          }
        }
      )
    },
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      WhyCanvas(
        treeDetails = treeDetails,
        canUndo = canUndo,
        canRedo = canRedo,
        onNodeMoved = onNodeMoved,
        onNodeAddChild = { nodeForAddChild = it },
        onNodeEdit = { nodeForEdit = it },
        onNodeToggleCollapse = { onToggleCollapse(it.id, it.isCollapsed) },
        onNodeDuplicate = { onDuplicateNode(it.id) },
        onNodeDelete = { nodeForDelete = it },
        onRelationshipClick = { connectionForEdit = it },
        onAutoArrange = onAutoArrange,
        onUndo = onUndo,
        onRedo = onRedo,
        onViewportChanged = onViewportChanged
      )
    }
  }

  // Dialogs
  if (nodeForAddChild != null) {
    AddChildDialog(
      parentNode = nodeForAddChild!!,
      onDismiss = { nodeForAddChild = null },
      onConfirm = { rel, customLabel, text, notes, dir ->
        val parent = nodeForAddChild!!
        nodeForAddChild = null
        onAddChild(parent.id, rel, customLabel, text, notes, dir)
      }
    )
  }

  if (nodeForEdit != null) {
    EditNodeDialog(
      node = nodeForEdit!!,
      onDismiss = { nodeForEdit = null },
      onSave = { text, notes ->
        val node = nodeForEdit!!
        nodeForEdit = null
        onEditNode(node.id, text, notes)
      }
    )
  }

  if (connectionForEdit != null) {
    ChangeRelationshipDialog(
      connection = connectionForEdit!!,
      onDismiss = { connectionForEdit = null },
      onSave = { rel, label, dir ->
        val conn = connectionForEdit!!
        connectionForEdit = null
        onUpdateConnection(conn.id, rel, label, dir)
      }
    )
  }

  if (nodeForDelete != null) {
    val node = nodeForDelete!!
    val descendantCount = remember(node.id, treeDetails) {
      treeDetails.getAllDescendantIds(node.id).size
    }
    ConfirmDeleteDialog(
      node = node,
      descendantCount = descendantCount,
      onDismiss = { nodeForDelete = null },
      onDeleteSubtree = {
        nodeForDelete = null
        onDeleteNode(node.id, true)
      },
      onDeleteNodeOnly = {
        nodeForDelete = null
        onDeleteNode(node.id, false)
      }
    )
  }

  if (showExportDialog) {
    ExportDialog(
      onDismiss = { showExportDialog = false },
      onExportPng = {
        val bitmap = TreeBitmapExporter.generateTreeBitmap(treeDetails)
        TreeBitmapExporter.shareBitmap(context, bitmap, treeDetails.tree.title)
      },
      onExportJson = {
        coroutineScope.launch {
          val json = getJsonExport()
          clipboardManager.setText(AnnotatedString(json))
          Toast.makeText(context, ".why JSON copied to clipboard! Sharing file...", Toast.LENGTH_SHORT).show()
          TreeBitmapExporter.shareText(context, json, "${treeDetails.tree.title}.why")
        }
      },
      onExportMarkdown = {
        coroutineScope.launch {
          val md = getMarkdownExport()
          clipboardManager.setText(AnnotatedString(md))
          Toast.makeText(context, "Markdown outline copied to clipboard!", Toast.LENGTH_SHORT).show()
          TreeBitmapExporter.shareText(context, md, "${treeDetails.tree.title} - Outline.md")
        }
      }
    )
  }

  if (showSettingsDialog) {
    TreeSettingsDialog(
      tree = treeDetails.tree,
      onDismiss = { showSettingsDialog = false },
      onSave = { title, desc ->
        showSettingsDialog = false
        onUpdateMetadata(title, desc)
      }
    )
  }
}
