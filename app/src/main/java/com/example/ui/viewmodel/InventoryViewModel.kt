package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.UiState
import com.example.data.repository.ProductRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class InventoryFilter(val label: String) {
    ALL("All"),
    SHOES("Shoes"),
    SLIPPERS("Slippers"),
    LOW_STOCK("Low Stock")
}

enum class InventorySort(val label: String) {
    FEATURED("Featured"),
    NAME_ASC("Name A–Z"),
    PRICE_LOW_HIGH("Price Low–High"),
    STOCK_LOW_HIGH("Lowest Stock")
}

data class InventorySummary(
    val totalSkus: Int = 0,
    val totalPairs: Long = 0L,
    val totalRetailValue: Double = 0.0,
    val lowStockCount: Int = 0
)

class InventoryViewModel(
    private val productRepository: ProductRepository,
    private val currentUserId: String
) : ViewModel() {

    private companion object {
        const val TAG = "InventoryViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    val inventoryState: StateFlow<UiState<List<Product>>> =
        productRepository.observeProducts(currentUserId)
            .map<List<Product>, UiState<List<Product>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing inventory", rootError)
                emit(UiState.Error(rootError.message ?: "Unable to load inventory"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _selectedFilter = MutableStateFlow(InventoryFilter.ALL)
    val selectedFilter: StateFlow<InventoryFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(InventorySort.FEATURED)
    val sortOption: StateFlow<InventorySort> = _sortOption.asStateFlow()

    private val _selectedProductId = MutableStateFlow<String?>(null)

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val inventorySummary: StateFlow<InventorySummary> = inventoryState
        .map { state ->
            val items = (state as? UiState.Success)?.data ?: emptyList()
            InventorySummary(
                totalSkus = items.size,
                totalPairs = items.sumOf { it.stockCount },
                totalRetailValue = items.sumOf { it.price * it.stockCount },
                lowStockCount = items.count { it.isLowStock }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), InventorySummary())

    val filteredInventory: StateFlow<List<Product>> = combine(
        inventoryState,
        selectedFilter,
        searchQuery,
        sortOption
    ) { state, filter, query, sort ->
        val products = (state as? UiState.Success)?.data ?: emptyList()
        val cleanQuery = query.trim()

        val filtered = products.filter { product ->
            val matchesFilter = when (filter) {
                InventoryFilter.ALL -> true
                InventoryFilter.SHOES -> product.categoryEnum == ProductCategory.SHOES
                InventoryFilter.SLIPPERS -> product.categoryEnum == ProductCategory.SLIPPERS
                InventoryFilter.LOW_STOCK -> product.isLowStock
            }
            val matchesQuery = cleanQuery.isEmpty() ||
                product.name.contains(cleanQuery, ignoreCase = true) ||
                product.sku.contains(cleanQuery, ignoreCase = true) ||
                product.brand.contains(cleanQuery, ignoreCase = true) ||
                product.size.contains(cleanQuery, ignoreCase = true) ||
                product.warehouseBin.contains(cleanQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }

        when (sort) {
            InventorySort.FEATURED -> filtered.sortedByDescending { it.featured }
            InventorySort.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            InventorySort.PRICE_LOW_HIGH -> filtered.sortedBy { it.price }
            InventorySort.STOCK_LOW_HIGH -> filtered.sortedBy { it.stockCount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val selectedProduct: StateFlow<Product?> = combine(
        inventoryState,
        _selectedProductId
    ) { state, selectedId ->
        if (selectedId == null) return@combine null
        val products = (state as? UiState.Success)?.data ?: return@combine null
        products.firstOrNull { it.id == selectedId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun setFilter(filter: InventoryFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(sort: InventorySort) {
        _sortOption.value = sort
    }

    fun selectProduct(product: Product?) {
        _selectedProductId.value = product?.id
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun saveProduct(
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
    ) {
        val parsedSizes = Product.parseSizes(size)
        val normalizedSku = sku.trim().uppercase().ifBlank { "SNU-${(100..999).random()}" }
        val product = Product(
            id = existing?.id.orEmpty(),
            userId = currentUserId,
            sku = normalizedSku,
            name = name,
            brand = brand,
            category = category.wireValue,
            subcategory = existing?.subcategory ?: if (category == ProductCategory.SHOES) "Sneakers" else "Cork Slides",
            description = description.ifBlank { "Crafted Sanu ${category.label.lowercase()} with ergonomic cushioned sole." },
            price = price,
            costPrice = existing?.costPrice ?: (price * 0.42),
            stockCount = stockCount,
            lowStockThreshold = existing?.lowStockThreshold ?: 6L,
            warehouseBin = warehouseBin,
            qrPayload = "SANU:$normalizedSku",
            imageKey = image,
            sizes = parsedSizes,
            colorway = existing?.colorway ?: "Warm Sand",
            rating = existing?.rating ?: 4.8,
            soldCount = existing?.soldCount ?: 0L,
            featured = existing?.featured ?: false
        )

        viewModelScope.launch {
            val result = if (existing == null) {
                productRepository.createProduct(currentUserId, product)
            } else {
                productRepository.updateProduct(currentUserId, product)
            }
            result.onSuccess { saved ->
                _statusMessage.value = if (existing == null) {
                    "Added ${saved.name} (${saved.size}) to inventory"
                } else {
                    "Updated ${saved.name}"
                }
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Unable to save inventory item"
            }
        }
    }

    fun adjustStock(product: Product, delta: Long) {
        val nextStock = (product.stockCount + delta).coerceIn(0L, 100000L)
        viewModelScope.launch {
            productRepository.adjustStock(currentUserId, product, nextStock)
                .onSuccess { updated ->
                    _statusMessage.value = "${product.sku} stock updated to $updated pairs"
                }
                .onFailure { err ->
                    _statusMessage.value = err.message ?: "Failed to update stock"
                }
        }
    }

    fun deleteProduct(product: Product) {
        if (product.id.isBlank()) return
        viewModelScope.launch {
            productRepository.deleteProduct(currentUserId, product.id)
                .onSuccess {
                    if (_selectedProductId.value == product.id) {
                        _selectedProductId.value = null
                    }
                    _statusMessage.value = "Deleted ${product.name} from inventory"
                }
                .onFailure { err ->
                    _statusMessage.value = err.message ?: "Failed to delete product"
                }
        }
    }
}
