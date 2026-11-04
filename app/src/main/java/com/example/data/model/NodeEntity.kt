package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "nodes",
  indices = [Index(value = ["treeId"])]
)
data class NodeEntity(
  @PrimaryKey val id: String,
  val treeId: String,
  val text: String,
  val notes: String = "",
  val posX: Float = 0f,
  val posY: Float = 0f,
  val isRoot: Boolean = false,
  val isCollapsed: Boolean = false,
  val colorTag: String = "",
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)
