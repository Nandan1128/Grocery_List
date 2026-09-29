package com.example.grocerylist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database entity representing an item in the grocery list.
 *
 * @property id Unique auto-generated identifier.
 * @property name Cleaned display name of the grocery item.
 * @property isBought Whether the item has been ticked off / purchased.
 * @property createdAt Epoch timestamp in milliseconds when the item was added.
 */
@Entity(tableName = "grocery_items")
data class GroceryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isBought: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
