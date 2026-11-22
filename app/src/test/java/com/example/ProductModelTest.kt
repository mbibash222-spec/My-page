package com.example

import com.example.data.model.Product
import com.example.data.model.ProductCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductModelTest {

    @Test
    fun convenienceConstructor_populatesNamePriceSizeAndImage() {
        val product = Product(
            name = "Breeze Cork Slide",
            price = 64.0,
            size = "US 7, US 8, US 9",
            image = "slipper",
            category = ProductCategory.SLIPPERS,
            stockCount = 18L
        )

        assertEquals("Breeze Cork Slide", product.name)
        assertEquals(64.0, product.price, 0.001)
        assertEquals("US 7, US 8, US 9", product.size)
        assertEquals(listOf("US 7", "US 8", "US 9"), product.sizes)
        assertEquals("slipper", product.image)
        assertEquals("slipper", product.imageKey)
        assertEquals(ProductCategory.SLIPPERS, product.categoryEnum)
    }

    @Test
    fun createAndUpdateMaps_matchFirestoreSchemaWithoutIdField() {
        val product = Product(
            id = "doc_123",
            userId = "alice_123",
            sku = "snu-sh-101",
            name = "Aether Runner",
            brand = "Sanu Motion",
            category = ProductCategory.SHOES.wireValue,
            price = 128.0,
            sizes = listOf("US 8", "US 9"),
            imageKey = "sneaker"
        )

        val createMap = product.toCreateMap()
        assertFalse(createMap.containsKey("id"))
        assertEquals("alice_123", createMap["userId"])
        assertEquals("SNU-SH-101", createMap["sku"])
        assertEquals("Aether Runner", createMap["name"])
        assertEquals(128.0, createMap["price"] as Double, 0.001)
        assertEquals(listOf("US 8", "US 9"), createMap["sizes"])
        assertEquals("sneaker", createMap["imageKey"])
        assertTrue(createMap.containsKey("createdAt"))
        assertTrue(createMap.containsKey("updatedAt"))

        val updateMap = product.toUpdateMap()
        assertFalse(updateMap.containsKey("id"))
        assertFalse(updateMap.containsKey("userId"))
        assertFalse(updateMap.containsKey("createdAt"))
        assertTrue(updateMap.containsKey("updatedAt"))
    }
}
