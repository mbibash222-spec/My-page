package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AnalyticsSummary
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ShopOrder
import com.example.data.model.ShopUserProfile
import com.example.data.model.TrackingStatus
import com.example.data.model.UiState
import com.example.ui.auth.AgentAuthConfig
import com.example.ui.auth.AgentCredentialsForm
import com.example.ui.components.CategorySplitDonutChart
import com.example.ui.components.QrTagDialog
import com.example.ui.components.RevenueComparisonBarChart
import com.example.ui.components.StockStatusPill
import com.example.util.PaymentUtils

@Composable
fun ManagerDashboardScreen(
    profile: ShopUserProfile?,
    analytics: AnalyticsSummary,
    productsState: UiState<List<Product>>,
    ordersState: UiState<List<ShopOrder>>,
    onAgentSignIn: (badgeId: String, pin: String, branch: String) -> Boolean,
    onAgentSignOut: () -> Unit,
    onAdjustStock: (Product, Long) -> Unit,
    onSaveProduct: (
        existing: Product?,
        sku: String,
        name: String,
        brand: String,
        category: ProductCategory,
        subcategory: String,
        description: String,
        price: Double,
        costPrice: Double,
        stockCount: Long,
        warehouseBin: String,
        colorway: String,
        imageKey: String,
        sizes: List<String>
    ) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onAdvanceOrder: (ShopOrder) -> Unit,
    onOpenQrScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (profile?.agentMode != true) {
        AgentLoginGate(
            onAgentSignIn = onAgentSignIn,
            modifier = modifier
        )
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var productForQrTag by remember { mutableStateOf<Product?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var productPendingDelete by remember { mutableStateOf<Product?>(null) }

    val products = (productsState as? UiState.Success)?.data ?: emptyList()
    val orders = (ordersState as? UiState.Success)?.data ?: emptyList()

    Column(modifier = modifier.fillMaxSize()) {
        // Active Agent Header Bar
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Agent ${profile.agentBadgeId} · Manager Portal",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = profile.storeBranch,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(
                    onClick = onAgentSignOut,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("agent_sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Exit Agent", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Analytics") },
                icon = { Icon(Icons.Filled.Analytics, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Inventory") },
                icon = { Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Dispatch") },
                icon = { Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        when (selectedTab) {
            0 -> AnalyticsTabContent(analytics = analytics)
            1 -> InventoryTabContent(
                products = products,
                onAddProduct = {
                    editingProduct = null
                    showEditDialog = true
                },
                onEditProduct = {
                    editingProduct = it
                    showEditDialog = true
                },
                onDeleteProduct = {
                    productPendingDelete = it
                },
                onAdjustStock = onAdjustStock,
                onShowQrTag = { productForQrTag = it },
                onOpenQrScanner = onOpenQrScanner
            )
            2 -> DispatchTabContent(
                orders = orders,
                onAdvanceOrder = onAdvanceOrder
            )
        }
    }

    productForQrTag?.let { prod ->
        QrTagDialog(product = prod, onDismiss = { productForQrTag = null })
    }

    productPendingDelete?.let { prod ->
        AlertDialog(
            onDismissRequest = { productPendingDelete = null },
            title = { Text("Delete ${prod.sku}?") },
            text = {
                Text(
                    "Remove \"${prod.name}\" from warehouse inventory? This permanently deletes the product record from Firestore."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(prod)
                        productPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_sku_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { productPendingDelete = null },
                    modifier = Modifier.testTag("cancel_delete_sku_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditDialog) {
        EditProductDialog(
            existing = editingProduct,
            onDismiss = { showEditDialog = false },
            onSave = { sku, name, brand, cat, sub, desc, price, cost, stock, bin, color, img, sizes ->
                onSaveProduct(
                    editingProduct,
                    sku,
                    name,
                    brand,
                    cat,
                    sub,
                    desc,
                    price,
                    cost,
                    stock,
                    bin,
                    color,
                    img,
                    sizes
                )
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun AgentLoginGate(
    onAgentSignIn: (badgeId: String, pin: String, branch: String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var badgeId by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf(AgentAuthConfig.STORE_BRANCHES.first()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Badge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Agent & Shop Manager Login", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Unlock inventory control, dispatch tracking & sales analytics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AgentCredentialsForm(
                    badgeId = badgeId,
                    onBadgeIdChange = {
                        badgeId = it
                        validationError = null
                    },
                    pin = pin,
                    onPinChange = {
                        pin = it
                        validationError = null
                    },
                    branch = branch,
                    onBranchChange = { branch = it },
                    validationError = validationError
                )

                Button(
                    onClick = {
                        val err = AgentAuthConfig.validateAgentCredentials(badgeId, pin)
                        if (err != null) {
                            validationError = err
                            return@Button
                        }
                        onAgentSignIn(badgeId, pin, branch)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("agent_login_submit_button")
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock Manager Portal")
                }
            }
        }
    }
}

@Composable
private fun AnalyticsTabContent(analytics: AnalyticsSummary) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 700.dp
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                val marginPct = if (analytics.totalRevenue > 0) {
                    ((analytics.totalProfit / analytics.totalRevenue) * 100).toInt()
                } else 0

                if (isWide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KpiCard("Gross Revenue", PaymentUtils.formatCurrency(analytics.totalRevenue), "${analytics.totalOrders} orders", Modifier.weight(1f))
                        KpiCard("Net Profit", PaymentUtils.formatCurrency(analytics.totalProfit), "$marginPct% margin", Modifier.weight(1f))
                        KpiCard("Avg Order", PaymentUtils.formatCurrency(analytics.averageOrderValue), "${analytics.totalUnitsSold} units sold", Modifier.weight(1f))
                        KpiCard("Warehouse Stock", "${analytics.totalInventoryUnits} prs", "${analytics.lowStockCount} low-stock", Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            KpiCard("Gross Revenue", PaymentUtils.formatCurrency(analytics.totalRevenue), "${analytics.totalOrders} orders", Modifier.weight(1f))
                            KpiCard("Net Profit", PaymentUtils.formatCurrency(analytics.totalProfit), "$marginPct% margin", Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            KpiCard("Avg Order", PaymentUtils.formatCurrency(analytics.averageOrderValue), "${analytics.totalUnitsSold} units sold", Modifier.weight(1f))
                            KpiCard("Warehouse Stock", "${analytics.totalInventoryUnits} prs", "${analytics.lowStockCount} low-stock", Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RevenueComparisonBarChart(
                        points = analytics.dailySeries,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Category Revenue Split", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        CategorySplitDonutChart(
                            shoesRevenue = analytics.shoesRevenue,
                            slippersRevenue = analytics.slippersRevenue
                        )
                    }
                }
            }

            item {
                Text("Top Performing Shoes & Slippers", style = MaterialTheme.typography.titleMedium)
            }

            items(analytics.topProducts, key = { it.id }) { prod ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prod.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${prod.sku} · ${prod.categoryEnum.label} · ${prod.soldCount} pairs sold",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = PaymentUtils.formatCurrency(prod.soldCount * prod.price),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InventoryTabContent(
    products: List<Product>,
    onAddProduct: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onAdjustStock: (Product, Long) -> Unit,
    onShowQrTag: (Product) -> Unit,
    onOpenQrScanner: () -> Unit
) {
    var filterMode by remember { mutableStateOf("ALL") }
    val filtered = remember(products, filterMode) {
        when (filterMode) {
            "LOW" -> products.filter { it.isLowStock }
            "SHOES" -> products.filter { it.categoryEnum == ProductCategory.SHOES }
            "SLIPPERS" -> products.filter { it.categoryEnum == ProductCategory.SLIPPERS }
            else -> products
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Warehouse Inventory (${products.size})", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenQrScanner,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.testTag("manager_scan_qr_button")
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan")
                    }
                    Button(
                        onClick = onAddProduct,
                        modifier = Modifier.testTag("add_inventory_sku_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add SKU")
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL" to "All", "LOW" to "Low Stock", "SHOES" to "Shoes", "SLIPPERS" to "Slippers").forEach { (key, label) ->
                    FilterChip(
                        selected = filterMode == key,
                        onClick = { filterMode = key },
                        label = { Text(label) }
                    )
                }
            }
        }

        items(filtered, key = { it.id }) { prod ->
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${prod.sku} · ${prod.warehouseBin}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(prod.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Retail ${PaymentUtils.formatCurrency(prod.price)} · Cost ${PaymentUtils.formatCurrency(prod.costPrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StockStatusPill(product = prod)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = { onShowQrTag(prod) },
                                modifier = Modifier.testTag("qr_sku_${prod.sku}")
                            ) {
                                Icon(Icons.Filled.QrCode2, contentDescription = "QR tag for ${prod.sku}")
                            }
                            IconButton(
                                onClick = { onEditProduct(prod) },
                                modifier = Modifier.testTag("edit_sku_${prod.sku}")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit ${prod.sku}")
                            }
                            IconButton(
                                onClick = { onDeleteProduct(prod) },
                                modifier = Modifier.testTag("delete_sku_${prod.sku}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Delete ${prod.sku}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onAdjustStock(prod, -1L) },
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease stock", modifier = Modifier.size(16.dp))
                            }
                            OutlinedButton(
                                onClick = { onAdjustStock(prod, 1L) },
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase stock", modifier = Modifier.size(16.dp))
                            }
                            Button(
                                onClick = { onAdjustStock(prod, 10L) },
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("+10")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DispatchTabContent(
    orders: List<ShopOrder>,
    onAdvanceOrder: (ShopOrder) -> Unit
) {
    val activeOrders = orders.filter { it.trackingStatusEnum != TrackingStatus.DELIVERED }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Active Fulfillment Queue (${activeOrders.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (activeOrders.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "All active orders have been delivered!",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(activeOrders, key = { it.id }) { order ->
                OrderTrackingCard(
                    order = order,
                    isAgentMode = true,
                    onAdvanceOrder = { onAdvanceOrder(order) }
                )
            }
        }
    }
}

@Composable
private fun EditProductDialog(
    existing: Product?,
    onDismiss: () -> Unit,
    onSave: (
        sku: String,
        name: String,
        brand: String,
        category: ProductCategory,
        subcategory: String,
        description: String,
        price: Double,
        costPrice: Double,
        stockCount: Long,
        warehouseBin: String,
        colorway: String,
        imageKey: String,
        sizes: List<String>
    ) -> Unit
) {
    var sku by remember { mutableStateOf(existing?.sku ?: "SNU-SH-009") }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var brand by remember { mutableStateOf(existing?.brand ?: "Sanu Atelier") }
    var category by remember { mutableStateOf(existing?.categoryEnum ?: ProductCategory.SHOES) }
    var subcategory by remember { mutableStateOf(existing?.subcategory ?: "Sneakers") }
    var description by remember { mutableStateOf(existing?.description ?: "Handcrafted Sanu footwear with ergonomic arch cushion.") }
    var priceText by remember { mutableStateOf(existing?.price?.toString() ?: "95.0") }
    var costText by remember { mutableStateOf(existing?.costPrice?.toString() ?: "38.0") }
    var stockText by remember { mutableStateOf(existing?.stockCount?.toString() ?: "15") }
    var bin by remember { mutableStateOf(existing?.warehouseBin ?: "AISLE-C · BIN-04") }
    var colorway by remember { mutableStateOf(existing?.colorway ?: "Warm Sand") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (existing == null) "Add Footwear SKU" else "Edit ${existing.sku}",
                    style = MaterialTheme.typography.titleLarge
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = category == ProductCategory.SHOES,
                        onClick = {
                            category = ProductCategory.SHOES
                            subcategory = "Sneakers"
                        },
                        label = { Text("Shoes") }
                    )
                    FilterChip(
                        selected = category == ProductCategory.SLIPPERS,
                        onClick = {
                            category = ProductCategory.SLIPPERS
                            subcategory = "Cork Slides"
                        },
                        label = { Text("Slippers") }
                    )
                }

                OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU Code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subcategory, onValueChange = { subcategory = it }, label = { Text("Subcategory") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("Cost ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it.filter { c -> c.isDigit() } },
                        label = { Text("Stock Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = bin,
                        onValueChange = { bin = it },
                        label = { Text("Warehouse Bin") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                }

                OutlinedTextField(value = colorway, onValueChange = { colorway = it }, label = { Text("Colorway") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, maxLines = 2, modifier = Modifier.fillMaxWidth())

                if (errorText != null) {
                    Text(errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            val parsedPrice = priceText.toDoubleOrNull()
                            val parsedCost = costText.toDoubleOrNull() ?: 0.0
                            val parsedStock = stockText.toLongOrNull() ?: 0L
                            if (name.trim().length < 2 || parsedPrice == null || parsedPrice <= 0.0) {
                                errorText = "Enter valid name and positive price."
                                return@Button
                            }
                            val imageKey = if (category == ProductCategory.SLIPPERS) "slipper"
                            else if (subcategory.contains("loafer", true) || subcategory.contains("boot", true)) "loafer"
                            else "sneaker"
                            onSave(
                                sku,
                                name,
                                brand,
                                category,
                                subcategory,
                                description,
                                parsedPrice,
                                parsedCost,
                                parsedStock,
                                bin,
                                colorway,
                                imageKey,
                                existing?.sizes ?: listOf("US 7", "US 8", "US 9", "US 10", "US 11")
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save SKU")
                    }
                }
            }
        }
    }
}
