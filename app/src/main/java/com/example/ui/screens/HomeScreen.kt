package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.MarketScaffold
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.MarketViewModel
import com.example.ui.viewmodel.Screen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: MarketViewModel
) {
    val todaySales by viewModel.todaySales.collectAsStateWithLifecycle()
    val todayProfit by viewModel.todayProfit.collectAsStateWithLifecycle()
    val todayInvoicesCount by viewModel.todayInvoicesCount.collectAsStateWithLifecycle()
    val lowStockCount by viewModel.lowStockCount.collectAsStateWithLifecycle()

    val currencyFormat = NumberFormat.getNumberInstance(Locale("ar", "EG")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    MarketScaffold(
        title = "ماركت أولاد عاشور"
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = EmeraldPrimary,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(EmeraldDark, EmeraldPrimary, EmeraldLight)
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "مرحباً بك في نظام كاشير",
                                color = GoldLight,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "ماركت أولاد عاشور",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(GoldAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = GoldLight,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Today's Stats Cards (2x2 Grid)
            Text(
                text = "إحصائيات اليوم",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "مبيعات اليوم",
                    value = "${currencyFormat.format(todaySales)} ج.م",
                    icon = Icons.Default.PointOfSale,
                    cardColor = Color(0xFF1B5E20),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_today_sales")
                )
                StatCard(
                    title = "أرباح اليوم",
                    value = "${currencyFormat.format(todayProfit)} ج.م",
                    icon = Icons.Default.Assessment,
                    cardColor = Color(0xFF00695C),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_today_profit")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "عدد فواتير اليوم",
                    value = "$todayInvoicesCount فاتورة",
                    icon = Icons.Default.ReceiptLong,
                    cardColor = Color(0xFF0D47A1),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_today_invoices")
                )
                StatCard(
                    title = "قليلة المخزون",
                    value = "$lowStockCount منتج",
                    icon = Icons.Default.Warning,
                    cardColor = if (lowStockCount > 0) WarningOrange else Color(0xFF546E7A),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_low_stock")
                )
            }

            // Quick Actions Title
            Text(
                text = "الخدمات والعمليات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Main 6 Action Buttons in Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    ActionTile(
                        title = "البيع (الكاشير)",
                        subtitle = "تسجيل فواتير جديدة",
                        icon = Icons.Default.PointOfSale,
                        highlight = true,
                        testTag = "home_pos_btn",
                        onClick = { viewModel.navigateTo(Screen.POS) }
                    )
                }
                item {
                    ActionTile(
                        title = "إضافة للمخزون",
                        subtitle = "استلام بضاعة جديدة",
                        icon = Icons.Default.AddBusiness,
                        testTag = "home_add_stock_btn",
                        onClick = { viewModel.navigateTo(Screen.ADD_STOCK) }
                    )
                }
                item {
                    ActionTile(
                        title = "المخزن",
                        subtitle = "قائمة وجرد المنتجات",
                        icon = Icons.Default.Inventory,
                        testTag = "home_inventory_btn",
                        onClick = { viewModel.navigateTo(Screen.INVENTORY) }
                    )
                }
                item {
                    ActionTile(
                        title = "كشف الحسابات",
                        subtitle = "الأرباح والمبيعات",
                        icon = Icons.Default.Assessment,
                        testTag = "home_reports_btn",
                        onClick = { viewModel.navigateTo(Screen.REPORTS) }
                    )
                }
                item {
                    ActionTile(
                        title = "الفواتير",
                        subtitle = "سجل عمليات البيع",
                        icon = Icons.Default.ReceiptLong,
                        testTag = "home_invoices_btn",
                        onClick = { viewModel.navigateTo(Screen.INVOICES) }
                    )
                }
                item {
                    ActionTile(
                        title = "الإعدادات",
                        subtitle = "النسخ الاحتياطي والبيانات",
                        icon = Icons.Default.Settings,
                        testTag = "home_settings_btn",
                        onClick = { viewModel.navigateTo(Screen.SETTINGS) }
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    cardColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(20.dp)
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

@Composable
fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    highlight: Boolean = false,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlight) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (highlight) GoldAccent else EmeraldPrimary.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (highlight) EmeraldDark else EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (highlight) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = if (highlight) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
