package com.example

import com.example.data.model.Product
import com.example.data.repository.CartItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testProductProfitAndValuation() {
        val product = Product(
            id = 1,
            barcode = "622300000001",
            productName = "شاي العروسة 250 جم",
            quantity = 20,
            wholesalePrice = 50.0,
            sellingPrice = 65.0,
            lowStockLimit = 5
        )

        // Profit per unit = selling - wholesale
        assertEquals(15.0, product.unitProfit, 0.001)

        // Total inventory wholesale value
        assertEquals(1000.0, product.totalWholesaleValue, 0.001)

        // Total inventory selling value
        assertEquals(1300.0, product.totalSellingValue, 0.001)

        // Low stock checks
        assertTrue(!product.isLowStock)
        assertTrue(!product.isOutOfStock)
    }

    @Test
    fun testCartItemProfitCalculation() {
        val product = Product(
            id = 2,
            barcode = "622300000002",
            productName = "سكر أبيض 1 كجم",
            quantity = 15,
            wholesalePrice = 30.0,
            sellingPrice = 35.0
        )

        val cartItem = CartItem(product = product, quantity = 4)

        assertEquals(140.0, cartItem.totalAmount, 0.001)
        assertEquals(120.0, cartItem.totalCost, 0.001)
        assertEquals(20.0, cartItem.profit, 0.001)
    }

    @Test
    fun testLowStockLimitDetection() {
        val lowStockProduct = Product(
            id = 3,
            barcode = "622300000003",
            productName = "زيت ذرة 800 مل",
            quantity = 3,
            wholesalePrice = 80.0,
            sellingPrice = 95.0,
            lowStockLimit = 5
        )

        assertTrue(lowStockProduct.isLowStock)
        assertTrue(!lowStockProduct.isOutOfStock)

        val outOfStockProduct = lowStockProduct.copy(quantity = 0)
        assertTrue(outOfStockProduct.isOutOfStock)
    }
}
