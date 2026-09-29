package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_movements",
    indices = [Index(value = ["product_id"])]
)
data class StockMovement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "movement_type")
    val movementType: String, // "إضافة مخزون", "بيع", "تعديل يدوي", "حذف منتج"
    @ColumnInfo(name = "quantity")
    val quantity: Int,
    @ColumnInfo(name = "previous_quantity")
    val previousQuantity: Int,
    @ColumnInfo(name = "new_quantity")
    val newQuantity: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
