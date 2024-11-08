package com.project.chicvault.views

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.chicvault.R
import com.project.chicvault.adapter.CartAdapter
import com.project.chicvault.models.CartItem

class CartActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var continueShoppingButton: Button
    private lateinit var checkoutButton: Button
    private lateinit var subtotalValue: TextView
    private lateinit var cartRecyclerView: RecyclerView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val cartItemList = ArrayList<CartItem>()
    private lateinit var cartAdapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        // Initialize views
        backButton = findViewById(R.id.backButton)
        checkoutButton = findViewById(R.id.checkoutButton)
        subtotalValue = findViewById(R.id.subtotalValue)
        cartRecyclerView = findViewById(R.id.cartRecyclerView)

        backButton.setOnClickListener { finish() }


        // Set up RecyclerView and adapter
        cartRecyclerView.layoutManager = LinearLayoutManager(this)
        cartAdapter =
            CartAdapter(cartItemList, ::removeItem, ::increaseQuantity, ::decreaseQuantity)
        cartRecyclerView.adapter = cartAdapter

        loadCartItems()
    }

    private fun loadCartItems() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please log in to view your cart.", Toast.LENGTH_SHORT).show()
            return
        }

        // Load cart items from Firestore
        db.collection("users").document(userId).collection("cart")
            .get()
            .addOnSuccessListener { documents ->
                cartItemList.clear()
                var subtotal = 0.0
                for (document in documents) {
                    val item = document.toObject(CartItem::class.java).apply {
                        id = document.id // Store document ID for access in Firestore
                    }
                    cartItemList.add(item)
                    subtotal += item.price * item.quantity
                }
                cartAdapter.notifyDataSetChanged()
                subtotalValue.text = "$${String.format("%.2f", subtotal)}"
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load cart items.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeItem(item: CartItem) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("cart")
            .document(item.id)
            .delete()
            .addOnSuccessListener {
                cartItemList.remove(item)
                cartAdapter.notifyDataSetChanged()
                loadCartItems() // Refresh subtotal after item removal
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to remove item.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun increaseQuantity(item: CartItem) {
        val userId = auth.currentUser?.uid ?: return
        val newQuantity = item.quantity + 1
        db.collection("users").document(userId).collection("cart")
            .document(item.id)
            .update("quantity", newQuantity)
            .addOnSuccessListener {
                item.quantity = newQuantity
                cartAdapter.notifyDataSetChanged()
                updateSubtotal()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update quantity.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun decreaseQuantity(item: CartItem) {
        if (item.quantity > 1) {
            val userId = auth.currentUser?.uid ?: return
            val newQuantity = item.quantity - 1
            db.collection("users").document(userId).collection("cart")
                .document(item.id)
                .update("quantity", newQuantity)
                .addOnSuccessListener {
                    item.quantity = newQuantity
                    cartAdapter.notifyDataSetChanged()
                    updateSubtotal()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update quantity.", Toast.LENGTH_SHORT).show()
                }
        } else {
            removeItem(item)
        }
    }

    private fun updateSubtotal() {
        var subtotal = 0.0
        for (item in cartItemList) {
            subtotal += item.price * item.quantity
        }
        subtotalValue.text = "$${String.format("%.2f", subtotal)}"
    }
}
