package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.MarketScaffold
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodel.MarketViewModel
import com.example.ui.viewmodel.ReportPeriod
import com.example.ui.viewmodel.Screen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MarketViewModel
) {
    val period by viewModel.reportPeriod.collectAsStateWithLifecycle()
    val summary by viewModel.financialSummary.collectAsStateWithLifecycle()
    val customStart by viewModel.customStartDate.collectAsStateWithLifecycle()
    val customEnd by viewModel.customEndDate.collectAsStateWithLifecycle()

    var showDateRangePicker by remember { mutableStateOf(false) }

    val currencyFormat = remember {
        NumberFormat.getNumberInstance(Locale("ar", "EG")).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
    }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale("ar", "EG")) }

    MarketScaffold(
        title = "كشف الحسابات والأرباح",
        onBack = { viewModel.navigateTo(Screen.HOME) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period Selection Chips
            Text(
                text = "الفترة الزمنية",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = period == ReportPeriod.TODAY,
                    onClick = { viewModel.loadReport(ReportPeriod.TODAY) },
                    label = { Text("اليوم") },
                    modifier = Modifier.testTag("report_period_today")
                )
                FilterChip(
                    selected = period == ReportPeriod.THIS_WEEK,
                    onClick = { viewModel.loadReport(ReportPeriod.THIS_WEEK) },
                    label = { Text("هذا الأسبوع") },
                    modifier = Modifier.testTag("report_period_week")
                )
                FilterChip(
                    selected = period == ReportPeriod.THIS_MONTH,
                    onClick = { viewModel.loadReport(ReportPeriod.THIS_MONTH) },
                    label = { Text("هذا الشهر") },
                    modifier = Modifier.testTag("report_period_month")
                )
                FilterChip(
                    selected = period == ReportPeriod.CUSTOM,
                    onClick = { showDateRangePicker = true },
                    label = { Text("فترة مخصصة") },
                    modifier = Modifier.testTag("report_period_custom")
                )
            }

            if (period == ReportPeriod.CUSTOM) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "من: ${dateFormat.format(Date(customStart))} إلى: ${dateFormat.format(Date(customEnd))}",
                            fontSize = 12.sp
                        )
                        OutlinedButton(onClick = { showDateRangePicker = true }) {
                            Text("تغيير التواريخ", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Key Metrics Cards
            Text(
                text = "مؤشرات الأداء المالي",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Primary Big Profit Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "صافي الأرباح المحققة",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                        Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = GoldAccent)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${currencyFormat.format(summary.totalProfit)} ج.م",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("report_total_profit")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "محسوبة بدقة بناءً على أسعار البيع والجملة المسجلة وقت الفواتير",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            // Sales and Cost Rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FinancialMetricCard(
                    title = "إجمالي المبيعات",
                    value = "${currencyFormat.format(summary.totalSales)} ج.م",
                    icon = Icons.Default.Paid,
                    containerColor = Color(0xFF1E88E5),
                    modifier = Modifier.weight(1f)
                )
                FinancialMetricCard(
                    title = "تكلفة البضاعة المباعة",
                    value = "${currencyFormat.format(summary.totalCost)} ج.م",
                    icon = Icons.Default.MoneyOff,
                    containerColor = Color(0xFF5E35B1),
                    modifier = Modifier.weight(1f)
                )
            }

            // Counts Rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FinancialMetricCard(
                    title = "عدد الفواتير",
                    value = "${summary.invoiceCount} فاتورة",
                    icon = Icons.Default.Receipt,
                    containerColor = Color(0xFF00897B),
                    modifier = Modifier.weight(1f)
                )
                FinancialMetricCard(
                    title = "القطع المباعة",
                    value = "${summary.totalItemsSold} قطعة",
                    icon = Icons.Default.ShoppingBag,
                    containerColor = Color(0xFFFB8C00),
                    modifier = Modifier.weight(1f)
                )
            }

            FinancialMetricCard(
                title = "متوسط قيمة الفاتورة",
                value = "${currencyFormat.format(summary.averageInvoiceValue)} ج.م",
                icon = Icons.Default.Assessment,
                containerColor = Color(0xFF455A64),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showDateRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = customStart,
            initialSelectedEndDateMillis = customEnd
        )

        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                Button(onClick = {
                    val s = dateRangePickerState.selectedStartDateMillis ?: customStart
                    val e = dateRangePickerState.selectedEndDateMillis ?: customEnd
                    viewModel.setCustomReportDates(s, e)
                    showDateRangePicker = false
                }) {
                    Text("تطبيق")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("إلغاء")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text(text = "اختر الفترة الزمنية", modifier = Modifier.padding(16.dp)) }
            )
        }
    }
}

@Composable
fun FinancialMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
