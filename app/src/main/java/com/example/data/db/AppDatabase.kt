package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ConnectionEntity
import com.example.data.model.NodeEntity
import com.example.data.model.TreeEntity

@Database(
  entities = [TreeEntity::class, NodeEntity::class, ConnectionEntity::class],
  version = 1,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun treeDao(): TreeDao
  abstract fun nodeDao(): NodeDao
  abstract fun connectionDao(): ConnectionDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "whytree_database.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
