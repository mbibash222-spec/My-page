package com.example

import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.util.QrCodeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrCodeUtilsTest {

    private val catalog = listOf(
        Product(
            id = "sku_sh_001",
            sku = "SNU-SH-001",
            name = "Aether Court Sneaker",
            category = ProductCategory.SHOES.wireValue,
            price = 128.0,
            warehouseBin = "AISLE-A · BIN-04",
            qrPayload = "SANU:SNU-SH-001"
        ),
        Product(
            id = "sku_sl_001",
            sku = "SNU-SL-001",
            name = "Dune Suede Cork Slide",
            category = ProductCategory.SLIPPERS.wireValue,
            price = 64.0,
            warehouseBin = "AISLE-C · BIN-01",
            qrPayload = "SANU:SNU-SL-001"
        )
    )

    @Test
    fun findMatchingProduct_resolvesQrPayloadSkuUrlBinAndId() {
        assertEquals("sku_sh_001", QrCodeUtils.findMatchingProduct("SANU:SNU-SH-001", catalog)?.id)
        assertEquals("sku_sh_001", QrCodeUtils.findMatchingProduct("snu-sh-001", catalog)?.id)
        assertEquals("sku_sl_001", QrCodeUtils.findMatchingProduct("https://sanushop.com/sku/SNU-SL-001", catalog)?.id)
        assertEquals("sku_sl_001", QrCodeUtils.findMatchingProduct("AISLE-C · BIN-01", catalog)?.id)
        assertEquals("sku_sh_001", QrCodeUtils.findMatchingProduct("SANU:SNU-SH-001|BIN:AISLE-A", catalog)?.id)
    }

    @Test
    fun findMatchingProduct_returnsNullForUnknownOrBlankCode() {
        assertNull(QrCodeUtils.findMatchingProduct("", catalog))
        assertNull(QrCodeUtils.findMatchingProduct("UNKNOWN-999", catalog))
    }
}
