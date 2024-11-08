package com.project.chicvault.views

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.project.chicvault.R
import com.project.chicvault.adapter.CategoriesAdapter
import com.project.chicvault.adapter.FeaturedProductsAdapter
import com.project.chicvault.adapter.ProductsAdapter
import com.project.chicvault.models.Product
import com.project.chicvault.constants.ACTION_UPDATE_CART_BADGE

class MainActivity : AppCompatActivity() {

    private lateinit var featuredProductsRecyclerView: RecyclerView
    private lateinit var featuredProductsAdapter: FeaturedProductsAdapter
    private val featuredProductList = ArrayList<Product>()

    private lateinit var productsRecyclerView: RecyclerView
    private lateinit var allProductsAdapter: ProductsAdapter
    private val allProductList = ArrayList<Product>()

    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var categoriesAdapter: CategoriesAdapter
    private val categoryList = ArrayList<String>()

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var ivMenu: ImageView
    private lateinit var ivCart: ImageView
    private lateinit var cartBadge: TextView

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val productsRef = FirebaseDatabase.getInstance().getReference("products")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        ivMenu = findViewById(R.id.ivMenu)
        ivCart = findViewById(R.id.ivCart)
        cartBadge = findViewById(R.id.cartBadge)

        ivMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        ivCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        // Register broadcast receiver to listen for cart updates
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(updateCartBadgeReceiver, IntentFilter(ACTION_UPDATE_CART_BADGE), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(updateCartBadgeReceiver, IntentFilter(ACTION_UPDATE_CART_BADGE))
        }
        // Set up RecyclerViews and adapters
        setupRecyclerViews()
        fetchCategories()
        updateCartBadge()
    }
    override fun onResume() {
        super.onResume()
        // Update badge every time activity resumes
        updateCartBadge()
    }
    private fun setupRecyclerViews() {
        featuredProductsRecyclerView = findViewById(R.id.featuredProductsRecyclerView)
        featuredProductsRecyclerView.layoutManager = GridLayoutManager(this, 2)
        featuredProductsAdapter = FeaturedProductsAdapter(featuredProductList)
        featuredProductsRecyclerView.adapter = featuredProductsAdapter

        productsRecyclerView = findViewById(R.id.productsRecyclerView)
        productsRecyclerView.layoutManager = LinearLayoutManager(this)
        allProductsAdapter = ProductsAdapter(allProductList, onCartUpdated = { updateCartBadge() })
        productsRecyclerView.adapter = allProductsAdapter

        categoryRecyclerView = findViewById(R.id.categoryRecyclerView)
        categoryRecyclerView.layoutManager = LinearLayoutManager(this)
        categoriesAdapter = CategoriesAdapter(categoryList) { category ->
            fetchProductsByCategory(category)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        categoryRecyclerView.adapter = categoriesAdapter
    }

    private fun fetchCategories() {
        productsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryList.clear()
                for (categorySnapshot in snapshot.children) {
                    val category = categorySnapshot.key
                    if (category != null) {
                        categoryList.add(category)
                    }
                }
                categoriesAdapter.notifyDataSetChanged()
                if (categoryList.isNotEmpty()) {
                    fetchProductsByCategory(categoryList[0])
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("MainActivity", "Failed to load categories.", error.toException())
            }
        })
    }

    private fun fetchProductsByCategory(category: String) {
        val categoryRef = productsRef.child(category)
        categoryRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                featuredProductList.clear()
                allProductList.clear()
                var count = 0

                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    product?.let {
                        if (count < 2) {
                            featuredProductList.add(it)
                            count++
                        }
                        allProductList.add(it)
                    }
                }
                featuredProductsAdapter.notifyDataSetChanged()
                allProductsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("MainActivity", "Failed to load products.", error.toException())
            }
        })
    }

    private fun updateCartBadge() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            cartBadge.visibility = View.GONE
            return
        }

        firestore.collection("users").document(userId).collection("cart").get()
            .addOnSuccessListener { documents ->
                val itemCount = documents.size()
                if (itemCount > 0) {
                    cartBadge.visibility = View.VISIBLE
                    cartBadge.text = itemCount.toString()
                } else {
                    cartBadge.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                cartBadge.visibility = View.GONE
            }
    }

    private val updateCartBadgeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateCartBadge()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(updateCartBadgeReceiver)
    }
}
