package com.project.chicvault.adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.project.chicvault.R
import com.project.chicvault.models.Product
import com.project.chicvault.views.ProductDetailActivity

class ProductsAdapter(
    private val productList: List<Product>,
    private val onCartUpdated: () -> Unit
) : RecyclerView.Adapter<ProductsAdapter.ProductViewHolder>() {

    private val TAG = "ProductsAdapter"
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productName: TextView = itemView.findViewById(R.id.productName)
        val productPrice: TextView = itemView.findViewById(R.id.product4Price)
        val productImage: ImageView = itemView.findViewById(R.id.productImage)
        val productAddIcon: ImageView = itemView.findViewById(R.id.product4AddIcon)

        init {
            // Open product details on item click
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val product = productList[position]
                    val context = itemView.context
                    val intent = Intent(context, ProductDetailActivity::class.java).apply {
                        putExtra("productName", product.name)
                        putExtra("productPrice", product.price.amount)
                        putExtra("productCurrency", product.price.currency)
                        putExtra("productImageUrl", product.imageUrl)
                        putExtra("productDescription", product.description)
                    }
                    context.startActivity(intent)
                }
            }

            // Add to cart on productAddIcon click
            productAddIcon.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val product = productList[position]
                    addToCart(itemView.context, product)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.product_item_layout, parent, false)
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        holder.productName.text = product.name
        holder.productPrice.text = "$${product.price.amount}"

        // Load product image using Firebase Storage
        val storageReference: StorageReference? = if (product.imageUrl.isNotEmpty()) {
            FirebaseStorage.getInstance().getReferenceFromUrl(product.imageUrl)
        } else null

        Glide.with(holder.itemView.context)
            .load(storageReference ?: R.drawable.product_1)
            .placeholder(R.drawable.product_1)
            .into(holder.productImage)
    }

    override fun getItemCount(): Int = productList.size

    private fun addToCart(context: Context, product: Product) {
        val userId = auth.currentUser?.uid ?: return
        val cartRef = db.collection("users").document(userId).collection("cart")

        // Check if product is already in cart
        cartRef.whereEqualTo("productName", product.name).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Add new product to cart
                    val cartItem = hashMapOf(
                        "productName" to product.name,
                        "price" to product.price.amount,
                        "currency" to product.price.currency,
                        "imageUrl" to product.imageUrl,
                        "quantity" to 1
                    )
                    cartRef.add(cartItem).addOnSuccessListener {
                        onCartUpdated() // Notify cart update
                        context.sendBroadcast(Intent(com.project.chicvault.constants.ACTION_UPDATE_CART_BADGE))
                        Toast.makeText(context, "${product.name} added to cart", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    // Increase quantity of existing product
                    val document = documents.first()
                    val newQuantity = (document.getLong("quantity") ?: 0) + 1
                    cartRef.document(document.id).update("quantity", newQuantity)
                        .addOnSuccessListener {
                            onCartUpdated() // Notify cart update
                            context.sendBroadcast(Intent(com.project.chicvault.constants.ACTION_UPDATE_CART_BADGE))
                            Toast.makeText(
                                context,
                                "Increased quantity of ${product.name} in cart",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding to cart: ${e.message}")
                Toast.makeText(context, "Failed to add ${product.name} to cart", Toast.LENGTH_SHORT)
                    .show()
            }
    }
}
