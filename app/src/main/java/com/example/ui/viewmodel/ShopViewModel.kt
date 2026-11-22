package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AnalyticsSummary
import com.example.data.model.CartItem
import com.example.data.model.DailyRevenuePoint
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ShopNotification
import com.example.data.model.ShopOrder
import com.example.data.model.ShopUserProfile
import com.example.data.model.TrackingStatus
import com.example.data.model.UiState
import com.example.data.repository.ProductRepository
import com.example.data.repository.ShopRepository
import com.example.ui.auth.AgentAuthConfig
import com.example.util.NotificationHelper
import com.example.util.PaymentUtils
import com.example.util.QrCodeUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CategoryFilter(val label: String) {
    ALL("All Footwear"),
    SHOES("Shoes"),
    SLIPPERS("Slippers")
}

class ShopViewModel(
    private val repository: ShopRepository,
    private val productRepository: ProductRepository,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String,
    private val initialAgentBadge: String? = null,
    private val initialAgentBranch: String? = null
) : ViewModel() {

    private companion object {
        const val TAG = "ShopViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    init {
        viewModelScope.launch {
            val seedResult = repository.ensureSeeded(currentUserId, userDisplayName, userEmail)
            val initialBadge = initialAgentBadge?.trim()?.uppercase(Locale.US)
            if (seedResult.isSuccess && !initialBadge.isNullOrBlank()) {
                val branch = initialAgentBranch?.takeIf { it.isNotBlank() }
                    ?: AgentAuthConfig.STORE_BRANCHES.first()
                repository.setAgentMode(
                    userId = currentUserId,
                    enabled = true,
                    badgeId = initialBadge,
                    branch = branch
                ).onSuccess {
                    _actionStatus.value = "Agent $initialBadge signed in to Manager Portal."
                }.onFailure { err ->
                    _actionStatus.value = err.message ?: "Unable to activate agent session."
                }
            }
        }
    }

    val profileState: StateFlow<ShopUserProfile?> = repository.observeProfile(currentUserId)
        .catch { error ->
            unwrapOrThrow(error, "profile")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val productsState: StateFlow<UiState<List<Product>>> =
        observeAsUiState(productRepository.observeProducts(currentUserId), "products")

    val ordersState: StateFlow<UiState<List<ShopOrder>>> =
        observeAsUiState(repository.observeOrders(currentUserId), "orders")

    val notificationsState: StateFlow<UiState<List<ShopNotification>>> =
        observeAsUiState(repository.observeNotifications(currentUserId), "notifications")

    private val _selectedCategory = MutableStateFlow(CategoryFilter.ALL)
    val selectedCategory: StateFlow<CategoryFilter> = _selectedCategory.asStateFlow()

    private val _selectedSubcategory = MutableStateFlow("All")
    val selectedSubcategory: StateFlow<String> = _selectedSubcategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _scannedProduct = MutableStateFlow<Product?>(null)
    val scannedProduct: StateFlow<Product?> = _scannedProduct.asStateFlow()

    private val _recentScannedProducts = MutableStateFlow<List<Product>>(emptyList())
    val recentScannedProducts: StateFlow<List<Product>> = _recentScannedProducts.asStateFlow()

    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

    private val _actionStatus = MutableStateFlow<String?>(null)
    val actionStatus: StateFlow<String?> = _actionStatus.asStateFlow()

    private val _lastPlacedOrder = MutableStateFlow<ShopOrder?>(null)
    val lastPlacedOrder: StateFlow<ShopOrder?> = _lastPlacedOrder.asStateFlow()

    private val _isProcessingPayment = MutableStateFlow(false)
    val isProcessingPayment: StateFlow<Boolean> = _isProcessingPayment.asStateFlow()

    val filteredProducts: StateFlow<List<Product>> = combine(
        productsState,
        selectedCategory,
        selectedSubcategory,
        searchQuery
    ) { state, category, subcat, query ->
        val items = (state as? UiState.Success)?.data ?: emptyList()
        items.filter { product ->
            val matchesCat = when (category) {
                CategoryFilter.ALL -> true
                CategoryFilter.SHOES -> product.categoryEnum == ProductCategory.SHOES
                CategoryFilter.SLIPPERS -> product.categoryEnum == ProductCategory.SLIPPERS
            }
            val matchesSub = subcat == "All" || product.subcategory.equals(subcat, ignoreCase = true)
            val q = query.trim()
            val matchesQuery = q.isEmpty() ||
                product.name.contains(q, ignoreCase = true) ||
                product.brand.contains(q, ignoreCase = true) ||
                product.sku.contains(q, ignoreCase = true) ||
                product.subcategory.contains(q, ignoreCase = true)
            matchesCat && matchesSub && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val analyticsSummary: StateFlow<AnalyticsSummary> = combine(
        productsState,
        ordersState
    ) { prodState, ordState ->
        val products = (prodState as? UiState.Success)?.data ?: emptyList()
        val orders = (ordState as? UiState.Success)?.data ?: emptyList()

        val totalRevenue = orders.sumOf { it.totalAmount }
        val totalProfit = orders.sumOf { it.estimatedProfit }
        val totalOrders = orders.size
        val totalUnits = orders.sumOf { it.totalUnits }
        val avgOrder = if (totalOrders > 0) totalRevenue / totalOrders else 0.0

        val shoesOrderRev = orders.filter { it.primaryCategory == "SHOES" }.sumOf { it.totalAmount } +
            orders.filter { it.primaryCategory == "MIXED" }.sumOf { it.totalAmount * 0.62 }
        val slippersOrderRev = orders.filter { it.primaryCategory == "SLIPPERS" }.sumOf { it.totalAmount } +
            orders.filter { it.primaryCategory == "MIXED" }.sumOf { it.totalAmount * 0.38 }

        val lowStockCount = products.count { it.isLowStock }
        val totalInventory = products.sumOf { it.stockCount }
        val topProducts = products.sortedByDescending { it.soldCount }.take(5)

        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Today")
        val baseShoes = (shoesOrderRev.coerceAtLeast(180.0)) / 7.0
        val baseSlippers = (slippersOrderRev.coerceAtLeast(120.0)) / 7.0
        val weights = listOf(0.72, 0.85, 0.78, 1.05, 1.18, 1.32, 1.1 + (totalOrders * 0.06))
        val series = dayLabels.mapIndexed { idx, label ->
            val w = weights[idx]
            DailyRevenuePoint(
                label = label,
                shoesRevenue = baseShoes * w,
                slippersRevenue = baseSlippers * (w * 0.92)
            )
        }

        AnalyticsSummary(
            totalRevenue = totalRevenue,
            totalProfit = totalProfit,
            totalOrders = totalOrders,
            totalUnitsSold = totalUnits,
            averageOrderValue = avgOrder,
            shoesRevenue = shoesOrderRev,
            slippersRevenue = slippersOrderRev,
            lowStockCount = lowStockCount,
            totalInventoryUnits = totalInventory,
            topProducts = topProducts,
            dailySeries = series
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AnalyticsSummary())

    val unreadNotificationsCount: StateFlow<Int> = notificationsState
        .map { state ->
            (state as? UiState.Success)?.data?.count { !it.readFlag } ?: 0
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

    fun setCategoryFilter(filter: CategoryFilter) {
        _selectedCategory.value = filter
        _selectedSubcategory.value = "All"
    }

    fun setSubcategoryFilter(subcategory: String) {
        _selectedSubcategory.value = subcategory
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addToCart(product: Product, selectedSize: String) {
        if (product.stockCount <= 0L) {
            _actionStatus.value = "${product.name} is currently out of stock."
            return
        }
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id && it.selectedSize == selectedSize }
        if (index >= 0) {
            val existing = current[index]
            if (existing.quantity < product.stockCount) {
                current[index] = existing.copy(quantity = existing.quantity + 1)
                _actionStatus.value = "Added another ${product.name} ($selectedSize) to bag"
            } else {
                _actionStatus.value = "Max available warehouse stock (${product.stockCount}) in bag"
            }
        } else {
            current.add(CartItem(product = product, selectedSize = selectedSize, quantity = 1))
            _actionStatus.value = "Added ${product.name} ($selectedSize) to bag"
        }
        _cartItems.value = current
    }

    fun updateCartQuantity(productId: String, size: String, delta: Int) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId && it.selectedSize == size }
        if (index < 0) return
        val item = current[index]
        val nextQty = item.quantity + delta
        if (nextQty <= 0) {
            current.removeAt(index)
        } else if (nextQty <= item.product.stockCount) {
            current[index] = item.copy(quantity = nextQty)
        } else {
            _actionStatus.value = "Only ${item.product.stockCount} pairs available in warehouse."
        }
        _cartItems.value = current
    }

    fun clearActionStatus() {
        _actionStatus.value = null
    }

    fun clearLastPlacedOrder() {
        _lastPlacedOrder.value = null
    }

    fun lookupByQrOrSku(rawInput: String) {
        val products = (productsState.value as? UiState.Success)?.data ?: emptyList()
        val match = QrCodeUtils.findMatchingProduct(rawInput, products)
        if (match != null) {
            _scannedProduct.value = match
            _recentScannedProducts.value = (listOf(match) + _recentScannedProducts.value.filterNot { it.id == match.id }).take(5)
            _scanMessage.value = "Verified ${match.sku} at ${match.warehouseBin}"
        } else {
            _scannedProduct.value = null
            _scanMessage.value = "No footwear SKU matched \"$rawInput\". Try SNU-SH-001 or SNU-SL-001."
        }
    }

    fun clearScanResult() {
        _scannedProduct.value = null
        _scanMessage.value = null
    }

    fun signInAgent(badgeId: String, pin: String, branch: String): Boolean {
        val validationError = AgentAuthConfig.validateAgentCredentials(badgeId, pin)
        if (validationError != null) {
            _actionStatus.value = validationError
            return false
        }
        val cleanBadge = badgeId.trim().uppercase(Locale.US)
        val cleanBranch = branch.ifBlank { AgentAuthConfig.STORE_BRANCHES.first() }
        viewModelScope.launch {
            val result = repository.setAgentMode(
                userId = currentUserId,
                enabled = true,
                badgeId = cleanBadge,
                branch = cleanBranch
            )
            result.onSuccess {
                _actionStatus.value = "Agent $cleanBadge signed in to Manager Portal."
            }.onFailure { err ->
                _actionStatus.value = err.message ?: "Unable to activate agent session."
            }
        }
        return true
    }

    fun signOutAgent() {
        viewModelScope.launch {
            repository.setAgentMode(
                userId = currentUserId,
                enabled = false,
                badgeId = "",
                branch = profileState.value?.storeBranch ?: "Sanu Flagship · Downtown"
            ).onSuccess {
                _actionStatus.value = "Switched back to Customer Shopper mode."
            }
        }
    }

    fun submitCheckout(
        context: Context,
        customerName: String,
        shippingAddress: String,
        paymentMethod: PaymentMethod,
        cardOrWalletInput: String,
        onComplete: (ShopOrder) -> Unit
    ) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            _actionStatus.value = "Your shopping bag is empty."
            return
        }
        val paymentRef = PaymentUtils.buildPaymentReference(paymentMethod.wireValue, cardOrWalletInput)
        val activeAgent = profileState.value?.agentBadgeId?.ifBlank { "AGT-104" } ?: "AGT-104"

        viewModelScope.launch {
            _isProcessingPayment.value = true
            val result = repository.placeOrder(
                userId = currentUserId,
                customerName = customerName.ifBlank { userDisplayName },
                shippingAddress = shippingAddress,
                cartItems = items,
                paymentMethod = paymentMethod,
                paymentReference = paymentRef,
                assignedAgent = activeAgent
            )
            _isProcessingPayment.value = false
            result.onSuccess { placed ->
                _cartItems.value = emptyList()
                _lastPlacedOrder.value = placed
                _actionStatus.value = "Payment authorized ($paymentRef)! Order ${placed.orderNumber} placed."
                NotificationHelper.showPushNotification(
                    context = context,
                    title = "Sanu Shop · Order ${placed.orderNumber}",
                    message = "Payment verified (${PaymentUtils.formatCurrency(placed.totalAmount)}). Tracking status: Order Placed."
                )
                onComplete(placed)
            }.onFailure { err ->
                _actionStatus.value = err.message ?: "Checkout failed. Please try again."
            }
        }
    }

    fun advanceOrderTracking(context: Context, order: ShopOrder) {
        val badge = profileState.value?.agentBadgeId?.ifBlank { "AGT-104" } ?: "AGT-104"
        viewModelScope.launch {
            val result = repository.advanceOrderTracking(currentUserId, order, badge)
            result.onSuccess { nextStatus ->
                _actionStatus.value = "${order.orderNumber} updated to ${nextStatus.title}"
                NotificationHelper.showPushNotification(
                    context = context,
                    title = "Order ${order.orderNumber} · ${nextStatus.title}",
                    message = "${nextStatus.defaultNote} (${nextStatus.defaultEta})"
                )
            }.onFailure { err ->
                _actionStatus.value = err.message ?: "Failed to update tracking status"
            }
        }
    }

    fun adjustStock(context: Context, product: Product, delta: Long) {
        val nextStock = (product.stockCount + delta).coerceIn(0L, 100000L)
        viewModelScope.launch {
            val res = productRepository.adjustStock(currentUserId, product, nextStock)
            res.onSuccess { updatedStock ->
                if (_scannedProduct.value?.id == product.id) {
                    _scannedProduct.value = product.copy(stockCount = updatedStock)
                }
                _actionStatus.value = "${product.sku} stock updated to $updatedStock pairs"
                if (updatedStock <= product.lowStockThreshold) {
                    repository.recordLowStockNotification(currentUserId, product, updatedStock)
                    NotificationHelper.showPushNotification(
                        context = context,
                        title = "Low Stock Alert · ${product.sku}",
                        message = "${product.name} at ${product.warehouseBin} has $updatedStock pairs left."
                    )
                }
            }.onFailure { err ->
                _actionStatus.value = err.message ?: "Failed to update stock"
            }
        }
    }

    fun saveInventoryProduct(
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
    ) {
        viewModelScope.launch {
            val candidate = Product(
                id = existing?.id.orEmpty(),
                userId = currentUserId,
                sku = sku,
                name = name,
                brand = brand,
                category = category.wireValue,
                subcategory = subcategory,
                description = description,
                price = price,
                costPrice = costPrice,
                stockCount = stockCount,
                lowStockThreshold = existing?.lowStockThreshold ?: 6L,
                warehouseBin = warehouseBin,
                qrPayload = "SANU:${sku.trim().uppercase().ifBlank { "SNU-CUSTOM" }}",
                imageKey = imageKey,
                sizes = sizes,
                colorway = colorway,
                rating = existing?.rating ?: 4.8,
                soldCount = existing?.soldCount ?: 0L,
                featured = existing?.featured ?: false
            )
            if (existing == null) {
                productRepository.createProduct(currentUserId, candidate)
                    .onSuccess { created ->
                        _actionStatus.value = "Added ${created.name} to inventory"
                    }
                    .onFailure { err ->
                        _actionStatus.value = err.message ?: "Unable to add product"
                    }
            } else {
                productRepository.updateProduct(currentUserId, candidate)
                    .onSuccess { updated ->
                        if (_scannedProduct.value?.id == updated.id) {
                            _scannedProduct.value = updated
                        }
                        _actionStatus.value = "Updated ${updated.name}"
                    }
                    .onFailure { err ->
                        _actionStatus.value = err.message ?: "Unable to update product"
                    }
            }
        }
    }

    fun deleteInventoryProduct(product: Product) {
        if (product.id.isBlank()) return
        viewModelScope.launch {
            productRepository.deleteProduct(currentUserId, product.id)
                .onSuccess {
                    _cartItems.value = _cartItems.value.filterNot { it.product.id == product.id }
                    _recentScannedProducts.value = _recentScannedProducts.value.filterNot { it.id == product.id }
                    if (_scannedProduct.value?.id == product.id) {
                        _scannedProduct.value = null
                        _scanMessage.value = null
                    }
                    _actionStatus.value = "Removed ${product.sku} (${product.name}) from inventory"
                }
                .onFailure { err ->
                    _actionStatus.value = err.message ?: "Unable to delete product"
                }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(currentUserId, notificationId)
        }
    }

    fun markAllNotificationsRead() {
        val current = (notificationsState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.markAllNotificationsRead(currentUserId, current)
        }
    }

    private fun <T> observeAsUiState(
        upstream: Flow<T>,
        streamName: String
    ): StateFlow<UiState<T>> = upstream
        .map<T, UiState<T>> { UiState.Success(it) }
        .catch { error ->
            val rootError = unwrapOrThrow(error, streamName)
            emit(UiState.Error(rootError.message ?: "Failed to observe $streamName"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private fun unwrapOrThrow(error: Throwable, streamName: String): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $streamName", rootError)
        return rootError
    }
}
