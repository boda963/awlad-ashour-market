package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "invoice_number")
    val invoiceNumber: String,
    @ColumnInfo(name = "total_amount")
    val totalAmount: Double,
    @ColumnInfo(name = "total_cost")
    val totalCost: Double,
    @ColumnInfo(name = "total_profit")
    val totalProfit: Double,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
