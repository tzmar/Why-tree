package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "connections",
  indices = [
    Index(value = ["treeId"]),
    Index(value = ["fromNodeId"]),
    Index(value = ["toNodeId"])
  ]
)
data class ConnectionEntity(
  @PrimaryKey val id: String,
  val treeId: String,
  val fromNodeId: String,
  val toNodeId: String,
  val relationshipType: String,
  val customLabel: String = "",
  val direction: String = "RIGHT",
  val createdAt: Long = System.currentTimeMillis()
) {
  val relType: RelationshipType
    get() = RelationshipType.fromString(relationshipType)

  val branchDir: BranchDirection
    get() = BranchDirection.fromString(direction)

  val displayLabel: String
    get() = if (customLabel.isNotBlank()) customLabel else relType.label
}
