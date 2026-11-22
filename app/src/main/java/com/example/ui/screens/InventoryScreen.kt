package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.UiState
import com.example.ui.components.QrTagDialog
import com.example.ui.components.StockStatusPill
import com.example.ui.components.resolveProductDrawable
import com.example.ui.viewmodel.InventoryFilter
import com.example.ui.viewmodel.InventorySort
import com.example.ui.viewmodel.InventorySummary
import com.example.util.PaymentUtils

@Composable
fun InventoryScreen(
    inventoryState: UiState<List<Product>>,
    filteredProducts: List<Product>,
    summary: InventorySummary,
    selectedFilter: InventoryFilter,
    searchQuery: String,
    sortOption: InventorySort,
    selectedProduct: Product?,
    onSelectProduct: (Product?) -> Unit,
    onFilterSelected: (InventoryFilter) -> Unit,
    onSearchChanged: (String) -> Unit,
    onSortChanged: (InventorySort) -> Unit,
    onAdjustStock: (Product, Long) -> Unit,
    onSaveProduct: (
        existing: Product?,
        name: String,
        price: Double,
        size: String,
        image: String,
        category: ProductCategory,
        sku: String,
        brand: String,
        stockCount: Long,
        warehouseBin: String,
        description: String
    ) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onAddToCart: (Product, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditorDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var productPendingDelete by remember { mutableStateOf<Product?>(null) }
    var productForQrTag by remember { mutableStateOf<Product?>(null) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isSplitPane = maxWidth >= 840.dp

        if (isSplitPane) {
            Row(modifier = Modifier.fillMaxSize()) {
                InventoryListPane(
                    inventoryState = inventoryState,
                    filteredProducts = filteredProducts,
                    summary = summary,
                    selectedFilter = selectedFilter,
                    searchQuery = searchQuery,
                    sortOption = sortOption,
                    selectedProductId = selectedProduct?.id,
                    onFilterSelected = onFilterSelected,
                    onSearchChanged = onSearchChanged,
                    onSortChanged = onSortChanged,
                    onProductClick = onSelectProduct,
                    onAddClick = {
                        editingProduct = null
                        showEditorDialog = true
                    },
                    onAdjustStock = onAdjustStock,
                    onShowQr = { productForQrTag = it },
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                )
                HorizontalDivider(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                )
                Box(
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxHeight()
                ) {
                    val activeDetail = selectedProduct ?: filteredProducts.firstOrNull()
                    if (activeDetail != null) {
                        InventoryProductDetailScreen(
                            product = activeDetail,
                            showBackButton = false,
                            onBack = { onSelectProduct(null) },
                            onEdit = {
                                editingProduct = activeDetail
                                showEditorDialog = true
                            },
                            onDelete = { productPendingDelete = activeDetail },
                            onAdjustStock = { delta -> onAdjustStock(activeDetail, delta) },
                            onShowQr = { productForQrTag = activeDetail },
                            onAddToCart = { size -> onAddToCart(activeDetail, size) }
                        )
                    } else {
                        InventoryEmptyDetailPlaceholder()
                    }
                }
            }
        } else {
            if (selectedProduct != null) {
                BackHandler { onSelectProduct(null) }
                InventoryProductDetailScreen(
                    product = selectedProduct,
                    showBackButton = true,
                    onBack = { onSelectProduct(null) },
                    onEdit = {
                        editingProduct = selectedProduct
                        showEditorDialog = true
                    },
                    onDelete = { productPendingDelete = selectedProduct },
                    onAdjustStock = { delta -> onAdjustStock(selectedProduct, delta) },
                    onShowQr = { productForQrTag = selectedProduct },
                    onAddToCart = { size -> onAddToCart(selectedProduct, size) }
                )
            } else {
                InventoryListPane(
                    inventoryState = inventoryState,
                    filteredProducts = filteredProducts,
                    summary = summary,
                    selectedFilter = selectedFilter,
                    searchQuery = searchQuery,
                    sortOption = sortOption,
                    selectedProductId = null,
                    onFilterSelected = onFilterSelected,
                    onSearchChanged = onSearchChanged,
                    onSortChanged = onSortChanged,
                    onProductClick = onSelectProduct,
                    onAddClick = {
                        editingProduct = null
                        showEditorDialog = true
                    },
                    onAdjustStock = onAdjustStock,
                    onShowQr = { productForQrTag = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showEditorDialog) {
        InventoryProductEditorDialog(
            existing = editingProduct,
            onDismiss = { showEditorDialog = false },
            onSave = { name, price, size, image, category, sku, brand, stockCount, bin, desc ->
                onSaveProduct(
                    editingProduct,
                    name,
                    price,
                    size,
                    image,
                    category,
                    sku,
                    brand,
                    stockCount,
                    bin,
                    desc
                )
                showEditorDialog = false
            }
        )
    }

    productPendingDelete?.let { prod ->
        AlertDialog(
            onDismissRequest = { productPendingDelete = null },
            title = { Text("Remove ${prod.name}?") },
            text = {
                Text("Delete ${prod.sku} from warehouse inventory? This removes it from Firestore immediately.")
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
                    modifier = Modifier.testTag("inventory_confirm_delete_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { productPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    productForQrTag?.let { prod ->
        QrTagDialog(
            product = prod,
            onDismiss = { productForQrTag = null }
        )
    }
}

@Composable
private fun InventoryListPane(
    inventoryState: UiState<List<Product>>,
    filteredProducts: List<Product>,
    summary: InventorySummary,
    selectedFilter: InventoryFilter,
    searchQuery: String,
    sortOption: InventorySort,
    selectedProductId: String?,
    onFilterSelected: (InventoryFilter) -> Unit,
    onSearchChanged: (String) -> Unit,
    onSortChanged: (InventorySort) -> Unit,
    onProductClick: (Product) -> Unit,
    onAddClick: () -> Unit,
    onAdjustStock: (Product, Long) -> Unit,
    onShowQr: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }

    when (inventoryState) {
        is UiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = inventoryState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        is UiState.Success -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = modifier.fillMaxSize()
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Warehouse Inventory",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${summary.totalSkus} products · ${summary.totalPairs} pairs in stock",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onAddClick,
                            modifier = Modifier.testTag("inventory_add_product_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Item")
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InventoryStatChip(
                            label = "Stock Value",
                            value = PaymentUtils.formatCurrency(summary.totalRetailValue),
                            modifier = Modifier.weight(1f)
                        )
                        InventoryStatChip(
                            label = "Low Stock",
                            value = "${summary.lowStockCount} SKUs",
                            highlightError = summary.lowStockCount > 0,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChanged,
                            placeholder = { Text("Search name, size, or SKU...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChanged("") }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear inventory search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("inventory_search_input")
                        )

                        Box {
                            OutlinedButton(
                                onClick = { showSortMenu = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                                modifier = Modifier.testTag("inventory_sort_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort inventory", modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                InventorySort.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.label,
                                                fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            onSortChanged(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InventoryFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { onFilterSelected(filter) },
                                label = { Text(filter.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("inventory_filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }

                if (filteredProducts.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Inventory2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No matching inventory items",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Adjust your search/filters or add a new shoe or slipper SKU.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredProducts, key = { it.id }) { product ->
                        InventoryListItemCard(
                            product = product,
                            isSelected = product.id == selectedProductId,
                            onClick = { onProductClick(product) },
                            onAdjustStock = { delta -> onAdjustStock(product, delta) },
                            onShowQr = { onShowQr(product) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryStatChip(
    label: String,
    value: String,
    highlightError: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (highlightError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (highlightError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun InventoryListItemCard(
    product: Product,
    isSelected: Boolean,
    onClick: () -> Unit,
    onAdjustStock: (Long) -> Unit,
    onShowQr: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
            else MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("inventory_item_card_${product.sku}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Image(
                    painter = painterResource(
                        id = resolveProductDrawable(product.image, product.categoryEnum)
                    ),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(5.dp)
                ) {
                    Text(
                        text = product.categoryEnum.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${product.sku} · ${product.warehouseBin}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = PaymentUtils.formatCurrency(product.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Straighten,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Size: ${product.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StockStatusPill(product = product)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onShowQr,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCode2,
                                contentDescription = "QR code for ${product.sku}",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { onAdjustStock(-1L) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = "Decrease ${product.name} stock",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { onAdjustStock(1L) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Increase ${product.name} stock",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InventoryProductDetailScreen(
    product: Product,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustStock: (Long) -> Unit,
    onShowQr: () -> Unit,
    onAddToCart: (String) -> Unit
) {
    var selectedSize by remember(product.id) {
        mutableStateOf(product.sizes.firstOrNull() ?: "US 9")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (showBackButton) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("inventory_detail_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("All Inventory")
                }
                Row {
                    IconButton(onClick = onShowQr) {
                        Icon(Icons.Filled.QrCode2, contentDescription = "View QR Tag")
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("inventory_detail_edit_button")) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Product")
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.testTag("inventory_detail_delete_button")) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "Delete Product",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onShowQr) {
                    Icon(Icons.Filled.QrCode2, contentDescription = "View QR Tag")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Product")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Delete Product",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(MaterialTheme.shapes.large)
        ) {
            Image(
                painter = painterResource(
                    id = resolveProductDrawable(product.image, product.categoryEnum)
                ),
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = "${product.categoryEnum.label} · ${product.sku}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.brand.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = PaymentUtils.formatCurrency(product.price),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StockStatusPill(product = product)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = product.warehouseBin,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Text(
            text = product.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Available Sizes (${product.size})",
                style = MaterialTheme.typography.titleSmall
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                product.sizes.forEach { sizeOption ->
                    val isSelected = selectedSize == sizeOption
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { selectedSize = sizeOption }
                    ) {
                        Text(
                            text = sizeOption,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

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
                Column {
                    Text("Warehouse Count", style = MaterialTheme.typography.labelMedium)
                    Text("${product.stockCount} pairs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAdjustStock(-1L) }) { Text("-1") }
                    OutlinedButton(onClick = { onAdjustStock(1L) }) { Text("+1") }
                    Button(onClick = { onAdjustStock(10L) }) { Text("+10") }
                }
            }
        }

        Button(
            onClick = { onAddToCart(selectedSize) },
            enabled = product.stockCount > 0L,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Filled.AddShoppingCart, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (product.stockCount > 0L) "Add $selectedSize to Bag · ${PaymentUtils.formatCurrency(product.price)}"
                else "Out of Stock"
            )
        }
    }
}

@Composable
private fun InventoryEmptyDetailPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Text("Select an inventory item", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Tap any shoe or slipper from the list to inspect sizes, stock count, and QR tags.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InventoryProductEditorDialog(
    existing: Product?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        price: Double,
        size: String,
        image: String,
        category: ProductCategory,
        sku: String,
        brand: String,
        stockCount: Long,
        warehouseBin: String,
        description: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var priceText by remember { mutableStateOf(existing?.price?.toString() ?: "95.0") }
    var sizeText by remember { mutableStateOf(existing?.size ?: "US 8, US 9, US 10") }
    var imageKey by remember { mutableStateOf(existing?.image ?: "sneaker") }
    var category by remember { mutableStateOf(existing?.categoryEnum ?: ProductCategory.SHOES) }
    var sku by remember { mutableStateOf(existing?.sku ?: "SNU-NEW-01") }
    var brand by remember { mutableStateOf(existing?.brand ?: "Sanu Atelier") }
    var stockText by remember { mutableStateOf(existing?.stockCount?.toString() ?: "16") }
    var bin by remember { mutableStateOf(existing?.warehouseBin ?: "AISLE-C · BIN-02") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
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
                    text = if (existing == null) "New Inventory Product" else "Edit ${existing.sku}",
                    style = MaterialTheme.typography.titleLarge
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = category == ProductCategory.SHOES,
                        onClick = {
                            category = ProductCategory.SHOES
                            if (imageKey == "slipper") imageKey = "sneaker"
                        },
                        label = { Text("Shoes") }
                    )
                    FilterChip(
                        selected = category == ProductCategory.SLIPPERS,
                        onClick = {
                            category = ProductCategory.SLIPPERS
                            imageKey = "slipper"
                        },
                        label = { Text("Slippers") }
                    )
                }

                Text("Product Image", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("sneaker" to "Sneaker", "loafer" to "Loafer", "slipper" to "Slipper").forEach { (key, label) ->
                        FilterChip(
                            selected = imageKey == key,
                            onClick = { imageKey = key },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inventory_editor_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inventory_editor_price_input")
                    )
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it.filter { c -> c.isDigit() } },
                        label = { Text("Stock Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = sizeText,
                    onValueChange = { sizeText = it },
                    label = { Text("Sizes (comma-separated)") },
                    placeholder = { Text("US 8, US 9, US 10") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inventory_editor_size_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = bin,
                        onValueChange = { bin = it },
                        label = { Text("Bin") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                }

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val parsedPrice = priceText.toDoubleOrNull()
                            val parsedStock = stockText.toLongOrNull() ?: 0L
                            if (name.trim().length < 2 || parsedPrice == null || parsedPrice <= 0.0) {
                                errorText = "Enter a valid name and positive price."
                                return@Button
                            }
                            onSave(
                                name,
                                parsedPrice,
                                sizeText,
                                imageKey,
                                category,
                                sku,
                                brand,
                                parsedStock,
                                bin,
                                description
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inventory_editor_save_button")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
