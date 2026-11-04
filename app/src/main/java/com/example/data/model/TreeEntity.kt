package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trees")
data class TreeEntity(
  @PrimaryKey val id: String,
  val title: String,
  val description: String = "",
  val rootNodeId: String,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val isDeleted: Boolean = false,
  val deletedAt: Long? = null,
  val zoom: Float = 1.0f,
  val panX: Float = 0f,
  val panY: Float = 0f
) {
  fun daysUntilPermanentDeletion(now: Long = System.currentTimeMillis()): Int {
    if (deletedAt == null) return 30
    val elapsedMillis = now - deletedAt
    val remainingDays = 30 - (elapsedMillis / (1000 * 60 * 60 * 24)).toInt()
    return remainingDays.coerceAtLeast(0)
  }
}
