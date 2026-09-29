package com.grocerylist.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.grocerylist.data.local.GroceryItem
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for grocery items.
 * Provides SQLite operations backed by Kotlin Coroutines and Flows.
 */
@Dao
interface GroceryDao {

    /**
     * Observes all items in the grocery list.
     * Items are ordered with unbought items first (isBought = 0),
     * followed by bought items (isBought = 1).
     * Within each group, items are sorted by creation date descending (newest first).
     */
    @Query("SELECT * FROM grocery_items ORDER BY isBought ASC, createdAt DESC, id DESC")
    fun getAllItems(): Flow<List<GroceryItem>>

    /**
     * Inserts or replaces a grocery item in the database.
     *
     * @param item Item to insert.
     * @return The row ID of the inserted item.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: GroceryItem): Long

    /**
     * Inserts multiple grocery items in a single transaction.
     *
     * @param items Items to insert.
     * @return List of row IDs of the inserted items.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg items: GroceryItem): List<Long>

    /**
     * Updates an existing item (e.g. toggling isBought status or updating name).
     *
     * @param item Item with updated fields.
     */
    @Update
    suspend fun update(item: GroceryItem)

    /**
     * Deletes a specific grocery item.
     *
     * @param item Item to delete.
     */
    @Delete
    suspend fun delete(item: GroceryItem)



    /**
     * Fetches a single grocery item matching the specified name case-insensitively.
     *
     * @param name Name of the item to query.
     * @return The item if found, null otherwise.
     */
    @Query("SELECT * FROM grocery_items WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getItemByName(name: String): GroceryItem?

    /**
     * Observes the count of remaining unbought items.
     */
    @Query("SELECT COUNT(*) FROM grocery_items WHERE isBought = 0")
    fun getUnboughtCount(): Flow<Int>

    /**
     * Returns total count of all grocery items.
     */
    @Query("SELECT COUNT(*) FROM grocery_items")
    suspend fun getCount(): Int

    /**
     * Deletes all grocery items from the table.
     *
     * @return Number of rows deleted.
     */
    @Query("DELETE FROM grocery_items")
    suspend fun deleteAll(): Int
}
