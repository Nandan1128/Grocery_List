package com.example.grocerylist.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.grocerylist.R
import com.example.grocerylist.data.local.GroceryItem
import com.example.grocerylist.databinding.ItemGroceryBinding


/**
 * RecyclerView ListAdapter for displaying grocery items with DiffUtil animations.
 *
 * @param onItemToggle Callback invoked when an item's check status is toggled.
 * @param onItemDelete Callback invoked when an item is deleted.
 */
class GroceryListAdapter(
    private val onItemToggle: (GroceryItem) -> Unit,
    private val onItemDelete: (GroceryItem) -> Unit
) : ListAdapter<GroceryItem, GroceryListAdapter.GroceryViewHolder>(GroceryDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroceryViewHolder {
        val binding = ItemGroceryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroceryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroceryViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class GroceryViewHolder(
        private val binding: ItemGroceryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Tapping the card (including the checkbox area) toggles bought status
            binding.itemContainer.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemToggle(getItem(position))
                }
            }

            // Tapping delete button removes the item
            binding.btnDelete.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemDelete(getItem(position))
                }
            }
        }

        fun bind(item: GroceryItem, position: Int) {
            val context = binding.root.context
            binding.tvSerialNumber.text = "${position + 1}."
            binding.tvItemName.text = item.name

            // Consistent checkbox checked state
            binding.cbBought.isChecked = item.isBought

            // Strikethrough, colors, and background styling
            if (item.isBought) {
                binding.itemContainer.setBackgroundResource(R.drawable.bg_item_card_bought)
                binding.tvItemName.paint.isStrikeThruText = true
                binding.tvItemName.setTextColor(
                    ContextCompat.getColor(context, R.color.text_strikethrough)
                )
                binding.tvSerialNumber.setTextColor(
                    ContextCompat.getColor(context, R.color.text_strikethrough)
                )
            } else {
                binding.itemContainer.setBackgroundResource(R.drawable.bg_item_card)
                binding.tvItemName.paint.isStrikeThruText = false
                binding.tvItemName.setTextColor(
                    ContextCompat.getColor(context, R.color.text_primary)
                )
                binding.tvSerialNumber.setTextColor(
                    ContextCompat.getColor(context, R.color.text_secondary)
                )
            }

            // Ensure alpha is always 1.0f so recycled views never get stuck with dimming/transparency
            binding.itemContainer.alpha = 1.0f
        }
    }

    companion object GroceryDiffCallback : DiffUtil.ItemCallback<GroceryItem>() {
        override fun areItemsTheSame(oldItem: GroceryItem, newItem: GroceryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GroceryItem, newItem: GroceryItem): Boolean {
            // Return false so DiffUtil triggers a rebind whenever items reorder or change positions.
            // This ensures onBindViewHolder is called for shifted views so tvSerialNumber always
            // displays the correct adapter position (${position + 1}.), preventing stale or duplicate
            // serial numbers during rapid clicks or list updates.
            return false
        }
    }
}
