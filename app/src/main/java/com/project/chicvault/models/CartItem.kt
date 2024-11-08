package com.project.chicvault.models

data class CartItem(
    var id: String = "",
    val productName: String = "",
    val price: Double = 0.0,
    var quantity: Int = 1,
    val imageUrl: String = ""
)
