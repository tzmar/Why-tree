package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TreeEntity
import com.example.ui.CurrentScreen
import com.example.ui.TreeViewModel
import com.example.ui.dialogs.ExportDialog
import com.example.ui.dialogs.ImportDialog
import com.example.ui.dialogs.NewTreeDialog
import com.example.ui.dialogs.TreeSettingsDialog
import com.example.ui.screens.CanvasScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.TreeBitmapExporter
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private val viewModel: TreeViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        WhyTreeApp(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun WhyTreeApp(viewModel: TreeViewModel) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
  val activeTrees by viewModel.activeTrees.collectAsStateWithLifecycle()
  val trashTrees by viewModel.trashTrees.collectAsStateWithLifecycle()
  val activeTreeDetails by viewModel.activeTreeDetails.collectAsStateWithLifecycle()
  val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
  val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

  var showNewTreeDialog by remember { mutableStateOf(false) }
  var showImportDialog by remember { mutableStateOf(false) }
  var treeForExportFromHome by remember { mutableStateOf<TreeEntity?>(null) }
  var treeForEditSettingsFromHome by remember { mutableStateOf<TreeEntity?>(null) }

  when (val screen = currentScreen) {
    is CurrentScreen.Home -> {
      HomeScreen(
        trees = activeTrees,
        trashCount = trashTrees.size,
        onOpenTree = { treeId -> viewModel.navigateToCanvas(treeId) },
        onCreateNewTree = { showNewTreeDialog = true },
        onOpenTrash = { viewModel.navigateToTrash() },
        onImportBackup = { showImportDialog = true },
        onDuplicateTree = { treeId -> viewModel.duplicateTree(treeId) },
        onEditTreeSettings = { tree -> treeForEditSettingsFromHome = tree },
        onMoveToTrash = { treeId -> viewModel.moveToTrash(treeId) },
        onExportTree = { tree -> treeForExportFromHome = tree }
      )
    }

    is CurrentScreen.Canvas -> {
      CanvasScreen(
        treeDetails = activeTreeDetails,
        canUndo = canUndo,
        canRedo = canRedo,
        onBack = { viewModel.navigateToHome() },
        onNodeMoved = { nodeId, x, y -> viewModel.moveNode(nodeId, x, y) },
        onAddChild = { parentId, rel, customLabel, text, notes, dir ->
          viewModel.addChildBranch(screen.treeId, parentId, rel, customLabel, text, notes, dir)
        },
        onEditNode = { nodeId, text, notes ->
          viewModel.updateNode(nodeId, text, notes)
        },
        onToggleCollapse = { nodeId, currentCollapsed ->
          viewModel.toggleNodeCollapse(nodeId, currentCollapsed)
        },
        onDuplicateNode = { nodeId ->
          viewModel.duplicateNode(screen.treeId, nodeId)
        },
        onDeleteNode = { nodeId, deleteSubtree ->
          viewModel.deleteNode(screen.treeId, nodeId, deleteSubtree)
        },
        onUpdateConnection = { connId, rel, label, dir ->
          viewModel.updateConnection(connId, rel, label, dir)
        },
        onAutoArrange = {
          viewModel.autoArrange(screen.treeId)
        },
        onUndo = { viewModel.undo(screen.treeId) },
        onRedo = { viewModel.redo(screen.treeId) },
        onViewportChanged = { zoom, panX, panY ->
          viewModel.updateViewport(screen.treeId, zoom, panX, panY)
        },
        onUpdateMetadata = { title, desc ->
          viewModel.updateTreeMetadata(screen.treeId, title, desc)
        },
        onMoveToTrash = {
          viewModel.moveToTrash(screen.treeId)
        },
        getJsonExport = {
          viewModel.repository.exportToJson(screen.treeId)
        },
        getMarkdownExport = {
          viewModel.repository.exportToMarkdown(screen.treeId)
        }
      )
    }

    is CurrentScreen.Trash -> {
      TrashScreen(
        trashTrees = trashTrees,
        onBack = { viewModel.navigateToHome() },
        onRestore = { treeId -> viewModel.restoreFromTrash(treeId) },
        onDeletePermanently = { treeId -> viewModel.deletePermanently(treeId) }
      )
    }
  }

  // App-level dialogs
  if (showNewTreeDialog) {
    NewTreeDialog(
      onDismiss = { showNewTreeDialog = false },
      onCreate = { title, rootQuestion, desc ->
        showNewTreeDialog = false
        viewModel.createNewTree(title, rootQuestion, desc)
      }
    )
  }

  if (showImportDialog) {
    ImportDialog(
      onDismiss = { showImportDialog = false },
      onImport = { json ->
        showImportDialog = false
        viewModel.importTreeFromJson(json) { result ->
          if (result.isSuccess) {
            Toast.makeText(context, "Tree imported successfully!", Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(context, "Import failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
          }
        }
      }
    )
  }

  if (treeForEditSettingsFromHome != null) {
    val tree = treeForEditSettingsFromHome!!
    TreeSettingsDialog(
      tree = tree,
      onDismiss = { treeForEditSettingsFromHome = null },
      onSave = { title, desc ->
        treeForEditSettingsFromHome = null
        viewModel.updateTreeMetadata(tree.id, title, desc)
      }
    )
  }

  if (treeForExportFromHome != null) {
    val tree = treeForExportFromHome!!
    ExportDialog(
      onDismiss = { treeForExportFromHome = null },
      onExportPng = {
        Toast.makeText(context, "Open tree in canvas to preview and share full PNG diagram", Toast.LENGTH_SHORT).show()
      },
      onExportJson = {
        coroutineScope.launch {
          val json = viewModel.repository.exportToJson(tree.id)
          TreeBitmapExporter.shareText(context, json, "${tree.title}.why")
        }
      },
      onExportMarkdown = {
        coroutineScope.launch {
          val md = viewModel.repository.exportToMarkdown(tree.id)
          TreeBitmapExporter.shareText(context, md, "${tree.title} - Outline.md")
        }
      }
    )
  }
}
