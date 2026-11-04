package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ConnectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionDao {
  @Query("SELECT * FROM connections WHERE treeId = :treeId ORDER BY createdAt ASC")
  fun getConnectionsForTree(treeId: String): Flow<List<ConnectionEntity>>

  @Query("SELECT * FROM connections WHERE treeId = :treeId ORDER BY createdAt ASC")
  suspend fun getConnectionsListForTree(treeId: String): List<ConnectionEntity>

  @Query("SELECT * FROM connections WHERE id = :id LIMIT 1")
  suspend fun getConnectionById(id: String): ConnectionEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertConnection(connection: ConnectionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertConnections(connections: List<ConnectionEntity>)

  @Update
  suspend fun updateConnection(connection: ConnectionEntity)

  @Query("UPDATE connections SET relationshipType = :type, customLabel = :label WHERE id = :id")
  suspend fun updateRelationship(id: String, type: String, label: String)

  @Query("DELETE FROM connections WHERE id = :id")
  suspend fun deleteConnection(id: String)

  @Query("DELETE FROM connections WHERE fromNodeId = :nodeId OR toNodeId = :nodeId")
  suspend fun deleteConnectionsForNode(nodeId: String)

  @Query("DELETE FROM connections WHERE fromNodeId IN (:nodeIds) OR toNodeId IN (:nodeIds)")
  suspend fun deleteConnectionsForNodes(nodeIds: List<String>)

  @Query("DELETE FROM connections WHERE treeId = :treeId")
  suspend fun deleteConnectionsForTree(treeId: String)
}
