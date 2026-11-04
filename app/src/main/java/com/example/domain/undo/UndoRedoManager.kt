package com.example.domain.undo

import com.example.data.model.ConnectionEntity
import com.example.data.model.NodeEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface UndoAction {
  data class NodeAdded(
    val node: NodeEntity,
    val connection: ConnectionEntity?
  ) : UndoAction

  data class NodeMoved(
    val nodeId: String,
    val oldX: Float,
    val oldY: Float,
    val newX: Float,
    val newY: Float
  ) : UndoAction

  data class NodeEdited(
    val nodeId: String,
    val oldText: String,
    val oldNotes: String,
    val newText: String,
    val newNotes: String
  ) : UndoAction

  data class NodeDeleted(
    val deletedNodes: List<NodeEntity>,
    val deletedConnections: List<ConnectionEntity>
  ) : UndoAction

  data class RelationshipChanged(
    val connectionId: String,
    val oldRel: String,
    val oldCustom: String,
    val newRel: String,
    val newCustom: String
  ) : UndoAction

  data class TreeRearranged(
    val oldPositions: Map<String, Pair<Float, Float>>,
    val newPositions: Map<String, Pair<Float, Float>>
  ) : UndoAction
}

class UndoRedoManager {
  private val undoStack = mutableListOf<UndoAction>()
  private val redoStack = mutableListOf<UndoAction>()

  private val _canUndo = MutableStateFlow(false)
  val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

  private val _canRedo = MutableStateFlow(false)
  val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

  fun record(action: UndoAction) {
    undoStack.add(action)
    redoStack.clear()
    updateState()
  }

  fun popUndo(): UndoAction? {
    if (undoStack.isEmpty()) return null
    val action = undoStack.removeAt(undoStack.lastIndex)
    redoStack.add(action)
    updateState()
    return action
  }

  fun popRedo(): UndoAction? {
    if (redoStack.isEmpty()) return null
    val action = redoStack.removeAt(redoStack.lastIndex)
    undoStack.add(action)
    updateState()
    return action
  }

  fun clear() {
    undoStack.clear()
    redoStack.clear()
    updateState()
  }

  private fun updateState() {
    _canUndo.value = undoStack.isNotEmpty()
    _canRedo.value = redoStack.isNotEmpty()
  }
}
