package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.ui.components.QrScannerView
import com.example.ui.components.QrTagDialog
import com.example.ui.components.StockStatusPill
import com.example.ui.components.resolveProductDrawable
import com.example.util.PaymentUtils

@Composable
fun QrLookupScreen(
    products: List<Product>,
    scannedProduct: Product?,
    recentScannedProducts: List<Product>,
    scanMessage: String?,
    isAgentMode: Boolean,
    onLookupQr: (String) -> Unit,
    onClearScan: () -> Unit,
    onAddToCart: (Product, String) -> Unit,
    onAdjustStock: (Product, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var manualSku by remember { mutableStateOf("") }
    var qrTagProduct by remember { mutableStateOf<Product?>(null) }
    var isScannerPaused by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ScannerAndManualPane(
                        products = products,
                        recentScannedProducts = recentScannedProducts,
                        manualSku = manualSku,
                        isAgentMode = isAgentMode,
                        isScannerPaused = isScannerPaused,
                        onTogglePause = { isScannerPaused = !isScannerPaused },
                        onManualSkuChange = { manualSku = it },
                        onQrDetected = { raw ->
                            manualSku = raw
                            isScannerPaused = true
                            onLookupQr(raw)
                        },
                        onLookupQr = onLookupQr
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LookupResultPane(
                        scannedProduct = scannedProduct,
                        scanMessage = scanMessage,
                        isAgentMode = isAgentMode,
                        isScannerPaused = isScannerPaused,
                        onScanNext = {
                            isScannerPaused = false
                            manualSku = ""
                            onClearScan()
                        },
                        onAddToCart = onAddToCart,
                        onAdjustStock = onAdjustStock,
                        onViewQrTag = { qrTagProduct = it }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ScannerAndManualPane(
                    products = products,
                    recentScannedProducts = recentScannedProducts,
                    manualSku = manualSku,
                    isAgentMode = isAgentMode,
                    isScannerPaused = isScannerPaused,
                    onTogglePause = { isScannerPaused = !isScannerPaused },
                    onManualSkuChange = { manualSku = it },
                    onQrDetected = { raw ->
                        manualSku = raw
                        isScannerPaused = true
                        onLookupQr(raw)
                    },
                    onLookupQr = onLookupQr
                )
                LookupResultPane(
                    scannedProduct = scannedProduct,
                    scanMessage = scanMessage,
                    isAgentMode = isAgentMode,
                    isScannerPaused = isScannerPaused,
                    onScanNext = {
                        isScannerPaused = false
                        manualSku = ""
                        onClearScan()
                    },
                    onAddToCart = onAddToCart,
                    onAdjustStock = onAdjustStock,
                    onViewQrTag = { qrTagProduct = it }
                )
            }
        }
    }

    qrTagProduct?.let { prod ->
        QrTagDialog(
            product = prod,
            onDismiss = { qrTagProduct = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScannerAndManualPane(
    products: List<Product>,
    recentScannedProducts: List<Product>,
    manualSku: String,
    isAgentMode: Boolean,
    isScannerPaused: Boolean,
    onTogglePause: () -> Unit,
    onManualSkuChange: (String) -> Unit,
    onQrDetected: (String) -> Unit,
    onLookupQr: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("QR Product & Warehouse Lookup", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "CameraX + ML Kit scanner for shoe boxes & bin labels",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isAgentMode) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Agent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        QrScannerView(
            onQrDetected = onQrDetected,
            isPaused = isScannerPaused,
            onTogglePause = onTogglePause
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualSku,
                onValueChange = onManualSkuChange,
                label = { Text("Enter SKU, Bin, or QR Payload") },
                placeholder = { Text("e.g. SNU-SH-001 or SANU:SNU-SL-001") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("qr_manual_sku_input")
            )
            Button(
                onClick = { onLookupQr(manualSku) },
                modifier = Modifier
                    .height(54.dp)
                    .testTag("qr_lookup_button")
            ) {
                Icon(Icons.Filled.Search, contentDescription = "Lookup SKU")
            }
        }

        if (recentScannedProducts.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Recent Scans",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                recentScannedProducts.forEach { prod ->
                    AssistChip(
                        onClick = {
                            onManualSkuChange(prod.qrPayload)
                            onLookupQr(prod.qrPayload)
                        },
                        label = { Text("${prod.sku} · ${prod.warehouseBin}") }
                    )
                }
            }
        }

        if (products.isNotEmpty()) {
            Text(
                text = "Quick Sample QR Tags",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                products.take(4).forEach { prod ->
                    AssistChip(
                        onClick = {
                            onManualSkuChange(prod.qrPayload)
                            onLookupQr(prod.qrPayload)
                        },
                        label = { Text("${prod.sku} (${prod.categoryEnum.label})") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.QrCode2,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LookupResultPane(
    scannedProduct: Product?,
    scanMessage: String?,
    isAgentMode: Boolean,
    isScannerPaused: Boolean,
    onScanNext: () -> Unit,
    onAddToCart: (Product, String) -> Unit,
    onAdjustStock: (Product, Long) -> Unit,
    onViewQrTag: (Product) -> Unit
) {
    if (scanMessage != null) {
        Surface(
            color = if (scannedProduct != null) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = scanMessage,
                    color = if (scannedProduct != null) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                if (isScannerPaused || scannedProduct != null) {
                    TextButton(
                        onClick = onScanNext,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("qr_scan_next_button")
                    ) {
                        Text("Scan Next")
                    }
                }
            }
        }
    }

    if (scannedProduct == null) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Text("Ready for QR Lookup", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Point the camera at a Sanu Shop QR label or warehouse barcode to view bin location, sizes, live stock, and agent margin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("qr_scanned_product_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(
                            id = resolveProductDrawable(scannedProduct.image, scannedProduct.categoryEnum)
                        ),
                        contentDescription = scannedProduct.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${scannedProduct.brand} · ${scannedProduct.sku}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = scannedProduct.name,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${scannedProduct.categoryEnum.label} · ${scannedProduct.colorway}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        StockStatusPill(product = scannedProduct)
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LookupRow("Warehouse Location", scannedProduct.warehouseBin)
                        LookupRow("QR Code Payload", scannedProduct.qrPayload)
                        LookupRow("Retail Price", PaymentUtils.formatCurrency(scannedProduct.price))
                        if (isAgentMode) {
                            val margin = (scannedProduct.price - scannedProduct.costPrice).coerceAtLeast(0.0)
                            val marginPct = if (scannedProduct.price > 0) ((margin / scannedProduct.price) * 100).toInt() else 0
                            LookupRow(
                                "Unit Cost / Margin",
                                "${PaymentUtils.formatCurrency(scannedProduct.costPrice)} (+${PaymentUtils.formatCurrency(margin)} · $marginPct%)"
                            )
                            LookupRow("Units Sold", "${scannedProduct.soldCount} pairs")
                        }
                        LookupRow("Available Sizes", scannedProduct.size)
                    }
                }

                if (isAgentMode) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Agent Warehouse Stock Control",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onAdjustStock(scannedProduct, -1L) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(" 1")
                                }
                                OutlinedButton(
                                    onClick = { onAdjustStock(scannedProduct, 1L) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(" 1")
                                }
                                Button(
                                    onClick = { onAdjustStock(scannedProduct, 10L) },
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Text("+10 Restock")
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onViewQrTag(scannedProduct) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("QR Tag")
                    }
                    Button(
                        onClick = {
                            onAddToCart(scannedProduct, scannedProduct.sizes.firstOrNull() ?: "US 9")
                        },
                        enabled = scannedProduct.stockCount > 0L,
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Filled.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to Bag")
                    }
                }
            }
        }
    }
}

@Composable
private fun LookupRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}
