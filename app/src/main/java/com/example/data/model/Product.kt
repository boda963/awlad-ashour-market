package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [Index(value = ["barcode"], unique = true)]
)
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "barcode")
    val barcode: String,
    @ColumnInfo(name = "product_name")
    val productName: String,
    @ColumnInfo(name = "quantity")
    val quantity: Int,
    @ColumnInfo(name = "wholesale_price")
    val wholesalePrice: Double,
    @ColumnInfo(name = "selling_price")
    val sellingPrice: Double,
    @ColumnInfo(name = "low_stock_limit")
    val lowStockLimit: Int = 5,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    val unitProfit: Double
        get() = sellingPrice - wholesalePrice

    val totalWholesaleValue: Double
        get() = wholesalePrice * quantity

    val totalSellingValue: Double
        get() = sellingPrice * quantity

    val isOutOfStock: Boolean
        get() = quantity <= 0

    val isLowStock: Boolean
        get() = quantity in 1..lowStockLimit
}
