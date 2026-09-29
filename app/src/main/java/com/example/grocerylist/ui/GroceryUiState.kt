package com.example.grocerylist.ui

import com.example.grocerylist.data.local.GroceryItem

/**
 * Immutable state representation for the grocery list screen.
 *
 * @property items Current list of grocery items.
 * @property unboughtCount Number of items left to buy.
 * @property isLoading Whether initial state loading is in progress.
 * @property userMessage Transient message for user feedback (errors, notifications).
 * @property lastDeletedItem Cached copy of the last deleted item to support Undo.
 */
data class GroceryUiState(
    val items: List<GroceryItem> = emptyList(),
    val unboughtCount: Int = 0,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val lastDeletedItem: GroceryItem? = null
)
