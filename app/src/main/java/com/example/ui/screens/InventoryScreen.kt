package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Product
import com.example.ui.components.MarketScaffold
import com.example.ui.scanner.BarcodeScannerDialog
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.MarketViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.StockFilter
import java.text.NumberFormat
import java.util.Locale

@Composable
fun InventoryScreen(
    viewModel: MarketViewModel
) {
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var stockFilter by remember { mutableStateOf(StockFilter.ALL) }

    var showScannerForInspect by remember { mutableStateOf(false) }

    // Dialogs state
    val inspectedProduct by viewModel.inspectedProduct.collectAsStateWithLifecycle()
    val showInspectDialog by viewModel.showInspectDialog.collectAsStateWithLifecycle()

    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var editName by remember { mutableStateOf("") }
    var editWholesale by remember { mutableStateOf("") }
    var editSelling by remember { mutableStateOf("") }
    var editQuantity by remember { mutableStateOf("") }

    var productToAddStock by remember { mutableStateOf<Product?>(null) }
    var addStockQtyText by remember { mutableStateOf("10") }

    var productToDelete by remember { mutableStateOf<Product?>(null) }

    val currencyFormat = remember {
        NumberFormat.getNumberInstance(Locale("ar", "EG")).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
    }

    val filteredProducts = remember(products, searchQuery, stockFilter) {
        products.filter { p ->
            val matchesQuery = searchQuery.isBlank() ||
                    p.productName.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (stockFilter) {
                StockFilter.ALL -> true
                StockFilter.LOW_STOCK -> p.quantity in 1..p.lowStockLimit
                StockFilter.OUT_OF_STOCK -> p.quantity <= 0
            }

            matchesQuery && matchesFilter
        }
    }

    MarketScaffold(
        title = "المخزن وجرد البضاعة",
        onBack = { viewModel.navigateTo(Screen.HOME) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Action Bar: Scan to Inspect + Search
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showScannerForInspect = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("inventory_inspect_barcode_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فحص منتج بالباركود", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Button(
                    onClick = { viewModel.navigateTo(Screen.ADD_STOCK) },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("inventory_add_new_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة صنف", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم المنتج أو الباركود...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inventory_search_input")
            )

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = stockFilter == StockFilter.ALL,
                    onClick = { stockFilter = StockFilter.ALL },
                    label = { Text("الكل (${products.size})") },
                    modifier = Modifier.testTag("filter_all")
                )
                FilterChip(
                    selected = stockFilter == StockFilter.LOW_STOCK,
                    onClick = { stockFilter = StockFilter.LOW_STOCK },
                    label = { Text("أوشكت على النفاد (${products.count { it.quantity in 1..it.lowStockLimit }})") },
                    modifier = Modifier.testTag("filter_low")
                )
                FilterChip(
                    selected = stockFilter == StockFilter.OUT_OF_STOCK,
                    onClick = { stockFilter = StockFilter.OUT_OF_STOCK },
                    label = { Text("نفدت (${products.count { it.quantity <= 0 }})") },
                    modifier = Modifier.testTag("filter_out")
                )
            }

            // Products List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "لا توجد نتائج للبحث '$searchQuery'" else "المخزن فارغ حالياً",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("inventory_product_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductItemCard(
                            product = product,
                            currencyFormat = currencyFormat,
                            onInspect = {
                                viewModel.inspectedProduct.value = product
                                viewModel.showInspectDialog.value = true
                            },
                            onEdit = {
                                productToEdit = product
                                editName = product.productName
                                editWholesale = product.wholesalePrice.toString()
                                editSelling = product.sellingPrice.toString()
                                editQuantity = product.quantity.toString()
                            },
                            onAddStock = {
                                productToAddStock = product
                                addStockQtyText = "10"
                            },
                            onDelete = {
                                productToDelete = product
                            }
                        )
                    }
                }
            }
        }
    }

    // Inspect Barcode Scanner
    if (showScannerForInspect) {
        BarcodeScannerDialog(
            title = "فحص بيانات منتج",
            onBarcodeScanned = { code ->
                viewModel.inspectBarcode(code)
            },
            onDismiss = { showScannerForInspect = false }
        )
    }

    // Inspect Product Detail Dialog
    if (showInspectDialog) {
        val p = inspectedProduct
        Dialog(onDismissRequest = { viewModel.showInspectDialog.value = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("inspect_product_dialog"),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (p == null) {
                        Text(
                            text = "لم يتم العثور على المنتج بالباركود المدخل!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                    } else {
                        Text(
                            text = p.productName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "باركود: ${p.barcode}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )

                        // Status badge
                        if (p.quantity <= 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFEBEE)
                            ) {
                                Text(
                                    text = "نفد من المخزون",
                                    color = Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 12.sp
                                )
                            }
                        } else if (p.quantity <= p.lowStockLimit) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFF3E0)
                            ) {
                                Text(
                                    text = "الكمية أوشكت على النفاد",
                                    color = WarningOrange,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        HorizontalDivider()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الكمية المتبقية:")
                            Text("${p.quantity} قطعة", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("سعر الجملة:")
                            Text("${currencyFormat.format(p.wholesalePrice)} ج.م", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("سعر البيع:")
                            Text("${currencyFormat.format(p.sellingPrice)} ج.م", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ربح القطعة:")
                            Text(
                                "${currencyFormat.format(p.unitProfit)} ج.م",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("قيمة المخزون بسعر الجملة:")
                            Text("${currencyFormat.format(p.totalWholesaleValue)} ج.م", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("قيمة المخزون بسعر البيع:")
                            Text("${currencyFormat.format(p.totalSellingValue)} ج.م", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.showInspectDialog.value = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إغلاق")
                    }
                }
            }
        }
    }

    // Edit Product Dialog
    productToEdit?.let { p ->
        AlertDialog(
            onDismissRequest = { productToEdit = null },
            title = { Text("تعديل بيانات المنتج") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("اسم المنتج") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editQuantity,
                        onValueChange = { editQuantity = it },
                        label = { Text("الكمية الحالية") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editWholesale,
                        onValueChange = { editWholesale = it },
                        label = { Text("سعر الجملة (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editSelling,
                        onValueChange = { editSelling = it },
                        label = { Text("سعر البيع (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newQty = editQuantity.toIntOrNull() ?: p.quantity
                        val newWholesale = editWholesale.toDoubleOrNull() ?: p.wholesalePrice
                        val newSelling = editSelling.toDoubleOrNull() ?: p.sellingPrice
                        if (editName.isNotBlank() && newQty >= 0 && newWholesale >= 0 && newSelling >= 0) {
                            viewModel.updateProduct(
                                p.copy(
                                    productName = editName.trim(),
                                    quantity = newQty,
                                    wholesalePrice = newWholesale,
                                    sellingPrice = newSelling
                                )
                            )
                            productToEdit = null
                        }
                    }
                ) {
                    Text("حفظ التعديلات")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToEdit = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Quick Add Stock Dialog
    productToAddStock?.let { p ->
        AlertDialog(
            onDismissRequest = { productToAddStock = null },
            title = { Text("إضافة كمية للمخزون") },
            text = {
                Column {
                    Text("المنتج: ${p.productName}")
                    Text("الكمية الحالية: ${p.quantity} قطعة")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addStockQtyText,
                        onValueChange = { addStockQtyText = it },
                        label = { Text("الكمية المضافة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val added = addStockQtyText.toIntOrNull() ?: 0
                        if (added > 0) {
                            viewModel.addStockToExisting(p.id, added)
                            productToAddStock = null
                        }
                    }
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToAddStock = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    productToDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("تأكيد حذف المنتج") },
            text = { Text("هل أنت متأكد من حذف المنتج '${p.productName}' من المخزن نهائياً؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmDeleteProduct(p)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    currencyFormat: NumberFormat,
    onInspect: () -> Unit,
    onEdit: () -> Unit,
    onAddStock: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.productName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "باركود: ${product.barcode}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Stock Badge
                if (product.quantity <= 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFFEBEE)
                    ) {
                        Text(
                            text = "نفد من المخزون",
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp
                        )
                    }
                } else if (product.quantity <= product.lowStockLimit) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFFF3E0)
                    ) {
                        Text(
                            text = "الكمية أوشكت على النفاد",
                            color = WarningOrange,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Text(
                            text = "متوفر: ${product.quantity} قطعة",
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pricing details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "سعر البيع: ${currencyFormat.format(product.sellingPrice)} ج.م",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    fontSize = 13.sp
                )
                Text(
                    text = "الجملة: ${currencyFormat.format(product.wholesalePrice)} ج.م",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "الربح: ${currencyFormat.format(product.unitProfit)} ج.م",
                    fontSize = 13.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onInspect, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "فحص تفصيلي", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onAddStock, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة كمية", tint = Color(0xFF2E7D32))
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = ErrorRed)
                }
            }
        }
    }
}
