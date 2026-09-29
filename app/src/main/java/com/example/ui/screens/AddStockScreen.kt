package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.MarketScaffold
import com.example.ui.scanner.BarcodeScannerDialog
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodel.MarketViewModel
import com.example.ui.viewmodel.Screen
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AddStockScreen(
    viewModel: MarketViewModel
) {
    val barcode by viewModel.addStockBarcode.collectAsStateWithLifecycle()
    val productName by viewModel.addStockProductName.collectAsStateWithLifecycle()
    val quantity by viewModel.addStockQuantity.collectAsStateWithLifecycle()
    val wholesalePrice by viewModel.addStockWholesalePrice.collectAsStateWithLifecycle()
    val sellingPrice by viewModel.addStockSellingPrice.collectAsStateWithLifecycle()
    val existingProduct by viewModel.existingProductFound.collectAsStateWithLifecycle()
    val successMessage by viewModel.addStockSuccessMessage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.addStockErrorMessage.collectAsStateWithLifecycle()

    var showScannerDialog by remember { mutableStateOf(false) }

    val wholesaleVal = wholesalePrice.toDoubleOrNull() ?: 0.0
    val sellingVal = sellingPrice.toDoubleOrNull() ?: 0.0
    val unitProfit = sellingVal - wholesaleVal

    val currencyFormat = remember {
        NumberFormat.getNumberInstance(Locale("ar", "EG")).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
    }

    MarketScaffold(
        title = "إضافة بضاعة للمخزون",
        onBack = {
            viewModel.resetAddStockForm()
            viewModel.navigateTo(Screen.HOME)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Scanner and Manual Barcode Header Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showScannerDialog = true },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp)
                        .testTag("add_stock_camera_scan_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("قراءة الباركود بالكاميرا", fontWeight = FontWeight.Bold)
                }
            }

            // Success Message
            AnimatedVisibility(visible = successMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = successMessage ?: "",
                            color = Color(0xFF1B5E20),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Error Message
            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Notice if existing product found
            if (existingProduct != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFF8E1),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFFF57F17))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "هذا المنتج مسجل بالفعل في المخزن!",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF57F17),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "الكمية المتوفرة حالياً: ${existingProduct!!.quantity} قطعة. سيتم إضافة الكمية الجديدة فوق المخزون الحالي.",
                                fontSize = 12.sp,
                                color = Color(0xFF5D4037)
                            )
                        }
                    }
                }
            }

            // Barcode Input Field
            OutlinedTextField(
                value = barcode,
                onValueChange = {
                    viewModel.addStockBarcode.value = it
                    viewModel.lookupBarcodeForStock(it)
                },
                label = { Text("الباركود *") },
                placeholder = { Text("امسح بالكاميرا أو اكتب الباركود") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_stock_barcode_input")
            )

            // Product Name Field
            OutlinedTextField(
                value = productName,
                onValueChange = { viewModel.addStockProductName.value = it },
                label = { Text("اسم المنتج *") },
                placeholder = { Text("مثال: جبنة لافاش كيري 8 قطع") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_stock_name_input")
            )

            // Quantity Field
            OutlinedTextField(
                value = quantity,
                onValueChange = { viewModel.addStockQuantity.value = it },
                label = { Text(if (existingProduct != null) "الكمية المضافة للمخزون *" else "الكمية الأولية *") },
                placeholder = { Text("1") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_stock_qty_input")
            )

            // Wholesale Price & Selling Price (Row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = wholesalePrice,
                    onValueChange = { viewModel.addStockWholesalePrice.value = it },
                    label = { Text("سعر الجملة (ج.م) *") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_stock_wholesale_input")
                )

                OutlinedTextField(
                    value = sellingPrice,
                    onValueChange = { viewModel.addStockSellingPrice.value = it },
                    label = { Text("سعر البيع (ج.م) *") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_stock_selling_input")
                )
            }

            // Live Unit Profit Calculation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "حساب الأرباح التقديرية",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ربح القطعة الواحدة:")
                        Text(
                            text = "${currencyFormat.format(unitProfit)} ج.م",
                            fontWeight = FontWeight.Bold,
                            color = if (unitProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }

                    val totalStockProfit = unitProfit * (quantity.toIntOrNull() ?: 0)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("إجمالي ربح الكمية المدخلة:")
                        Text(
                            text = "${currencyFormat.format(totalStockProfit)} ج.م",
                            fontWeight = FontWeight.Bold,
                            color = if (totalStockProfit >= 0) EmeraldPrimary else Color(0xFFC62828)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Submit Button
            Button(
                onClick = { viewModel.saveStockEntry() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("add_stock_save_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (existingProduct != null) "حفظ وتحديث المخزون" else "حفظ المنتج في المخزن",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            OutlinedButton(
                onClick = { viewModel.resetAddStockForm() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_stock_reset_btn")
            ) {
                Text("تفريغ الحقول")
            }
        }
    }

    if (showScannerDialog) {
        BarcodeScannerDialog(
            title = "مسح باركود لإضافته للمخزن",
            onBarcodeScanned = { code ->
                viewModel.onAddStockBarcodeScanned(code)
            },
            onDismiss = { showScannerDialog = false }
        )
    }
}
