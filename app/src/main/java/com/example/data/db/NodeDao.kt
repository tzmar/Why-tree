package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.NodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NodeDao {
  @Query("SELECT * FROM nodes WHERE treeId = :treeId ORDER BY createdAt ASC")
  fun getNodesForTree(treeId: String): Flow<List<NodeEntity>>

  @Query("SELECT * FROM nodes WHERE treeId = :treeId ORDER BY createdAt ASC")
  suspend fun getNodesListForTree(treeId: String): List<NodeEntity>

  @Query("SELECT * FROM nodes WHERE id = :id LIMIT 1")
  suspend fun getNodeById(id: String): NodeEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNode(node: NodeEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNodes(nodes: List<NodeEntity>)

  @Update
  suspend fun updateNode(node: NodeEntity)

  @Query("UPDATE nodes SET posX = :posX, posY = :posY, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateNodePosition(id: String, posX: Float, posY: Float, updatedAt: Long = System.currentTimeMillis())

  @Query("UPDATE nodes SET isCollapsed = :collapsed, updatedAt = :updatedAt WHERE id = :id")
  suspend fun setNodeCollapsed(id: String, collapsed: Boolean, updatedAt: Long = System.currentTimeMillis())

  @Query("DELETE FROM nodes WHERE id = :id")
  suspend fun deleteNode(id: String)

  @Query("DELETE FROM nodes WHERE id IN (:ids)")
  suspend fun deleteNodes(ids: List<String>)

  @Query("DELETE FROM nodes WHERE treeId = :treeId")
  suspend fun deleteNodesForTree(treeId: String)
}
