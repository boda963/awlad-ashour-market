package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.db.SaleWithItems
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.StockMovement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CartItem(
    val product: Product,
    var quantity: Int
) {
    val totalAmount: Double
        get() = product.sellingPrice * quantity

    val totalCost: Double
        get() = product.wholesalePrice * quantity

    val profit: Double
        get() = totalAmount - totalCost
}

sealed class SaleResult {
    data class Success(val sale: Sale, val items: List<SaleItem>) : SaleResult()
    data class Error(val message: String) : SaleResult()
}

data class FinancialSummary(
    val totalSales: Double,
    val totalCost: Double,
    val totalProfit: Double,
    val invoiceCount: Int,
    val totalItemsSold: Int,
    val averageInvoiceValue: Double
)

class MarketRepository(private val db: AppDatabase) {

    private val productDao = db.productDao()
    private val saleDao = db.saleDao()
    private val stockMovementDao = db.stockMovementDao()

    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val lowStockCount: Flow<Int> = productDao.getLowStockCount()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val allSales: Flow<List<Sale>> = saleDao.getAllSales()
    val allStockMovements: Flow<List<StockMovement>> = stockMovementDao.getAllMovements()

    fun searchProducts(query: String): Flow<List<Product>> =
        productDao.searchProducts(query)

    suspend fun getProductByBarcode(barcode: String): Product? =
        withContext(Dispatchers.IO) {
            productDao.getProductByBarcode(barcode.trim())
        }

    suspend fun getProductById(id: Long): Product? =
        withContext(Dispatchers.IO) {
            productDao.getProductById(id)
        }

    fun getItemsForSale(saleId: Long): Flow<List<SaleItem>> =
        saleDao.getItemsForSale(saleId)

    suspend fun getItemsForSaleList(saleId: Long): List<SaleItem> =
        withContext(Dispatchers.IO) {
            saleDao.getItemsForSaleList(saleId)
        }

    fun getTodaySalesTotal(startOfDay: Long, endOfDay: Long): Flow<Double> =
        saleDao.getTodaySalesTotal(startOfDay, endOfDay)

    fun getTodayProfitTotal(startOfDay: Long, endOfDay: Long): Flow<Double> =
        saleDao.getTodayProfitTotal(startOfDay, endOfDay)

    fun getTodayInvoiceCount(startOfDay: Long, endOfDay: Long): Flow<Int> =
        saleDao.getTodayInvoiceCount(startOfDay, endOfDay)

    /**
     * Add product or increase stock for existing barcode
     */
    suspend fun saveOrUpdateProduct(
        barcode: String,
        name: String,
        quantityToAdd: Int,
        wholesalePrice: Double,
        sellingPrice: Double,
        lowStockLimit: Int = 5
    ): Result<Product> = withContext(Dispatchers.IO) {
        try {
            val cleanBarcode = barcode.trim()
            val cleanName = name.trim()

            if (cleanBarcode.isBlank()) return@withContext Result.failure(IllegalArgumentException("الباركود مطلوب"))
            if (cleanName.isBlank()) return@withContext Result.failure(IllegalArgumentException("اسم المنتج مطلوب"))
            if (quantityToAdd < 0) return@withContext Result.failure(IllegalArgumentException("الكمية لا يمكن أن تكون سالبة"))
            if (wholesalePrice < 0 || sellingPrice < 0) return@withContext Result.failure(IllegalArgumentException("الأسعار لا يمكن أن تكون سالبة"))

            val now = System.currentTimeMillis()
            val existing = productDao.getProductByBarcode(cleanBarcode)

            if (existing != null) {
                val previousQty = existing.quantity
                val newQty = previousQty + quantityToAdd
                val updated = existing.copy(
                    productName = cleanName,
                    quantity = newQty,
                    wholesalePrice = wholesalePrice,
                    sellingPrice = sellingPrice,
                    lowStockLimit = lowStockLimit,
                    updatedAt = now
                )
                productDao.updateProduct(updated)

                if (quantityToAdd > 0) {
                    stockMovementDao.insertMovement(
                        StockMovement(
                            productId = existing.id,
                            movementType = "إضافة مخزون",
                            quantity = quantityToAdd,
                            previousQuantity = previousQty,
                            newQuantity = newQty,
                            createdAt = now
                        )
                    )
                }
                Result.success(updated)
            } else {
                val newProduct = Product(
                    barcode = cleanBarcode,
                    productName = cleanName,
                    quantity = quantityToAdd,
                    wholesalePrice = wholesalePrice,
                    sellingPrice = sellingPrice,
                    lowStockLimit = lowStockLimit,
                    createdAt = now,
                    updatedAt = now
                )
                val newId = productDao.insertProduct(newProduct)
                val insertedProduct = newProduct.copy(id = newId)

                stockMovementDao.insertMovement(
                    StockMovement(
                        productId = newId,
                        movementType = "إضافة منتج جديد",
                        quantity = quantityToAdd,
                        previousQuantity = 0,
                        newQuantity = quantityToAdd,
                        createdAt = now
                    )
                )
                Result.success(insertedProduct)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProduct(product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val old = productDao.getProductById(product.id)
            val now = System.currentTimeMillis()
            val updated = product.copy(updatedAt = now)
            productDao.updateProduct(updated)

            if (old != null && old.quantity != product.quantity) {
                val diff = product.quantity - old.quantity
                stockMovementDao.insertMovement(
                    StockMovement(
                        productId = product.id,
                        movementType = "تعديل كمية يدوي",
                        quantity = diff,
                        previousQuantity = old.quantity,
                        newQuantity = product.quantity,
                        createdAt = now
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addStockQuantity(productId: Long, addedQty: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (addedQty <= 0) return@withContext Result.failure(IllegalArgumentException("يجب أن تكون الكمية المضافة أكبر من صفر"))
            val product = productDao.getProductById(productId)
                ?: return@withContext Result.failure(IllegalArgumentException("المنتج غير موجود"))

            val now = System.currentTimeMillis()
            val newQty = product.quantity + addedQty
            val updated = product.copy(quantity = newQty, updatedAt = now)
            productDao.updateProduct(updated)

            stockMovementDao.insertMovement(
                StockMovement(
                    productId = product.id,
                    movementType = "إضافة مخزون",
                    quantity = addedQty,
                    previousQuantity = product.quantity,
                    newQuantity = newQty,
                    createdAt = now
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            productDao.deleteProduct(product)
            stockMovementDao.insertMovement(
                StockMovement(
                    productId = product.id,
                    movementType = "حذف منتج (${product.productName})",
                    quantity = -product.quantity,
                    previousQuantity = product.quantity,
                    newQuantity = 0,
                    createdAt = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Complete sale inside a single atomic SQLite transaction
     */
    suspend fun completeSaleTransaction(cartItems: List<CartItem>): SaleResult = withContext(Dispatchers.IO) {
        if (cartItems.isEmpty()) {
            return@withContext SaleResult.Error("سلة البيع فارغة")
        }

        try {
            var generatedSale: Sale? = null
            val createdSaleItems = mutableListOf<SaleItem>()

            db.withTransaction {
                // 1. Verify stock for all items
                val freshProducts = mutableMapOf<Long, Product>()
                for (item in cartItems) {
                    val p = productDao.getProductById(item.product.id)
                        ?: throw IllegalStateException("المنتج '${item.product.productName}' غير موجود في المخزن")

                    if (item.quantity <= 0) {
                        throw IllegalStateException("كمية '${p.productName}' يجب أن تكون أكبر من صفر")
                    }

                    if (p.quantity < item.quantity) {
                        throw IllegalStateException("الكمية المتاحة من '${p.productName}' هي (${p.quantity}) فقط. لا يمكنك بيع (${item.quantity}).")
                    }
                    freshProducts[item.product.id] = p
                }

                // 2. Compute totals based on prices at sale time
                var invoiceTotalAmount = 0.0
                var invoiceTotalCost = 0.0

                for (item in cartItems) {
                    val p = freshProducts[item.product.id]!!
                    val lineAmount = p.sellingPrice * item.quantity
                    val lineCost = p.wholesalePrice * item.quantity
                    invoiceTotalAmount += lineAmount
                    invoiceTotalCost += lineCost
                }

                val invoiceTotalProfit = invoiceTotalAmount - invoiceTotalCost
                val now = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                val invoiceNumber = "INV-${dateFormat.format(Date(now))}"

                val sale = Sale(
                    invoiceNumber = invoiceNumber,
                    totalAmount = invoiceTotalAmount,
                    totalCost = invoiceTotalCost,
                    totalProfit = invoiceTotalProfit,
                    createdAt = now
                )

                val saleId = saleDao.insertSale(sale)
                val saleWithId = sale.copy(id = saleId)
                generatedSale = saleWithId

                // 3. Create items, deduct stock, log movements
                val saleItemsToInsert = mutableListOf<SaleItem>()
                for (item in cartItems) {
                    val p = freshProducts[item.product.id]!!
                    val lineAmount = p.sellingPrice * item.quantity
                    val lineCost = p.wholesalePrice * item.quantity
                    val lineProfit = lineAmount - lineCost

                    val saleItem = SaleItem(
                        saleId = saleId,
                        productId = p.id,
                        barcode = p.barcode,
                        productName = p.productName,
                        quantity = item.quantity,
                        wholesalePrice = p.wholesalePrice,
                        sellingPrice = p.sellingPrice,
                        totalAmount = lineAmount,
                        totalCost = lineCost,
                        profit = lineProfit
                    )
                    saleItemsToInsert.add(saleItem)

                    // Deduct stock
                    val newQty = p.quantity - item.quantity
                    productDao.updateProduct(p.copy(quantity = newQty, updatedAt = now))

                    // Log stock movement
                    stockMovementDao.insertMovement(
                        StockMovement(
                            productId = p.id,
                            movementType = "بيع (فاتورة $invoiceNumber)",
                            quantity = -item.quantity,
                            previousQuantity = p.quantity,
                            newQuantity = newQty,
                            createdAt = now
                        )
                    )
                }

                saleDao.insertSaleItems(saleItemsToInsert)
                createdSaleItems.addAll(saleItemsToInsert)
            }

            SaleResult.Success(generatedSale!!, createdSaleItems)
        } catch (e: Exception) {
            SaleResult.Error(e.message ?: "حدث خطأ غير متوقع أثناء إتمام البيع")
        }
    }

    /**
     * Financial reporting over a custom period
     */
    suspend fun getFinancialSummary(startTime: Long, endTime: Long): FinancialSummary = withContext(Dispatchers.IO) {
        val sales = saleDao.getSalesBetweenList(startTime, endTime)
        var totalSales = 0.0
        var totalCost = 0.0
        var totalProfit = 0.0
        var totalItemsSold = 0

        for (s in sales) {
            totalSales += s.totalAmount
            totalCost += s.totalCost
            totalProfit += s.totalProfit

            val items = saleDao.getItemsForSaleList(s.id)
            for (i in items) {
                totalItemsSold += i.quantity
            }
        }

        val invoiceCount = sales.size
        val avgInvoice = if (invoiceCount > 0) totalSales / invoiceCount else 0.0

        FinancialSummary(
            totalSales = totalSales,
            totalCost = totalCost,
            totalProfit = totalProfit,
            invoiceCount = invoiceCount,
            totalItemsSold = totalItemsSold,
            averageInvoiceValue = avgInvoice
        )
    }

    /**
     * Backup to JSON string
     */
    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "ماركت أولاد عاشور")
        root.put("version", 1)
        root.put("exported_at", System.currentTimeMillis())
        root.put("developer", "Abdulrahman Ashour")

        // Products
        val products = productDao.getAllProductsList()
        val productsArray = JSONArray()
        for (p in products) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("barcode", p.barcode)
            obj.put("product_name", p.productName)
            obj.put("quantity", p.quantity)
            obj.put("wholesale_price", p.wholesalePrice)
            obj.put("selling_price", p.sellingPrice)
            obj.put("low_stock_limit", p.lowStockLimit)
            obj.put("created_at", p.createdAt)
            obj.put("updated_at", p.updatedAt)
            productsArray.put(obj)
        }
        root.put("products", productsArray)

        // Sales
        val sales = saleDao.getAllSalesList()
        val salesArray = JSONArray()
        for (s in sales) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("invoice_number", s.invoiceNumber)
            obj.put("total_amount", s.totalAmount)
            obj.put("total_cost", s.totalCost)
            obj.put("total_profit", s.totalProfit)
            obj.put("created_at", s.createdAt)
            salesArray.put(obj)
        }
        root.put("sales", salesArray)

        // SaleItems
        val saleItems = saleDao.getAllSaleItemsList()
        val saleItemsArray = JSONArray()
        for (si in saleItems) {
            val obj = JSONObject()
            obj.put("id", si.id)
            obj.put("sale_id", si.saleId)
            obj.put("product_id", si.productId)
            obj.put("barcode", si.barcode)
            obj.put("product_name", si.productName)
            obj.put("quantity", si.quantity)
            obj.put("wholesale_price", si.wholesalePrice)
            obj.put("selling_price", si.sellingPrice)
            obj.put("total_amount", si.totalAmount)
            obj.put("total_cost", si.totalCost)
            obj.put("profit", si.profit)
            saleItemsArray.put(obj)
        }
        root.put("sale_items", saleItemsArray)

        // StockMovements
        val movements = stockMovementDao.getAllMovementsList()
        val movementsArray = JSONArray()
        for (m in movements) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("product_id", m.productId)
            obj.put("movement_type", m.movementType)
            obj.put("quantity", m.quantity)
            obj.put("previous_quantity", m.previousQuantity)
            obj.put("new_quantity", m.newQuantity)
            obj.put("created_at", m.createdAt)
            movementsArray.put(obj)
        }
        root.put("stock_movements", movementsArray)

        root.toString(2)
    }

    /**
     * Restore from JSON
     */
    suspend fun restoreBackupJson(jsonString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val productsArray = root.optJSONArray("products") ?: JSONArray()
            val salesArray = root.optJSONArray("sales") ?: JSONArray()
            val saleItemsArray = root.optJSONArray("sale_items") ?: JSONArray()
            val movementsArray = root.optJSONArray("stock_movements") ?: JSONArray()

            val products = mutableListOf<Product>()
            for (i in 0 until productsArray.length()) {
                val obj = productsArray.getJSONObject(i)
                products.add(
                    Product(
                        id = obj.optLong("id", 0),
                        barcode = obj.getString("barcode"),
                        productName = obj.getString("product_name"),
                        quantity = obj.getInt("quantity"),
                        wholesalePrice = obj.getDouble("wholesale_price"),
                        sellingPrice = obj.getDouble("selling_price"),
                        lowStockLimit = obj.optInt("low_stock_limit", 5),
                        createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updated_at", System.currentTimeMillis())
                    )
                )
            }

            val sales = mutableListOf<Sale>()
            for (i in 0 until salesArray.length()) {
                val obj = salesArray.getJSONObject(i)
                sales.add(
                    Sale(
                        id = obj.optLong("id", 0),
                        invoiceNumber = obj.getString("invoice_number"),
                        totalAmount = obj.getDouble("total_amount"),
                        totalCost = obj.getDouble("total_cost"),
                        totalProfit = obj.getDouble("total_profit"),
                        createdAt = obj.optLong("created_at", System.currentTimeMillis())
                    )
                )
            }

            val saleItems = mutableListOf<SaleItem>()
            for (i in 0 until saleItemsArray.length()) {
                val obj = saleItemsArray.getJSONObject(i)
                saleItems.add(
                    SaleItem(
                        id = obj.optLong("id", 0),
                        saleId = obj.getLong("sale_id"),
                        productId = obj.getLong("product_id"),
                        barcode = obj.getString("barcode"),
                        productName = obj.getString("product_name"),
                        quantity = obj.getInt("quantity"),
                        wholesalePrice = obj.getDouble("wholesale_price"),
                        sellingPrice = obj.getDouble("selling_price"),
                        totalAmount = obj.getDouble("total_amount"),
                        totalCost = obj.getDouble("total_cost"),
                        profit = obj.getDouble("profit")
                    )
                )
            }

            val movements = mutableListOf<StockMovement>()
            for (i in 0 until movementsArray.length()) {
                val obj = movementsArray.getJSONObject(i)
                movements.add(
                    StockMovement(
                        id = obj.optLong("id", 0),
                        productId = obj.getLong("product_id"),
                        movementType = obj.getString("movement_type"),
                        quantity = obj.getInt("quantity"),
                        previousQuantity = obj.getInt("previous_quantity"),
                        newQuantity = obj.getInt("new_quantity"),
                        createdAt = obj.optLong("created_at", System.currentTimeMillis())
                    )
                )
            }

            db.withTransaction {
                // Clear existing
                saleDao.clearAllSaleItems()
                saleDao.clearAllSales()
                stockMovementDao.clearAll()
                productDao.clearAll()

                // Insert restored
                productDao.insertAll(products)
                saleDao.insertAllSales(sales)
                saleDao.insertSaleItems(saleItems)
                stockMovementDao.insertAll(movements)
            }

            Result.success("تمت استعادة ${products.size} منتج و ${sales.size} فاتورة بنجاح")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Export Products to CSV
     */
    suspend fun exportProductsCsv(): String = withContext(Dispatchers.IO) {
        val products = productDao.getAllProductsList()
        val sb = StringBuilder()
        sb.append("الباركود,اسم المنتج,الكمية,سعر الجملة,سعر البيع,ربح القطعة,قيمة المخزون جملة,قيمة المخزون بيع,حد الطلب\n")
        for (p in products) {
            val name = p.productName.replace(",", " ")
            val profit = p.sellingPrice - p.wholesalePrice
            sb.append("${p.barcode},${name},${p.quantity},${p.wholesalePrice},${p.sellingPrice},${profit},${p.totalWholesaleValue},${p.totalSellingValue},${p.lowStockLimit}\n")
        }
        sb.toString()
    }

    /**
     * Export Sales to CSV
     */
    suspend fun exportSalesCsv(): String = withContext(Dispatchers.IO) {
        val sales = saleDao.getAllSalesList()
        val sb = StringBuilder()
        sb.append("رقم الفاتورة,التاريخ والوقت,إجمالي الفاتورة,تكلفة البضاعة,إجمالي الربح\n")
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        for (s in sales) {
            val dateStr = df.format(Date(s.createdAt))
            sb.append("${s.invoiceNumber},${dateStr},${s.totalAmount},${s.totalCost},${s.totalProfit}\n")
        }
        sb.toString()
    }
}
