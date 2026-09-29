package com.example.grocerylist.data.repository

import com.example.grocerylist.data.local.GroceryItem
import com.grocerylist.app.data.local.GroceryDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Interface defining domain operations for grocery items.
 */
interface GroceryRepository {

    /**
     * Observes all grocery items sorted with unbought first and newest first.
     */
    val allItems: Flow<List<GroceryItem>>

    /**
     * Observes the count of remaining unbought items.
     */
    val unboughtCount: Flow<Int>

    /**
     * Adds a new grocery item with sanitized name.
     *
     * @param name Name of the grocery item.
     * @return Row ID of the inserted item.
     */
    suspend fun addItem(name: String): Long

    /**
     * Restores a complete [GroceryItem] (used for Undo functionality and reactivation).
     *
     * @param item The grocery item to restore.
     * @return Row ID of the inserted item.
     */
    suspend fun restoreItem(item: GroceryItem): Long

    /**
     * Toggles the bought status of an item.
     *
     * @param item Item to toggle.
     */
    suspend fun toggleItemStatus(item: GroceryItem)


    /**
     * Deletes a grocery item.
     *
     * @param item Item to delete.
     */
    suspend fun deleteItem(item: GroceryItem)


    /**
     * Fetches an item by name (case-insensitive).
     *
     * @param name Name of the item.
     * @return The item if found, null otherwise.
     */
    suspend fun getItemByName(name: String): GroceryItem?

    /**
     * Deletes all grocery items from the list.
     *
     * @return Number of rows deleted.
     */
    suspend fun deleteAll(): Int
}

/**
 * Default implementation of [GroceryRepository] backed by Room [GroceryDao].
 */
class GroceryRepositoryImpl(
    private val groceryDao: GroceryDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GroceryRepository {

    override val allItems: Flow<List<GroceryItem>> = groceryDao.getAllItems()

    override val unboughtCount: Flow<Int> = groceryDao.getUnboughtCount()

    override suspend fun addItem(name: String): Long = withContext(ioDispatcher) {
        val item = GroceryItem(
            name = name.trim(),
            isBought = false,
            createdAt = System.currentTimeMillis()
        )
        groceryDao.insert(item)
    }

    override suspend fun restoreItem(item: GroceryItem): Long = withContext(ioDispatcher) {
        groceryDao.insert(item)
    }

    override suspend fun toggleItemStatus(item: GroceryItem): Unit = withContext(ioDispatcher) {
        val updated = item.copy(isBought = !item.isBought)
        groceryDao.update(updated)
    }

    override suspend fun getItemByName(name: String): GroceryItem? = withContext(ioDispatcher) {
        groceryDao.getItemByName(name.trim())
    }

    override suspend fun deleteItem(item: GroceryItem): Unit = withContext(ioDispatcher) {
        groceryDao.delete(item)
    }

    override suspend fun deleteAll(): Int = withContext(ioDispatcher) {
        groceryDao.deleteAll()
    }
}
