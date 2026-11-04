package com.example.data.model

enum class BranchDirection(
  val label: String,
  val deltaX: Float,
  val deltaY: Float
) {
  RIGHT("Right", 1f, 0f),
  DOWN("Down", 0f, 1f),
  LEFT("Left", -1f, 0f),
  UP("Up", 0f, -1f);

  companion object {
    fun fromString(value: String): BranchDirection {
      return entries.find { it.name.equals(value, ignoreCase = true) } ?: RIGHT
    }
  }
}
