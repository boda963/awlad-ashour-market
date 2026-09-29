package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import kotlinx.coroutines.flow.Flow

data class SaleWithItems(
    val sale: Sale,
    val items: List<SaleItem>
)

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY created_at DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales ORDER BY created_at DESC")
    suspend fun getAllSalesList(): List<Sale>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getSaleById(id: Long): Sale?

    @Query("SELECT * FROM sales WHERE created_at >= :startTime AND created_at <= :endTime ORDER BY created_at DESC")
    fun getSalesBetween(startTime: Long, endTime: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE created_at >= :startTime AND created_at <= :endTime ORDER BY created_at DESC")
    suspend fun getSalesBetweenList(startTime: Long, endTime: Long): List<Sale>

    @Query("SELECT * FROM sale_items WHERE sale_id = :saleId")
    fun getItemsForSale(saleId: Long): Flow<List<SaleItem>>

    @Query("SELECT * FROM sale_items WHERE sale_id = :saleId")
    suspend fun getItemsForSaleList(saleId: Long): List<SaleItem>

    @Query("SELECT * FROM sale_items")
    suspend fun getAllSaleItemsList(): List<SaleItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSales(sales: List<Sale>)

    @Query("SELECT COUNT(*) FROM sales WHERE created_at >= :startTime AND created_at <= :endTime")
    fun getTodayInvoiceCount(startTime: Long, endTime: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(total_amount), 0.0) FROM sales WHERE created_at >= :startTime AND created_at <= :endTime")
    fun getTodaySalesTotal(startTime: Long, endTime: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(total_profit), 0.0) FROM sales WHERE created_at >= :startTime AND created_at <= :endTime")
    fun getTodayProfitTotal(startTime: Long, endTime: Long): Flow<Double>

    @Query("DELETE FROM sales")
    suspend fun clearAllSales()

    @Query("DELETE FROM sale_items")
    suspend fun clearAllSaleItems()
}
