package com.project.chicvault.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.project.chicvault.R
import com.project.chicvault.models.CartItem

class CartAdapter(
    private val cartItemList: List<CartItem>,
    private val onRemoveItem: (CartItem) -> Unit,
    private val onIncreaseQuantity: (CartItem) -> Unit,
    private val onDecreaseQuantity: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.productImage)
        val productTitle: TextView = itemView.findViewById(R.id.productTitle)
        val productPrice: TextView = itemView.findViewById(R.id.productPrice)
        val quantityText: TextView = itemView.findViewById(R.id.quantityText)
        val increaseButton: ImageView = itemView.findViewById(R.id.increaseQuantityButton)
        val decreaseButton: ImageView = itemView.findViewById(R.id.decreaseQuantityButton)
        val removeButton: ImageView = itemView.findViewById(R.id.removeProductButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cart_item_layout, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartItemList[position]
        holder.productTitle.text = item.productName
        holder.productPrice.text = "$${String.format("%.2f", item.price * item.quantity)}"
        holder.quantityText.text = item.quantity.toString()

        // Check if imageUrl is not null or empty and load the image accordingly
        if (!item.imageUrl.isNullOrEmpty()) {
            val storageReference: StorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(item.imageUrl)
            Glide.with(holder.itemView.context)
                .load(storageReference)
                .placeholder(R.drawable.product_1) // Placeholder while loading
                .error(R.drawable.product_1) // Fallback if loading fails
                .into(holder.productImage)
        } else {
            // Load placeholder image if imageUrl is null or empty
            Glide.with(holder.itemView.context)
                .load(R.drawable.product_1) // Placeholder image
                .into(holder.productImage)
        }

        // Set up click listeners for quantity and remove actions
        holder.increaseButton.setOnClickListener { onIncreaseQuantity(item) }
        holder.decreaseButton.setOnClickListener { onDecreaseQuantity(item) }
        holder.removeButton.setOnClickListener { onRemoveItem(item) }
    }

    override fun getItemCount() = cartItemList.size
}
