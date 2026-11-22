package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.UiState
import com.example.data.repository.ProductRepository
import com.example.data.repository.ShopRepository
import com.example.ui.auth.AgentLoginDialog
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.screens.CartCheckoutBottomSheet
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.ManagerDashboardScreen
import com.example.ui.screens.NotificationsBottomSheet
import com.example.ui.screens.OrdersTrackingScreen
import com.example.ui.screens.QrLookupScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.theme.SanuShopTheme
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private const val ROUTE_SHOP = "shop"
private const val ROUTE_INVENTORY = "inventory"
private const val ROUTE_SCAN = "scan"
private const val ROUTE_ORDERS = "orders"
private const val ROUTE_MANAGER = "manager"

private data class ShopNavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val shopDestinations = listOf(
    ShopNavDestination(ROUTE_SHOP, R.string.nav_shop, Icons.Filled.Storefront, Icons.Outlined.Storefront),
    ShopNavDestination(ROUTE_INVENTORY, R.string.nav_inventory, Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    ShopNavDestination(ROUTE_SCAN, R.string.nav_scan, Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner),
    ShopNavDestination(ROUTE_ORDERS, R.string.nav_orders, Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping),
    ShopNavDestination(ROUTE_MANAGER, R.string.nav_manager, Icons.Filled.Analytics, Icons.Outlined.Analytics)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannel(this)
        setContent {
            SanuShopTheme {
                SanuShopRoot()
            }
        }
    }
}

@Composable
fun SanuShopRoot() {
    val auth = Firebase.auth
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var pendingAgentSession by remember { mutableStateOf<Pair<String, String>?>(null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val nextUser = firebaseAuth.currentUser
            if (nextUser == null) {
                pendingAgentSession = null
            }
            currentUser = nextUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = auth.currentUser },
            onAgentAuthSuccess = { badgeId, branch ->
                pendingAgentSession = badgeId to branch
                currentUser = auth.currentUser
            }
        )
    } else {
        key(user.uid) {
            SanuShopApp(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: "Sanu Shopper",
                userEmail = user.email ?: "",
                initialAgentBadge = pendingAgentSession?.first,
                initialAgentBranch = pendingAgentSession?.second
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanuShopApp(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String,
    initialAgentBadge: String? = null,
    initialAgentBranch: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val db = remember(context) {
        FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
    }

    val viewModel: ShopViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                ShopViewModel(
                    repository = ShopRepository(db),
                    productRepository = ProductRepository(db),
                    currentUserId = currentUserId,
                    userDisplayName = userDisplayName,
                    userEmail = userEmail,
                    initialAgentBadge = initialAgentBadge,
                    initialAgentBranch = initialAgentBranch
                )
            }
        }
    )

    val inventoryViewModel: InventoryViewModel = viewModel(
        key = "inv_$currentUserId",
        factory = viewModelFactory {
            initializer {
                InventoryViewModel(
                    productRepository = ProductRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedSubcategory by viewModel.selectedSubcategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val scannedProduct by viewModel.scannedProduct.collectAsStateWithLifecycle()
    val recentScannedProducts by viewModel.recentScannedProducts.collectAsStateWithLifecycle()
    val scanMessage by viewModel.scanMessage.collectAsStateWithLifecycle()
    val actionStatus by viewModel.actionStatus.collectAsStateWithLifecycle()
    val lastPlacedOrder by viewModel.lastPlacedOrder.collectAsStateWithLifecycle()
    val isProcessingPayment by viewModel.isProcessingPayment.collectAsStateWithLifecycle()
    val analytics by viewModel.analyticsSummary.collectAsStateWithLifecycle()
    val unreadNotifs by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()

    val inventoryState by inventoryViewModel.inventoryState.collectAsStateWithLifecycle()
    val filteredInventory by inventoryViewModel.filteredInventory.collectAsStateWithLifecycle()
    val inventorySummary by inventoryViewModel.inventorySummary.collectAsStateWithLifecycle()
    val inventoryFilter by inventoryViewModel.selectedFilter.collectAsStateWithLifecycle()
    val inventorySearch by inventoryViewModel.searchQuery.collectAsStateWithLifecycle()
    val inventorySort by inventoryViewModel.sortOption.collectAsStateWithLifecycle()
    val selectedInventoryProduct by inventoryViewModel.selectedProduct.collectAsStateWithLifecycle()
    val inventoryStatusMessage by inventoryViewModel.statusMessage.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_SHOP

    val snackbarHostState = remember { SnackbarHostState() }
    var showCartSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }
    var showAgentLoginDialog by remember { mutableStateOf(false) }

    LaunchedEffect(actionStatus) {
        val msg = actionStatus
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionStatus()
        }
    }

    LaunchedEffect(inventoryStatusMessage) {
        val msg = inventoryStatusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            inventoryViewModel.clearStatusMessage()
        }
    }

    val totalBagUnits = cartItems.sumOf { it.quantity }
    val productsList = (productsState as? UiState.Success)?.data ?: emptyList()

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.sanu_shop_logo),
                                contentDescription = "Sanu Shop logo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .height(34.dp)
                                    .width(46.dp)
                                    .testTag("topbar_brand_logo")
                            )
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (profile?.agentMode == true) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(50),
                                    onClick = { navigateTo(ROUTE_MANAGER) },
                                    modifier = Modifier.testTag("active_agent_badge_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.VerifiedUser,
                                            contentDescription = "Verified store agent",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = profile?.agentBadgeId?.ifBlank { "AGENT" } ?: "AGENT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    },
                    actions = {
                        if (profile?.agentMode != true) {
                            OutlinedButton(
                                onClick = { showAgentLoginDialog = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("topbar_agent_login_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Badge,
                                    contentDescription = "Store Agent Login",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Agent",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }

                        IconButton(
                            onClick = { showNotificationsSheet = true },
                            modifier = Modifier.testTag("notifications_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifs > 0) {
                                        Badge { Text("$unreadNotifs") }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = "Notifications"
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                viewModel.clearLastPlacedOrder()
                                showCartSheet = true
                            },
                            modifier = Modifier.testTag("shopping_bag_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (totalBagUnits > 0) {
                                        Badge { Text("$totalBagUnits") }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ShoppingBag,
                                    contentDescription = "Shopping Bag"
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { showAccountMenu = true },
                                modifier = Modifier.testTag("account_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = "Account and Agent menu"
                                )
                            }
                            DropdownMenu(
                                expanded = showAccountMenu,
                                onDismissRequest = { showAccountMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(userDisplayName, style = MaterialTheme.typography.titleSmall)
                                            if (userEmail.isNotBlank()) {
                                                Text(
                                                    userEmail,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (profile?.agentMode == true) {
                                                Text(
                                                    text = "Agent ${profile?.agentBadgeId} · ${profile?.storeBranch}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    onClick = { showAccountMenu = false }
                                )
                                HorizontalDivider()
                                if (profile?.agentMode == true) {
                                    DropdownMenuItem(
                                        text = { Text("Exit Agent Mode") },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Badge, contentDescription = null)
                                        },
                                        onClick = {
                                            showAccountMenu = false
                                            viewModel.signOutAgent()
                                        },
                                        modifier = Modifier.testTag("menu_agent_logout_item")
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("Store Agent Login") },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Badge, contentDescription = null)
                                        },
                                        onClick = {
                                            showAccountMenu = false
                                            showAgentLoginDialog = true
                                        },
                                        modifier = Modifier.testTag("menu_agent_login_item")
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Sign Out") },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                                    },
                                    onClick = {
                                        showAccountMenu = false
                                        signOut(credentialManager, scope)
                                    },
                                    modifier = Modifier.testTag("menu_sign_out_item")
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ) {
                        shopDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.weight(0.15f))
                        shopDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                val initialRoute = remember(initialAgentBadge) {
                    if (!initialAgentBadge.isNullOrBlank()) ROUTE_MANAGER else ROUTE_SHOP
                }
                NavHost(
                    navController = navController,
                    startDestination = initialRoute,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_SHOP) {
                        ShopScreen(
                            productsState = productsState,
                            filteredProducts = filteredProducts,
                            selectedCategory = selectedCategory,
                            selectedSubcategory = selectedSubcategory,
                            searchQuery = searchQuery,
                            onCategorySelected = viewModel::setCategoryFilter,
                            onSubcategorySelected = viewModel::setSubcategoryFilter,
                            onSearchChanged = viewModel::setSearchQuery,
                            onAddToCart = viewModel::addToCart,
                            onOpenQrScanner = { navigateTo(ROUTE_SCAN) }
                        )
                    }

                    composable(ROUTE_INVENTORY) {
                        InventoryScreen(
                            inventoryState = inventoryState,
                            filteredProducts = filteredInventory,
                            summary = inventorySummary,
                            selectedFilter = inventoryFilter,
                            searchQuery = inventorySearch,
                            sortOption = inventorySort,
                            selectedProduct = selectedInventoryProduct,
                            onSelectProduct = inventoryViewModel::selectProduct,
                            onFilterSelected = inventoryViewModel::setFilter,
                            onSearchChanged = inventoryViewModel::setSearchQuery,
                            onSortChanged = inventoryViewModel::setSortOption,
                            onAdjustStock = inventoryViewModel::adjustStock,
                            onSaveProduct = inventoryViewModel::saveProduct,
                            onDeleteProduct = inventoryViewModel::deleteProduct,
                            onAddToCart = viewModel::addToCart
                        )
                    }

                    composable(ROUTE_SCAN) {
                        QrLookupScreen(
                            products = productsList,
                            scannedProduct = scannedProduct,
                            recentScannedProducts = recentScannedProducts,
                            scanMessage = scanMessage,
                            isAgentMode = profile?.agentMode == true,
                            onLookupQr = viewModel::lookupByQrOrSku,
                            onClearScan = viewModel::clearScanResult,
                            onAddToCart = viewModel::addToCart,
                            onAdjustStock = { product, delta ->
                                viewModel.adjustStock(context, product, delta)
                            }
                        )
                    }

                    composable(ROUTE_ORDERS) {
                        OrdersTrackingScreen(
                            ordersState = ordersState,
                            isAgentMode = profile?.agentMode == true,
                            onAdvanceOrder = { order ->
                                viewModel.advanceOrderTracking(context, order)
                            }
                        )
                    }

                    composable(ROUTE_MANAGER) {
                        ManagerDashboardScreen(
                            profile = profile,
                            analytics = analytics,
                            productsState = productsState,
                            ordersState = ordersState,
                            onAgentSignIn = viewModel::signInAgent,
                            onAgentSignOut = viewModel::signOutAgent,
                            onAdjustStock = { product, delta ->
                                viewModel.adjustStock(context, product, delta)
                            },
                            onSaveProduct = viewModel::saveInventoryProduct,
                            onDeleteProduct = viewModel::deleteInventoryProduct,
                            onAdvanceOrder = { order ->
                                viewModel.advanceOrderTracking(context, order)
                            },
                            onOpenQrScanner = { navigateTo(ROUTE_SCAN) }
                        )
                    }
                }
            }
        }
    }

    if (showCartSheet) {
        CartCheckoutBottomSheet(
            cartItems = cartItems,
            defaultCustomerName = userDisplayName,
            isProcessing = isProcessingPayment,
            lastPlacedOrder = lastPlacedOrder,
            onUpdateQuantity = viewModel::updateCartQuantity,
            onCheckout = { name, addr, method, tokenSrc ->
                viewModel.submitCheckout(
                    context = context,
                    customerName = name,
                    shippingAddress = addr,
                    paymentMethod = method,
                    cardOrWalletInput = tokenSrc,
                    onComplete = {}
                )
            },
            onViewTracking = {
                showCartSheet = false
                navigateTo(ROUTE_ORDERS)
            },
            onDismiss = { showCartSheet = false }
        )
    }

    if (showNotificationsSheet) {
        NotificationsBottomSheet(
            notificationsState = notificationsState,
            onMarkRead = viewModel::markNotificationRead,
            onMarkAllRead = viewModel::markAllNotificationsRead,
            onDismiss = { showNotificationsSheet = false }
        )
    }

    if (showAgentLoginDialog) {
        AgentLoginDialog(
            initialBranch = profile?.storeBranch.orEmpty(),
            onDismiss = { showAgentLoginDialog = false },
            onAgentSignIn = viewModel::signInAgent
        )
    }
}
