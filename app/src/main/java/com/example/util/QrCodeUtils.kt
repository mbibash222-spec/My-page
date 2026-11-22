package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.model.Product
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeUtils {

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) Color.parseColor("#1F1916") else Color.WHITE
                }
            }
            Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        } catch (_: Exception) {
            null
        }
    }

    fun extractLookupToken(rawPayload: String): String {
        val cleaned = rawPayload.trim()
        if (cleaned.isEmpty()) return ""
        val firstSegment = cleaned.substringBefore("|").trim()
        return when {
            firstSegment.startsWith("SANU:", ignoreCase = true) ->
                firstSegment.substringAfter(":").trim()
            firstSegment.contains("://") ->
                firstSegment.substringAfterLast("/").substringBefore("?").trim()
            else -> firstSegment
        }
    }

    fun findMatchingProduct(rawPayload: String, products: List<Product>): Product? {
        val cleaned = rawPayload.trim()
        if (cleaned.isEmpty()) return null
        val token = extractLookupToken(cleaned)
        return products.firstOrNull { product ->
            product.qrPayload.equals(cleaned, ignoreCase = true) ||
                product.qrPayload.equals("SANU:$token", ignoreCase = true) ||
                product.sku.equals(token, ignoreCase = true) ||
                product.id.equals(token, ignoreCase = true) ||
                product.warehouseBin.equals(cleaned, ignoreCase = true) ||
                product.warehouseBin.equals(token, ignoreCase = true)
        }
    }
}
