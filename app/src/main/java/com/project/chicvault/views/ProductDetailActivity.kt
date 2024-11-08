package com.project.chicvault.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.project.chicvault.R
import com.project.chicvault.constants.ACTION_UPDATE_CART_BADGE


class ProductDetailActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var cartButton: ImageButton
    private lateinit var cartBadge: TextView
    private lateinit var productImage: ImageView
    private lateinit var productTitle: TextView
    private lateinit var productDescription: TextView
    private lateinit var productPrice: TextView
    private lateinit var decreaseQuantityButton: ImageView
    private lateinit var increaseQuantityButton: ImageView
    private lateinit var quantityText: TextView
    private lateinit var addToCartButton: Button
    private lateinit var favoriteButton: ImageButton

    private var quantity = 1
    private var isFavorite = false

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        // Initialize views
        backButton = findViewById(R.id.backButton)
        cartButton = findViewById(R.id.cartButton)
        cartBadge = findViewById(R.id.cartBadge)
        productImage = findViewById(R.id.productImage)
        productTitle = findViewById(R.id.productTitle)
        productDescription = findViewById(R.id.productDescription)
        productPrice = findViewById(R.id.productPrice)
        decreaseQuantityButton = findViewById(R.id.decreaseQuantityButton)
        increaseQuantityButton = findViewById(R.id.increaseQuantityButton)
        quantityText = findViewById(R.id.quantityText)
        addToCartButton = findViewById(R.id.addToCartButton)
        favoriteButton = findViewById(R.id.favoriteButton)

        backButton.setOnClickListener { finish() }
        cartButton.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
        }

        // Load product details from intent
        val name = intent.getStringExtra("productName") ?: "No Name"
        val price = intent.getDoubleExtra("productPrice", 0.0)
        val currency = intent.getStringExtra("productCurrency") ?: ""
        val imageUrl = intent.getStringExtra("productImageUrl") ?: ""
        val description = intent.getStringExtra("productDescription") ?: "No Description"

        productTitle.text = name
        productDescription.text = description
        productPrice.text = "$currency $price"
        loadProductImage(imageUrl)
        quantityText.text = quantity.toString()

        increaseQuantityButton.setOnClickListener { updateQuantity(1) }
        decreaseQuantityButton.setOnClickListener { updateQuantity(-1) }

        addToCartButton.setOnClickListener {
            addToCart(name, price, currency, imageUrl, description)
        }

        favoriteButton.setOnClickListener { toggleFavorite() }

        // Load cart item count for badge
        updateCartItemCount()
    }

    private fun loadProductImage(imageUrl: String) {
        val storageReference: StorageReference? = if (imageUrl.isNotEmpty()) {
            FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
        } else null

        Glide.with(this)
            .load(storageReference ?: R.drawable.product_1)
            .placeholder(R.drawable.product_1)
            .into(productImage)
    }

    private fun updateQuantity(change: Int) {
        quantity = (quantity + change).coerceAtLeast(1)
        quantityText.text = quantity.toString()
    }

    private fun toggleFavorite() {
        isFavorite = !isFavorite
        favoriteButton.setImageResource(if (isFavorite) R.drawable.pink_favorite_border_24 else R.drawable.pink_favorite_border_24)
        val favoriteStatus = if (isFavorite) "added to" else "removed from"
        Toast.makeText(this, "${productTitle.text} $favoriteStatus favorites", Toast.LENGTH_SHORT).show()
    }

    private fun addToCart(name: String, price: Double, currency: String, imageUrl: String, description: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please log in to add items to the cart.", Toast.LENGTH_SHORT).show()
            return
        }

        val cartRef = db.collection("users").document(userId).collection("cart")
        val query = cartRef.whereEqualTo("productName", name)

        query.get().addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                // Item not in cart, add it
                val cartItem = hashMapOf(
                    "productName" to name,
                    "price" to price,
                    "currency" to currency,
                    "imageUrl" to imageUrl,
                    "description" to description,
                    "quantity" to quantity
                )

                cartRef.add(cartItem).addOnSuccessListener {
                    Toast.makeText(this, "$name added to cart with quantity $quantity", Toast.LENGTH_SHORT).show()
                    updateCartItemCount()
                    sendCartUpdateBroadcast() // Send broadcast to update cart badge in MainActivity
                }
            } else {
                // Item exists, update quantity
                val document = documents.first()
                val existingQuantity = document.getLong("quantity") ?: 0
                val newQuantity = existingQuantity + quantity

                cartRef.document(document.id).update("quantity", newQuantity).addOnSuccessListener {
                    Toast.makeText(this, "$name quantity updated to $newQuantity", Toast.LENGTH_SHORT).show()
                    updateCartItemCount()
                    sendCartUpdateBroadcast() // Send broadcast to update cart badge in MainActivity
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error checking cart: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCartItemCount() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            cartBadge.visibility = View.GONE
            return
        }

        val cartRef = db.collection("users").document(userId).collection("cart")
        cartRef.get().addOnSuccessListener { documents ->
            val itemCount = documents.size()
            if (itemCount > 0) {
                cartBadge.visibility = View.VISIBLE
                cartBadge.text = itemCount.toString()
            } else {
                cartBadge.visibility = View.GONE
            }
        }.addOnFailureListener {
            cartBadge.visibility = View.GONE
        }
    }

    private fun sendCartUpdateBroadcast() {
        val intent = Intent(ACTION_UPDATE_CART_BADGE)
        sendBroadcast(intent)
    }
}
