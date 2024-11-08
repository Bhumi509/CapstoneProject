package com.project.chicvault.models

data class Price(
    val amount: Double = 0.0,
    val currency: String = "CAD"
)

data class Product(
    val name: String = "",
    val price: Price = Price(),
    val imageUrl: String = "",
    val description: String = "",
    val available: Boolean = true,
    val rating: Double = 0.0,
    val reviews: Int = 0,
    val stock: Int = 0,
    val tags: List<String> = listOf()
)
