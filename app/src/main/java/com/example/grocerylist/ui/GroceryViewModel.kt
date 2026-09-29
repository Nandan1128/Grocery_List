package com.example.grocerylist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.grocerylist.data.local.GroceryItem
import com.example.grocerylist.data.repository.GroceryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing the business logic, input validation, and UI state for the grocery list.
 * Exposes a unidirectional [uiState] stream observed by the UI layer.
 */
class GroceryViewModel(
    private val repository: GroceryRepository,
    started: SharingStarted = SharingStarted.WhileSubscribed(5000),
) : ViewModel() {

    companion object {
        const val MAX_ITEM_NAME_LENGTH = 200
    }

    private val _userMessage = MutableStateFlow<String?>(null)
    private val _lastDeletedItem = MutableStateFlow<GroceryItem?>(null)

    val uiState: StateFlow<GroceryUiState> = combine(
        repository.allItems,
        _userMessage,
        _lastDeletedItem,
    ) { items, userMessage, lastDeletedItem ->
        val unbought = items.count { !it.isBought }
        GroceryUiState(
            items = items,
            unboughtCount = unbought,
            isLoading = false,
            userMessage = userMessage,
            lastDeletedItem = lastDeletedItem
        )
    }.stateIn(
        scope = viewModelScope,
        started = started,
        initialValue = GroceryUiState(isLoading = true)
    )

    /**
     * Validates and sanitizes item name, then inserts it into the database.
     * Prevents empty or blank entries and caps max length defensively.
     *
     * @param rawName Raw input text entered by the user.
     * @return `true` if item was valid and queued for insertion; `false` otherwise.
     */
    fun addItem(rawName: String): Boolean {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) {
            _userMessage.value = "Item name cannot be empty"
            return false
        }

        val sanitized = if (trimmed.length > MAX_ITEM_NAME_LENGTH) {
            trimmed.substring(0, MAX_ITEM_NAME_LENGTH)
        } else {
            trimmed
        }

        viewModelScope.launch {
            val existingItem = repository.getItemByName(sanitized)
            if (existingItem != null) {
                if (existingItem.isBought) {
                    val reactivated = existingItem.copy(
                        isBought = false,
                        createdAt = System.currentTimeMillis()
                    )
                    repository.restoreItem(reactivated)
                    _userMessage.value = "\"${existingItem.name}\" restored to list"
                } else {
                    _userMessage.value = "\"${existingItem.name}\" is already on your list"
                }
            } else {
                repository.addItem(sanitized)
            }
        }
        return true
    }

    private var lastToggledItemId = -1L
    private var lastToggleTime = 0L

    /**
     * Toggles an item between bought and unbought status.
     * Prevents accidental double-taps on the same item, while allowing
     * instant fast clicks across different items.
     *
     * @param item Item to toggle.
     */
    fun toggleItemBought(item: GroceryItem) {
        val now = System.currentTimeMillis()
        if (item.id == lastToggledItemId && (now - lastToggleTime < 250L)) {
            return
        }
        lastToggledItemId = item.id
        lastToggleTime = now

        viewModelScope.launch {
            repository.toggleItemStatus(item)
        }
    }

    /**
     * Deletes an item from the grocery list and stages it for potential Undo restoration.
     *
     * @param item Item to remove.
     */
    fun removeItem(item: GroceryItem) {
        _lastDeletedItem.value = item
        _userMessage.value = "\"${item.name}\" removed"
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    /**
     * Restores a deleted item back to the database.
     * If no item is passed, restores the last deleted item staged in [_lastDeletedItem].
     *
     * @param item Optional specific item to restore.
     */
    fun undoRemove(item: GroceryItem? = null) {
        val itemToRestore = item ?: _lastDeletedItem.value ?: return
        viewModelScope.launch {
            repository.restoreItem(itemToRestore)
            _lastDeletedItem.value = null
            _userMessage.value = "\"${itemToRestore.name}\" restored"
        }
    }

    /**
     * Deletes all items from the grocery list.
     */
    fun deleteAllItems() {
        viewModelScope.launch {
            val count = repository.deleteAll()
            if (count > 0) {
                _lastDeletedItem.value = null
                _userMessage.value = "All items deleted"
            }
        }
    }

    /**
     * Clears transient user messages after they have been shown (e.g. via Snackbar/Toast).
     */
    fun clearUserMessage() {
        _userMessage.value = null
    }
}

/**
 * Factory for creating [GroceryViewModel] with its repository dependency.
 */
class GroceryViewModelFactory(
    private val repository: GroceryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroceryViewModel::class.java)) {
            return GroceryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
