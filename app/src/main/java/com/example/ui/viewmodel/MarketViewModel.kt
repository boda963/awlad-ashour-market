package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.repository.CartItem
import com.example.data.repository.FinancialSummary
import com.example.data.repository.MarketRepository
import com.example.data.repository.SaleResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class Screen {
    SPLASH,
    HOME,
    POS,
    ADD_STOCK,
    INVENTORY,
    REPORTS,
    INVOICES,
    SETTINGS
}

enum class StockFilter {
    ALL,
    LOW_STOCK,
    OUT_OF_STOCK
}

enum class ReportPeriod {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    CUSTOM
}

class MarketViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MarketRepository = MarketRepository(AppDatabase.getDatabase(application))
    val currentScreen = MutableStateFlow(Screen.SPLASH)

    // --- Navigation ---
    fun navigateTo(screen: Screen) {
        currentScreen.value = screen
    }

    // --- Home Screen Stats ---
    val todaySales = MutableStateFlow(0.0)
    val todayProfit = MutableStateFlow(0.0)
    val todayInvoicesCount = MutableStateFlow(0)
    val lowStockCount: StateFlow<Int> = repository.lowStockCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun loadTodayStats() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis

        viewModelScope.launch {
            repository.getTodaySalesTotal(startOfDay, endOfDay).collect {
                todaySales.value = it
            }
        }
        viewModelScope.launch {
            repository.getTodayProfitTotal(startOfDay, endOfDay).collect {
                todayProfit.value = it
            }
        }
        viewModelScope.launch {
            repository.getTodayInvoiceCount(startOfDay, endOfDay).collect {
                todayInvoicesCount.value = it
            }
        }
    }

    // --- POS (Point of Sale) ---
    val cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val lastCompletedSale = MutableStateFlow<Sale?>(null)
    val lastSaleItems = MutableStateFlow<List<SaleItem>>(emptyList())
    val showSaleSuccessDialog = MutableStateFlow(false)
    val posErrorMessage = MutableStateFlow<String?>(null)

    val cartTotal: StateFlow<Double> = cartItems.combine(cartItems) { items, _ ->
        items.sumOf { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun addBarcodeToCart(barcode: String) {
        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode.trim())
            if (product == null) {
                posErrorMessage.value = "المنتج ذو الباركود ($barcode) غير مسجل في المخزن"
                return@launch
            }
            if (product.quantity <= 0) {
                posErrorMessage.value = "المنتج '${product.productName}' نفد من المخزون!"
                return@launch
            }
            addProductToCart(product)
        }
    }

    fun addProductToCart(product: Product) {
        val current = cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            val currentItem = current[index]
            if (currentItem.quantity + 1 > product.quantity) {
                posErrorMessage.value = "الكمية المتاحة من '${product.productName}' هي (${product.quantity}) فقط."
                return
            }
            current[index] = currentItem.copy(quantity = currentItem.quantity + 1)
        } else {
            current.add(CartItem(product = product, quantity = 1))
        }
        cartItems.value = current
        posErrorMessage.value = null
    }

    fun incrementCartItem(productId: Long) {
        val current = cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            val item = current[index]
            if (item.quantity + 1 > item.product.quantity) {
                posErrorMessage.value = "الكمية المتاحة من '${item.product.productName}' هي (${item.product.quantity}) فقط."
                return
            }
            current[index] = item.copy(quantity = item.quantity + 1)
            cartItems.value = current
            posErrorMessage.value = null
        }
    }

    fun decrementCartItem(productId: Long) {
        val current = cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            val item = current[index]
            if (item.quantity > 1) {
                current[index] = item.copy(quantity = item.quantity - 1)
                cartItems.value = current
            } else {
                current.removeAt(index)
                cartItems.value = current
            }
            posErrorMessage.value = null
        }
    }

    fun setCartItemQuantity(productId: Long, qty: Int) {
        val current = cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            val item = current[index]
            if (qty <= 0) {
                current.removeAt(index)
            } else if (qty > item.product.quantity) {
                posErrorMessage.value = "الكمية المتاحة من '${item.product.productName}' هي (${item.product.quantity}) فقط."
                return
            } else {
                current[index] = item.copy(quantity = qty)
            }
            cartItems.value = current
            posErrorMessage.value = null
        }
    }

    fun removeCartItem(productId: Long) {
        cartItems.value = cartItems.value.filterNot { it.product.id == productId }
    }

    fun clearCart() {
        cartItems.value = emptyList()
        posErrorMessage.value = null
    }

    fun checkoutSale() {
        val items = cartItems.value
        if (items.isEmpty()) {
            posErrorMessage.value = "السلة فارغة. يرجى مسح أو إضافة منتجات أولاً."
            return
        }

        viewModelScope.launch {
            when (val result = repository.completeSaleTransaction(items)) {
                is SaleResult.Success -> {
                    lastCompletedSale.value = result.sale
                    lastSaleItems.value = result.items
                    showSaleSuccessDialog.value = true
                    cartItems.value = emptyList()
                    posErrorMessage.value = null
                    loadTodayStats()
                    loadReport(ReportPeriod.TODAY)
                }
                is SaleResult.Error -> {
                    posErrorMessage.value = result.message
                }
            }
        }
    }

    fun dismissSaleSuccessDialog() {
        showSaleSuccessDialog.value = false
        lastCompletedSale.value = null
        lastSaleItems.value = emptyList()
    }

    // --- Add to Stock Screen ---
    val addStockBarcode = MutableStateFlow("")
    val addStockProductName = MutableStateFlow("")
    val addStockQuantity = MutableStateFlow("1")
    val addStockWholesalePrice = MutableStateFlow("")
    val addStockSellingPrice = MutableStateFlow("")
    val existingProductFound = MutableStateFlow<Product?>(null)
    val addStockSuccessMessage = MutableStateFlow<String?>(null)
    val addStockErrorMessage = MutableStateFlow<String?>(null)

    fun onAddStockBarcodeScanned(barcode: String) {
        addStockBarcode.value = barcode.trim()
        lookupBarcodeForStock(barcode.trim())
    }

    fun lookupBarcodeForStock(barcode: String) {
        if (barcode.isBlank()) return
        viewModelScope.launch {
            val existing = repository.getProductByBarcode(barcode.trim())
            if (existing != null) {
                existingProductFound.value = existing
                addStockProductName.value = existing.productName
                addStockWholesalePrice.value = if (existing.wholesalePrice % 1.0 == 0.0) existing.wholesalePrice.toInt().toString() else existing.wholesalePrice.toString()
                addStockSellingPrice.value = if (existing.sellingPrice % 1.0 == 0.0) existing.sellingPrice.toInt().toString() else existing.sellingPrice.toString()
                addStockErrorMessage.value = null
            } else {
                existingProductFound.value = null
            }
        }
    }

    fun saveStockEntry() {
        val barcode = addStockBarcode.value.trim()
        val name = addStockProductName.value.trim()
        val qty = addStockQuantity.value.toIntOrNull() ?: -1
        val wholesale = addStockWholesalePrice.value.toDoubleOrNull() ?: -1.0
        val selling = addStockSellingPrice.value.toDoubleOrNull() ?: -1.0

        if (barcode.isBlank()) {
            addStockErrorMessage.value = "يرجى مسح أو إدخال الباركود"
            return
        }
        if (name.isBlank()) {
            addStockErrorMessage.value = "يرجى كتابة اسم المنتج"
            return
        }
        if (qty < 0) {
            addStockErrorMessage.value = "يرجى إدخال كمية صحيحة (صفر أو أكثر)"
            return
        }
        if (wholesale < 0) {
            addStockErrorMessage.value = "سعر الجملة غير صالح"
            return
        }
        if (selling < 0) {
            addStockErrorMessage.value = "سعر البيع غير صالح"
            return
        }

        viewModelScope.launch {
            val result = repository.saveOrUpdateProduct(
                barcode = barcode,
                name = name,
                quantityToAdd = qty,
                wholesalePrice = wholesale,
                sellingPrice = selling
            )
            result.onSuccess { product ->
                addStockSuccessMessage.value = if (existingProductFound.value != null) {
                    "تم تحديث المخزون للمنتج (${product.productName}) بنجاح. الكمية الإجمالية: ${product.quantity}"
                } else {
                    "تمت إضافة المنتج الجديد (${product.productName}) بنجاح بالمخزن"
                }
                addStockErrorMessage.value = null
                resetAddStockForm()
            }.onFailure { err ->
                addStockErrorMessage.value = err.message ?: "فشلت عملية الحفظ"
            }
        }
    }

    fun resetAddStockForm() {
        addStockBarcode.value = ""
        addStockProductName.value = ""
        addStockQuantity.value = "1"
        addStockWholesalePrice.value = ""
        addStockSellingPrice.value = ""
        existingProductFound.value = null
    }

    // --- Inventory Screen ---
    val inventorySearchQuery = MutableStateFlow("")
    val inventoryStockFilter = MutableStateFlow(StockFilter.ALL)
    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inspectedProduct = MutableStateFlow<Product?>(null)
    val showInspectDialog = MutableStateFlow(false)
    val productToEdit = MutableStateFlow<Product?>(null)
    val showEditProductDialog = MutableStateFlow(false)
    val productToAddStock = MutableStateFlow<Product?>(null)
    val showAddStockDialog = MutableStateFlow(false)
    val productToDelete = MutableStateFlow<Product?>(null)
    val showDeleteConfirmDialog = MutableStateFlow(false)

    fun inspectBarcode(barcode: String) {
        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode.trim())
            inspectedProduct.value = product
            showInspectDialog.value = true
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product)
            showEditProductDialog.value = false
            productToEdit.value = null
        }
    }

    fun addStockToExisting(productId: Long, qty: Int) {
        viewModelScope.launch {
            repository.addStockQuantity(productId, qty)
            showAddStockDialog.value = false
            productToAddStock.value = null
        }
    }

    fun confirmDeleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            showDeleteConfirmDialog.value = false
            productToDelete.value = null
        }
    }

    // --- Financial Reports Screen ---
    val reportPeriod = MutableStateFlow(ReportPeriod.TODAY)
    val customStartDate = MutableStateFlow(System.currentTimeMillis() - 86400000L * 7)
    val customEndDate = MutableStateFlow(System.currentTimeMillis())
    val financialSummary = MutableStateFlow(
        FinancialSummary(0.0, 0.0, 0.0, 0, 0, 0.0)
    )

    fun loadReport(period: ReportPeriod) {
        reportPeriod.value = period
        val (startTime, endTime) = when (period) {
            ReportPeriod.TODAY -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val s = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val e = cal.timeInMillis
                Pair(s, e)
            }
            ReportPeriod.THIS_WEEK -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val s = cal.timeInMillis
                val e = System.currentTimeMillis()
                Pair(s, e)
            }
            ReportPeriod.THIS_MONTH -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val s = cal.timeInMillis
                val e = System.currentTimeMillis()
                Pair(s, e)
            }
            ReportPeriod.CUSTOM -> {
                Pair(customStartDate.value, customEndDate.value)
            }
        }

        viewModelScope.launch {
            financialSummary.value = repository.getFinancialSummary(startTime, endTime)
        }
    }

    fun setCustomReportDates(start: Long, end: Long) {
        customStartDate.value = start
        customEndDate.value = end
        loadReport(ReportPeriod.CUSTOM)
    }

    // --- Invoices Screen ---
    val allSales: StateFlow<List<Sale>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedInvoice = MutableStateFlow<Sale?>(null)
    val selectedInvoiceItems = MutableStateFlow<List<SaleItem>>(emptyList())
    val showInvoiceDetailDialog = MutableStateFlow(false)

    fun openInvoiceDetail(sale: Sale) {
        viewModelScope.launch {
            selectedInvoice.value = sale
            selectedInvoiceItems.value = repository.getItemsForSaleList(sale.id)
            showInvoiceDetailDialog.value = true
        }
    }

    fun dismissInvoiceDetail() {
        showInvoiceDetailDialog.value = false
        selectedInvoice.value = null
        selectedInvoiceItems.value = emptyList()
    }

    // --- Settings, Backup & Restore ---
    val settingsStatusMessage = MutableStateFlow<String?>(null)
    val isSettingsError = MutableStateFlow(false)
    val backupJsonContent = MutableStateFlow<String?>(null)
    val showBackupDialog = MutableStateFlow(false)

    fun generateBackup() {
        viewModelScope.launch {
            try {
                val json = repository.createBackupJson()
                backupJsonContent.value = json
                showBackupDialog.value = true
                settingsStatusMessage.value = "تم إنشاء النسخة الاحتياطية بنجاح"
                isSettingsError.value = false
            } catch (e: Exception) {
                settingsStatusMessage.value = "فشل إنشاء النسخة الاحتياطية: ${e.message}"
                isSettingsError.value = true
            }
        }
    }

    fun restoreBackup(json: String) {
        viewModelScope.launch {
            val result = repository.restoreBackupJson(json)
            result.onSuccess { msg ->
                settingsStatusMessage.value = msg
                isSettingsError.value = false
                loadTodayStats()
                loadReport(ReportPeriod.TODAY)
            }.onFailure { err ->
                settingsStatusMessage.value = "فشلت الاستعادة: ${err.message}"
                isSettingsError.value = true
            }
        }
    }

    suspend fun getExportProductsCsv(): String = repository.exportProductsCsv()

    suspend fun getExportSalesCsv(): String = repository.exportSalesCsv()

    init {
        loadTodayStats()
        loadReport(ReportPeriod.TODAY)
    }
}
