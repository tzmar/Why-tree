package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.BranchDirection
import com.example.data.model.ConnectionEntity
import com.example.data.model.NodeEntity
import com.example.data.model.RelationshipType
import com.example.data.model.TreeEntity
import com.example.data.model.TreeWithDetails
import com.example.data.repository.TreeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CurrentScreen {
  object Home : CurrentScreen
  data class Canvas(val treeId: String) : CurrentScreen
  object Trash : CurrentScreen
}

class TreeViewModel(application: Application) : AndroidViewModel(application) {
  private val db = AppDatabase.getDatabase(application)
  val repository = TreeRepository(db)

  private val _currentScreen = MutableStateFlow<CurrentScreen>(CurrentScreen.Home)
  val currentScreen: StateFlow<CurrentScreen> = _currentScreen.asStateFlow()

  val activeTrees: StateFlow<List<TreeEntity>> = repository.activeTrees
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val trashTrees: StateFlow<List<TreeEntity>> = repository.trashTrees
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _activeTreeId = MutableStateFlow<String?>(null)
  val activeTreeId: StateFlow<String?> = _activeTreeId.asStateFlow()

  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  val activeTreeDetails: StateFlow<TreeWithDetails?> = _activeTreeId
    .flatMapLatest { id ->
      if (id == null) flowOf(null)
      else repository.getTreeDetailsFlow(id)
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val canUndo: StateFlow<Boolean> = repository.undoRedoManager.canUndo
  val canRedo: StateFlow<Boolean> = repository.undoRedoManager.canRedo

  init {
    viewModelScope.launch {
      repository.cleanExpiredTrash()
      repository.seedInitialDataIfEmpty()
    }
  }

  fun navigateToHome() {
    _currentScreen.value = CurrentScreen.Home
    _activeTreeId.value = null
  }

  fun navigateToCanvas(treeId: String) {
    _activeTreeId.value = treeId
    _currentScreen.value = CurrentScreen.Canvas(treeId)
  }

  fun navigateToTrash() {
    _currentScreen.value = CurrentScreen.Trash
  }

  fun createNewTree(
    title: String,
    rootQuestion: String,
    description: String = ""
  ) {
    viewModelScope.launch {
      val treeId = repository.createTree(title, rootQuestion, description)
      navigateToCanvas(treeId)
    }
  }

  fun createFiveWhysTemplate(problemStatement: String) {
    viewModelScope.launch {
      val treeId = repository.createTree("5 Whys: $problemStatement", problemStatement)
      val details = repository.getTreeDetailsFlow(treeId)
      // Once created, we navigate to it
      navigateToCanvas(treeId)
    }
  }

  fun addChildBranch(
    treeId: String,
    parentId: String,
    relationship: RelationshipType,
    customLabel: String,
    text: String,
    notes: String,
    direction: BranchDirection
  ) {
    viewModelScope.launch {
      repository.addChildBranch(
        treeId = treeId,
        parentId = parentId,
        relationshipType = relationship,
        customLabel = customLabel,
        text = text,
        notes = notes,
        direction = direction
      )
    }
  }

  fun updateNode(nodeId: String, text: String, notes: String) {
    viewModelScope.launch {
      repository.updateNode(nodeId, text, notes)
    }
  }

  fun moveNode(nodeId: String, newX: Float, newY: Float) {
    viewModelScope.launch {
      repository.moveNode(nodeId, newX, newY)
    }
  }

  fun toggleNodeCollapse(nodeId: String, currentCollapsed: Boolean) {
    viewModelScope.launch {
      repository.setNodeCollapsed(nodeId, !currentCollapsed)
    }
  }

  fun updateConnection(
    connectionId: String,
    relationship: RelationshipType,
    customLabel: String,
    direction: BranchDirection
  ) {
    viewModelScope.launch {
      repository.updateConnection(connectionId, relationship, customLabel, direction)
    }
  }

  fun deleteNode(treeId: String, nodeId: String, deleteSubtree: Boolean) {
    viewModelScope.launch {
      repository.deleteNode(treeId, nodeId, deleteSubtree)
    }
  }

  fun duplicateNode(treeId: String, nodeId: String) {
    viewModelScope.launch {
      repository.duplicateNode(treeId, nodeId)
    }
  }

  fun autoArrange(treeId: String) {
    viewModelScope.launch {
      repository.autoArrange(treeId)
    }
  }

  fun updateViewport(treeId: String, zoom: Float, panX: Float, panY: Float) {
    viewModelScope.launch {
      repository.updateViewport(treeId, zoom, panX, panY)
    }
  }

  fun undo(treeId: String) {
    viewModelScope.launch {
      repository.undo(treeId)
    }
  }

  fun redo(treeId: String) {
    viewModelScope.launch {
      repository.redo(treeId)
    }
  }

  fun updateTreeMetadata(treeId: String, title: String, description: String) {
    viewModelScope.launch {
      repository.updateTreeMetadata(treeId, title, description)
    }
  }

  fun duplicateTree(treeId: String) {
    viewModelScope.launch {
      val newId = repository.duplicateTree(treeId)
      if (newId.isNotBlank()) {
        navigateToCanvas(newId)
      }
    }
  }

  fun moveToTrash(treeId: String) {
    viewModelScope.launch {
      repository.moveToTrash(treeId)
      if (_activeTreeId.value == treeId) {
        navigateToHome()
      }
    }
  }

  fun restoreFromTrash(treeId: String) {
    viewModelScope.launch {
      repository.restoreFromTrash(treeId)
    }
  }

  fun deletePermanently(treeId: String) {
    viewModelScope.launch {
      repository.deletePermanently(treeId)
    }
  }

  fun importTreeFromJson(jsonStr: String, onComplete: (Result<String>) -> Unit) {
    viewModelScope.launch {
      val res = repository.importFromJson(jsonStr)
      if (res.isSuccess) {
        val treeId = res.getOrThrow()
        navigateToCanvas(treeId)
      }
      onComplete(res)
    }
  }
}
