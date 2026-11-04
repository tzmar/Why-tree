package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TreeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TreeDao {
  @Query("SELECT * FROM trees WHERE isDeleted = 0 ORDER BY updatedAt DESC")
  fun getAllActiveTrees(): Flow<List<TreeEntity>>

  @Query("SELECT * FROM trees WHERE isDeleted = 1 ORDER BY deletedAt DESC")
  fun getTrashTrees(): Flow<List<TreeEntity>>

  @Query("SELECT * FROM trees WHERE id = :id LIMIT 1")
  fun getTreeByIdFlow(id: String): Flow<TreeEntity?>

  @Query("SELECT * FROM trees WHERE id = :id LIMIT 1")
  suspend fun getTreeById(id: String): TreeEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTree(tree: TreeEntity)

  @Update
  suspend fun updateTree(tree: TreeEntity)

  @Query("UPDATE trees SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
  suspend fun moveToTrash(id: String, deletedAt: Long = System.currentTimeMillis())

  @Query("UPDATE trees SET isDeleted = 0, deletedAt = NULL, updatedAt = :updatedAt WHERE id = :id")
  suspend fun restoreFromTrash(id: String, updatedAt: Long = System.currentTimeMillis())

  @Query("DELETE FROM trees WHERE id = :id")
  suspend fun deletePermanently(id: String)

  @Query("DELETE FROM trees WHERE isDeleted = 1 AND deletedAt < :cutoffTimestamp")
  suspend fun deleteExpiredTrash(cutoffTimestamp: Long)

  @Query("UPDATE trees SET zoom = :zoom, panX = :panX, panY = :panY, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateViewport(id: String, zoom: Float, panX: Float, panY: Float, updatedAt: Long = System.currentTimeMillis())
}
