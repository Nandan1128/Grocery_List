package com.example.grocerylist.ui

import android.graphics.Rect
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.grocerylist.R
import com.example.grocerylist.data.local.GroceryDatabase
import com.example.grocerylist.data.repository.GroceryRepositoryImpl
import com.example.grocerylist.databinding.ActivityMainBinding
import com.example.grocerylist.util.throttleClick
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

import kotlinx.coroutines.launch

/**
 * Main activity displaying the grocery shopping list.
 * Designed for 100% offline, zero-friction usage during shopping trips.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: GroceryListAdapter

    private val viewModel: GroceryViewModel by viewModels {
        val database = GroceryDatabase.getDatabase(applicationContext)
        val repository = GroceryRepositoryImpl(database.groceryDao())
        GroceryViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.getInsetsController(window, binding.rootLayout).isAppearanceLightStatusBars = true

        setupWindowInsets()
        setupRecyclerView()
        setupInputListeners()
        observeUiState()
    }

    private fun setupWindowInsets() {
        val initialHeaderPaddingTop = binding.headerContainer.paddingTop
        val initialBottomPaddingBottom = binding.bottomInputContainer.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

            binding.rootLayout.updatePadding(
                left = systemBars.left,
                right = systemBars.right
            )

            binding.headerContainer.updatePadding(
                top = initialHeaderPaddingTop + systemBars.top
            )

            binding.bottomInputContainer.updatePadding(
                bottom = initialBottomPaddingBottom + maxOf(systemBars.bottom, imeInsets.bottom)
            )

            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupRecyclerView() {
        adapter = GroceryListAdapter(
            onItemToggle = { item ->
                viewModel.toggleItemBought(item)
            },
            onItemDelete = { item ->
                viewModel.removeItem(item)
            }
        )

        val customLayoutManager = object : LinearLayoutManager(this@MainActivity) {
            override fun requestChildRectangleOnScreen(
                parent: RecyclerView,
                child: View,
                rect: Rect,
                immediate: Boolean,
                focusedChildVisible: Boolean
            ): Boolean {
                // Prevent RecyclerView from auto-scrolling to follow moved/animating children to the end
                return false
            }
        }

        binding.recyclerViewItems.apply {
            layoutManager = customLayoutManager
            adapter = this@MainActivity.adapter
            itemAnimator = null // Instantaneous, jitter-free updates without flying items or scroll jumps
        }
    }

    private fun setupInputListeners() {
        TooltipCompat.setTooltipText(binding.btnDeleteAll, getString(R.string.delete_all))

        // Delete all items button tap
        binding.btnDeleteAll.throttleClick {
            showDeleteAllConfirmationDialog()
        }

        // Add button tap
        binding.btnAddItem.throttleClick {
            submitItemInput()
        }

        // Soft keyboard 'Done' action
        binding.etItemInput.setOnEditorActionListener { _, actionId, event ->
            val isEnterKey = event != null &&
                    event.keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.action == KeyEvent.ACTION_DOWN

            if (actionId == EditorInfo.IME_ACTION_DONE || isEnterKey) {
                submitItemInput()
                true
            } else {
                false
            }
        }
    }

    private fun showDeleteAllConfirmationDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_all_confirmation_title)
            .setMessage(R.string.delete_all_confirmation_message)
            .setPositiveButton(R.string.delete_all_confirm) { _, _ ->
                viewModel.deleteAllItems()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            ContextCompat.getColor(this, R.color.action_delete)
        )
    }

    private fun submitItemInput() {
        val rawInput = binding.etItemInput.text?.toString().orEmpty()
        val wasAdded = viewModel.addItem(rawInput)
        if (wasAdded) {
            binding.etItemInput.text?.clear()
            binding.recyclerViewItems.post {
                binding.recyclerViewItems.scrollToPosition(0)
            }
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUi(state)
                }
            }
        }
    }

    private fun renderUi(state: GroceryUiState) {
        val layoutManager = binding.recyclerViewItems.layoutManager as? LinearLayoutManager
        val wasNearTop = (layoutManager?.findFirstVisibleItemPosition() ?: 0) <= 1

        // Update items list and guarantee the viewport stays at the top if the user was near the top
        adapter.submitList(state.items) {
            if (wasNearTop) {
                binding.recyclerViewItems.scrollToPosition(0)
            }
        }

        // Handle empty vs populated state
        if (state.items.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.recyclerViewItems.visibility = View.GONE
            binding.tvItemsCount.visibility = View.GONE
            binding.btnDeleteAll.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.recyclerViewItems.visibility = View.VISIBLE
            binding.tvItemsCount.visibility = View.VISIBLE
            binding.btnDeleteAll.visibility = View.VISIBLE

            binding.tvItemsCount.text = when (state.unboughtCount) {
                0 -> getString(R.string.all_items_bought)
                1 -> getString(R.string.item_remaining_single)
                else -> getString(R.string.items_remaining, state.unboughtCount)
            }
        }

        // Handle user notifications and Undo actions
        state.userMessage?.let { message ->
            val snackbar = Snackbar.make(binding.rootLayout, message, Snackbar.LENGTH_LONG).apply {
                setBackgroundTint(ContextCompat.getColor(this@MainActivity, R.color.surface))
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                setActionTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
            }
            val itemToRestore = state.lastDeletedItem
            if (itemToRestore != null) {
                snackbar.setAction(R.string.undo) {
                    viewModel.undoRemove(itemToRestore)
                }
            }
            snackbar.show()
            viewModel.clearUserMessage()
        }
    }
}
