package com.example.grocerylist.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.grocerylist.app.data.local.GroceryDao

/**
 * The Room Database for this app.
 * Provides the main access point to the underlying persisted SQLite connection.
 */
@Database(
    entities = [GroceryItem::class],
    version = 1,
    exportSchema = false
)
abstract class GroceryDatabase : RoomDatabase() {

    abstract fun groceryDao(): GroceryDao

    companion object {
        private const val DATABASE_NAME = "grocery_database"

        @Volatile
        private var INSTANCE: GroceryDatabase? = null

        /**
         * Gets the singleton instance of [GroceryDatabase].
         * Uses double-checked locking for thread-safe lazy initialization.
         */
        fun getDatabase(context: Context): GroceryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GroceryDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
