package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sku: String = "",
    val name: String = "",
    val brand: String = "",
    val category: String = ProductCategory.SHOES.wireValue,
    val subcategory: String = "Sneakers",
    val description: String = "",
    val price: Double = 0.0,
    val costPrice: Double = 0.0,
    val stockCount: Long = 0L,
    val lowStockThreshold: Long = 6L,
    val warehouseBin: String = "",
    val qrPayload: String = "",
    val imageKey: String = "sneaker",
    val sizes: List<String> = listOf("US 8", "US 9", "US 10"),
    val colorway: String = "",
    val rating: Double = 4.8,
    val soldCount: Long = 0L,
    val featured: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    constructor(
        name: String,
        price: Double,
        size: String,
        image: String,
        category: ProductCategory = ProductCategory.SHOES,
        stockCount: Long = 10L
    ) : this(
        name = name,
        price = price,
        category = category.wireValue,
        imageKey = image,
        sizes = parseSizes(size),
        stockCount = stockCount
    )

    @get:Exclude
    val size: String
        get() = sizes.joinToString(", ")

    @get:Exclude
    val image: String
        get() = imageKey

    @get:Exclude
    val categoryEnum: ProductCategory
        get() = ProductCategory.fromWire(category)

    @get:Exclude
    val isLowStock: Boolean
        get() = stockCount <= lowStockThreshold

    companion object {
        fun parseSizes(raw: String): List<String> =
            raw.split(",", "·", ";")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(15)
                .ifEmpty { listOf("US 9") }
    }

    fun normalized(ownerUid: String = userId): Product {
        val cleanSku = sku.trim().uppercase(Locale.US).take(32)
            .let { if (it.length < 2) "SNU-CUSTOM" else it }
        val cleanName = name.trim().take(80)
        val cleanBrand = brand.trim().take(40)
            .let { if (it.length < 2) "Sanu Atelier" else it }
        val resolvedCategory = ProductCategory.fromWire(category)
        val defaultSubcat = if (resolvedCategory == ProductCategory.SHOES) "Sneakers" else "Slides"
        val cleanSubcat = subcategory.trim().take(40)
            .let { if (it.length < 2) defaultSubcat else it }
        val cleanBin = warehouseBin.trim().take(40)
            .let { if (it.length < 2) "AISLE-C · BIN-01" else it }
        val cleanQr = qrPayload.trim().take(120)
            .let { if (it.length < 4) "SANU:$cleanSku" else it }
        val defaultImage = if (resolvedCategory == ProductCategory.SHOES) "sneaker" else "slipper"
        val cleanImage = imageKey.trim().take(40).ifBlank { defaultImage }
        val cleanSizes = sizes.mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            .take(15)
            .ifEmpty { listOf("US 9") }
        val cleanColorway = colorway.trim().take(40).ifBlank { "Warm Sand" }

        return copy(
            userId = ownerUid,
            sku = cleanSku,
            name = cleanName,
            brand = cleanBrand,
            category = resolvedCategory.wireValue,
            subcategory = cleanSubcat,
            description = description.trim().take(400),
            price = price.coerceIn(0.01, 10000.0),
            costPrice = costPrice.coerceIn(0.0, 10000.0),
            stockCount = stockCount.coerceIn(0L, 100000L),
            lowStockThreshold = lowStockThreshold.coerceIn(1L, 1000L),
            warehouseBin = cleanBin,
            qrPayload = cleanQr,
            imageKey = cleanImage,
            sizes = cleanSizes,
            colorway = cleanColorway,
            rating = rating.coerceIn(0.0, 5.0),
            soldCount = soldCount.coerceIn(0L, 1000000L)
        )
    }

    fun toCreateMap(): Map<String, Any> {
        val clean = normalized()
        return mapOf(
            "userId" to clean.userId,
            "sku" to clean.sku,
            "name" to clean.name,
            "brand" to clean.brand,
            "category" to clean.category,
            "subcategory" to clean.subcategory,
            "description" to clean.description,
            "price" to clean.price,
            "costPrice" to clean.costPrice,
            "stockCount" to clean.stockCount,
            "lowStockThreshold" to clean.lowStockThreshold,
            "warehouseBin" to clean.warehouseBin,
            "qrPayload" to clean.qrPayload,
            "imageKey" to clean.imageKey,
            "sizes" to clean.sizes,
            "colorway" to clean.colorway,
            "rating" to clean.rating,
            "soldCount" to clean.soldCount,
            "featured" to clean.featured,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun toUpdateMap(): Map<String, Any> {
        val clean = normalized()
        return mapOf(
            "sku" to clean.sku,
            "name" to clean.name,
            "brand" to clean.brand,
            "category" to clean.category,
            "subcategory" to clean.subcategory,
            "description" to clean.description,
            "price" to clean.price,
            "costPrice" to clean.costPrice,
            "stockCount" to clean.stockCount,
            "lowStockThreshold" to clean.lowStockThreshold,
            "warehouseBin" to clean.warehouseBin,
            "qrPayload" to clean.qrPayload,
            "imageKey" to clean.imageKey,
            "sizes" to clean.sizes,
            "colorway" to clean.colorway,
            "rating" to clean.rating,
            "soldCount" to clean.soldCount,
            "featured" to clean.featured,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

typealias FootwearProduct = Product
