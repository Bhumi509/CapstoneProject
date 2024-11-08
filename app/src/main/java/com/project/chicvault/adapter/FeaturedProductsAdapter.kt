package com.project.chicvault.adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.project.chicvault.R
import com.project.chicvault.models.Product
import com.project.chicvault.views.ProductDetailActivity

class FeaturedProductsAdapter(private val featuredProducts: List<Product>) :
    RecyclerView.Adapter<FeaturedProductsAdapter.FeaturedProductViewHolder>() {

    private val TAG = "FeaturedProductsAdapter"

    inner class FeaturedProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productName: TextView = itemView.findViewById(R.id.productName)
        val productPrice: TextView = itemView.findViewById(R.id.productPrice)
        val productImage: ImageView = itemView.findViewById(R.id.productImage)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val product = featuredProducts[position]
                    val context = itemView.context
                    val intent = Intent(context, ProductDetailActivity::class.java).apply {
                        putExtra("productName", product.name)
                        putExtra("productPrice", product.price.amount)
                        putExtra("productCurrency", product.price.currency)
                        putExtra("productImageUrl", product.imageUrl)
                        putExtra("productDescription", product.description)
                        putExtra("productAvailable", product.available)
                        putExtra("productRating", product.rating)
                        putExtra("productReviews", product.reviews)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeaturedProductViewHolder {
        Log.d(TAG, "onCreateViewHolder: inflating layout for a new view holder")
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_featured_product, parent, false)
        return FeaturedProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FeaturedProductViewHolder, position: Int) {
        val product = featuredProducts[position]
        Log.d(TAG, "onBindViewHolder: binding product at position $position: $product")

        // Bind product details to the views
        holder.productName.text = product.name
        holder.productPrice.text = "${product.price.currency} ${product.price.amount}"

        // Load product image from Firebase Storage if the imageUrl is valid
        if (product.imageUrl.isNotEmpty()) {
            val storageReference: StorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(product.imageUrl)
            Glide.with(holder.itemView.context)
                .load(storageReference)
                .placeholder(R.drawable.product_1) // Replace with your placeholder image resource
                .into(holder.productImage)
        } else {
            // Load a placeholder image if imageUrl is empty
            Glide.with(holder.itemView.context)
                .load(R.drawable.product_1) // Replace with your placeholder image resource
                .into(holder.productImage)
        }
    }

    override fun getItemCount(): Int {
        val size = featuredProducts.size
        Log.d(TAG, "getItemCount: total items = $size")
        return size
    }
}
