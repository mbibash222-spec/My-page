package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ShopNotification
import com.example.data.model.ShopOrder
import com.example.data.model.ShopUserProfile
import com.example.data.model.TrackingStatus
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale

class ShopRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUserId != null) {
            check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user access." }
        }
        return uid
    }

    fun observeProfile(userId: String): Flow<ShopUserProfile?> = flow {
        val uid = requireUserId(userId)
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    snap.toObject(
                        ShopUserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, docRef.path, uid)
                    throw error
                }
        )
    }

    fun observeOrders(userId: String): Flow<List<ShopOrder>> = flow {
        val uid = requireUserId(userId)
        val colRef = db.collection("users").document(uid).collection("orders")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ShopOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, uid)
                    throw error
                }
        )
    }

    fun observeNotifications(userId: String): Flow<List<ShopNotification>> = flow {
        val uid = requireUserId(userId)
        val colRef = db.collection("users").document(uid).collection("notifications")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ShopNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, colRef.path, uid)
                    throw error
                }
        )
    }

    private fun handleFlowError(error: Throwable, op: OperationType, path: String, uid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    suspend fun ensureSeeded(
        userId: String,
        displayName: String,
        email: String
    ): Result<Unit> {
        val userDocPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            val existingProfile = userSnap.toObject(
                ShopUserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )

            if (existingProfile != null && existingProfile.seeded) {
                return Result.success(Unit)
            }

            val batch = db.batch()
            if (existingProfile == null) {
                val profile = ShopUserProfile(
                    userId = uid,
                    displayName = displayName.ifBlank { "Sanu Shopper" },
                    email = email,
                    agentMode = false,
                    agentBadgeId = "",
                    storeBranch = "Sanu Flagship · Downtown",
                    seeded = true
                )
                batch.set(userRef, profile.toCreateMap())
            } else {
                batch.update(
                    userRef,
                    mapOf("seeded" to true, "updatedAt" to FieldValue.serverTimestamp())
                )
            }

            val productsSnap = userRef.collection("products").limit(1).get().await()
            if (productsSnap.isEmpty) {
                defaultCatalog(uid).forEach { (docId, product) ->
                    batch.set(userRef.collection("products").document(docId), product.toCreateMap())
                }
                defaultOrders(uid, displayName.ifBlank { "Sanu Shopper" }).forEach { (docId, order) ->
                    batch.set(userRef.collection("orders").document(docId), order.toCreateMap())
                }
                defaultNotifications(uid).forEach { (docId, notif) ->
                    batch.set(userRef.collection("notifications").document(docId), notif.toCreateMap())
                }
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDocPath)
            Result.failure(e)
        }
    }

    suspend fun setAgentMode(
        userId: String,
        enabled: Boolean,
        badgeId: String,
        branch: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val cleanBadge = if (enabled) badgeId.trim().take(32) else ""
            val cleanBranch = branch.trim().take(60).ifBlank { "Sanu Flagship · Downtown" }
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "agentMode" to enabled,
                        "agentBadgeId" to cleanBadge,
                        "storeBranch" to cleanBranch,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordLowStockNotification(
        userId: String,
        product: Product,
        remainingStock: Long
    ): Result<Unit> {
        val path = "users/$userId/notifications"
        return try {
            val uid = requireUserId(userId)
            createNotificationInternal(
                uid = uid,
                title = "Low Stock Alert: ${product.sku}",
                message = "${product.name} at ${product.warehouseBin} has $remainingStock pairs remaining.",
                notifType = "INVENTORY",
                referenceId = product.id
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun placeOrder(
        userId: String,
        customerName: String,
        shippingAddress: String,
        cartItems: List<CartItem>,
        paymentMethod: PaymentMethod,
        paymentReference: String,
        assignedAgent: String
    ): Result<ShopOrder> {
        val ordersPath = "users/$userId/orders"
        return try {
            val uid = requireUserId(userId)
            require(cartItems.isNotEmpty()) { "Cart cannot be empty" }
            require(shippingAddress.trim().length >= 4) { "Enter a valid delivery address" }

            val subtotal = cartItems.sumOf { it.lineTotal }
            val tax = (subtotal * 0.08).let { Math.round(it * 100.0) / 100.0 }
            val total = (subtotal + tax).let { Math.round(it * 100.0) / 100.0 }
            val profit = cartItems.sumOf { it.lineProfit }.let { Math.round(it * 100.0) / 100.0 }
            val totalUnits = cartItems.sumOf { it.quantity }.toLong().coerceIn(1L, 1000L)

            val categories = cartItems.map { it.product.categoryEnum }.distinct()
            val primaryCategory = when {
                categories.size > 1 -> "MIXED"
                categories.firstOrNull() == ProductCategory.SLIPPERS -> "SLIPPERS"
                else -> "SHOES"
            }

            val summary = cartItems.joinToString(", ") { item ->
                "${item.product.name} (${item.selectedSize}) x${item.quantity}"
            }.take(300)

            val orderNumber = "ORD-${(1000..9999).random()}-${(10..99).random()}"
            val paymentStatus = if (paymentMethod == PaymentMethod.COD) "COD_PENDING" else "PAID"

            val order = ShopOrder(
                userId = uid,
                orderNumber = orderNumber,
                customerName = customerName.trim().take(80).ifBlank { "Sanu Customer" },
                shippingAddress = shippingAddress.trim().take(200),
                itemsSummary = summary,
                primaryCategory = primaryCategory,
                totalUnits = totalUnits,
                subtotal = subtotal,
                tax = tax,
                totalAmount = total,
                estimatedProfit = profit,
                paymentMethod = paymentMethod.wireValue,
                paymentReference = paymentReference.take(64),
                paymentStatus = paymentStatus,
                trackingStatus = TrackingStatus.PLACED.wireValue,
                trackingNote = TrackingStatus.PLACED.defaultNote,
                assignedAgent = assignedAgent.ifBlank { "AGT-104" },
                etaText = TrackingStatus.PLACED.defaultEta
            )

            val userRef = db.collection("users").document(uid)
            val orderRef = userRef.collection("orders").document()
            val batch = db.batch()
            batch.set(orderRef, order.toCreateMap())

            // Aggregate quantities per productId so multiple sizes of same SKU update cleanly
            val qtyByProductId = cartItems.groupBy { it.product.id }
                .mapValues { entry -> entry.value.sumOf { it.quantity }.toLong() }

            val lowStockAlerts = mutableListOf<Product>()
            qtyByProductId.forEach { (productId, qtyBought) ->
                val prod = cartItems.first { it.product.id == productId }.product
                val nextStock = (prod.stockCount - qtyBought).coerceAtLeast(0L)
                val nextSold = prod.soldCount + qtyBought
                batch.update(
                    userRef.collection("products").document(productId),
                    mapOf(
                        "stockCount" to nextStock,
                        "soldCount" to nextSold,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (nextStock <= prod.lowStockThreshold) {
                    lowStockAlerts.add(prod.copy(stockCount = nextStock))
                }
            }

            val orderNotifRef = userRef.collection("notifications").document()
            val orderNotif = ShopNotification(
                userId = uid,
                title = "Order Confirmed · $orderNumber",
                message = "${paymentMethod.displayName} ($paymentReference) verified for $summary.",
                notifType = "ORDER",
                readFlag = false,
                referenceId = orderRef.id
            )
            batch.set(orderNotifRef, orderNotif.toCreateMap())

            lowStockAlerts.forEach { lowProd ->
                val alertRef = userRef.collection("notifications").document()
                val alert = ShopNotification(
                    userId = uid,
                    title = "Warehouse Low Stock · ${lowProd.sku}",
                    message = "${lowProd.name} (${lowProd.warehouseBin}) down to ${lowProd.stockCount} pairs.",
                    notifType = "INVENTORY",
                    readFlag = false,
                    referenceId = lowProd.id
                )
                batch.set(alertRef, alert.toCreateMap())
            }

            batch.commit().await()
            Result.success(order.copy(id = orderRef.id))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ordersPath)
            Result.failure(e)
        }
    }

    suspend fun advanceOrderTracking(
        userId: String,
        order: ShopOrder,
        agentBadge: String
    ): Result<TrackingStatus> {
        val path = "users/$userId/orders/${order.id}"
        return try {
            val uid = requireUserId(userId)
            val current = order.trackingStatusEnum
            val next = current.nextStatus()
                ?: return Result.failure(IllegalStateException("Order already delivered"))

            val paymentStatus = if (next == TrackingStatus.DELIVERED && order.paymentStatus == "COD_PENDING") {
                "PAID"
            } else {
                order.paymentStatus
            }

            db.collection("users").document(uid)
                .collection("orders").document(order.id)
                .update(
                    mapOf(
                        "trackingStatus" to next.wireValue,
                        "trackingNote" to next.defaultNote,
                        "assignedAgent" to agentBadge.ifBlank { order.assignedAgent.ifBlank { "AGT-104" } },
                        "etaText" to next.defaultEta,
                        "paymentStatus" to paymentStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()

            createNotificationInternal(
                uid = uid,
                title = "${order.orderNumber} · ${next.title}",
                message = "${next.defaultNote} (${next.defaultEta})",
                notifType = "ORDER",
                referenceId = order.id
            )
            Result.success(next)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> {
        val path = "users/$userId/notifications/$notificationId"
        return try {
            val uid = requireUserId(userId)
            db.collection("users").document(uid)
                .collection("notifications").document(notificationId)
                .update(
                    mapOf(
                        "readFlag" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(userId: String, notifications: List<ShopNotification>): Result<Unit> {
        val path = "users/$userId/notifications"
        return try {
            val uid = requireUserId(userId)
            val unread = notifications.filter { !it.readFlag && it.id.isNotBlank() }
            if (unread.isEmpty()) return Result.success(Unit)
            val batch = db.batch()
            unread.forEach { notif ->
                val ref = db.collection("users").document(uid).collection("notifications").document(notif.id)
                batch.update(ref, mapOf("readFlag" to true, "updatedAt" to FieldValue.serverTimestamp()))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private suspend fun createNotificationInternal(
        uid: String,
        title: String,
        message: String,
        notifType: String,
        referenceId: String
    ) {
        val notif = ShopNotification(
            userId = uid,
            title = title,
            message = message,
            notifType = notifType,
            readFlag = false,
            referenceId = referenceId
        )
        db.collection("users").document(uid)
            .collection("notifications").document()
            .set(notif.toCreateMap())
            .await()
    }

    private fun defaultCatalog(uid: String): List<Pair<String, Product>> = listOf(
        "sku_sh_001" to Product(
            userId = uid,
            sku = "SNU-SH-001",
            name = "Aether Court Sneaker",
            brand = "Sanu Atelier",
            category = ProductCategory.SHOES.wireValue,
            subcategory = "Sneakers",
            description = "Full-grain cream calfskin sneaker with terracotta suede overlays and cushioned OrthoStep insole.",
            price = 128.0,
            costPrice = 54.0,
            stockCount = 24L,
            lowStockThreshold = 6L,
            warehouseBin = "AISLE-A · BIN-04",
            qrPayload = "SANU:SNU-SH-001",
            imageKey = "sneaker",
            sizes = listOf("US 7", "US 8", "US 9", "US 10", "US 11", "US 12"),
            colorway = "Alabaster / Terracotta",
            rating = 4.9,
            soldCount = 68L,
            featured = true
        ),
        "sku_sh_002" to Product(
            userId = uid,
            sku = "SNU-SH-002",
            name = "Solstice Penny Loafer",
            brand = "Sanu Heritage",
            category = ProductCategory.SHOES.wireValue,
            subcategory = "Loafers",
            description = "Hand-stitched cognac chestnut leather loafer built with Goodyear-welted flex outsole.",
            price = 158.0,
            costPrice = 68.0,
            stockCount = 14L,
            lowStockThreshold = 5L,
            warehouseBin = "AISLE-A · BIN-11",
            qrPayload = "SANU:SNU-SH-002",
            imageKey = "loafer",
            sizes = listOf("US 7", "US 8", "US 9", "US 10", "US 11"),
            colorway = "Cognac Chestnut",
            rating = 4.8,
            soldCount = 41L,
            featured = true
        ),
        "sku_sh_003" to Product(
            userId = uid,
            sku = "SNU-SH-003",
            name = "Terra Pace Runner",
            brand = "Sanu Motion",
            category = ProductCategory.SHOES.wireValue,
            subcategory = "Running",
            description = "Breathable knit upper paired with nitrogen-infused midsole foam for all-day urban miles.",
            price = 114.0,
            costPrice = 46.0,
            stockCount = 5L,
            lowStockThreshold = 6L,
            warehouseBin = "AISLE-B · BIN-02",
            qrPayload = "SANU:SNU-SH-003",
            imageKey = "sneaker",
            sizes = listOf("US 6", "US 7", "US 8", "US 9", "US 10"),
            colorway = "Oatmeal / Espresso",
            rating = 4.7,
            soldCount = 53L,
            featured = false
        ),
        "sku_sh_004" to Product(
            userId = uid,
            sku = "SNU-SH-004",
            name = "Sienna Chelsea Boot",
            brand = "Sanu Heritage",
            category = ProductCategory.SHOES.wireValue,
            subcategory = "Boots",
            description = "Weather-treated full leather ankle boot with elastic gore panels and Vibram-grade traction.",
            price = 185.0,
            costPrice = 82.0,
            stockCount = 11L,
            lowStockThreshold = 4L,
            warehouseBin = "AISLE-B · BIN-09",
            qrPayload = "SANU:SNU-SH-004",
            imageKey = "loafer",
            sizes = listOf("US 8", "US 9", "US 10", "US 11", "US 12"),
            colorway = "Burnished Saddle",
            rating = 4.9,
            soldCount = 29L,
            featured = false
        ),
        "sku_sl_001" to Product(
            userId = uid,
            sku = "SNU-SL-001",
            name = "Dune Suede Cork Slide",
            brand = "Sanu Cloud",
            category = ProductCategory.SLIPPERS.wireValue,
            subcategory = "Cork Slides",
            description = "Contoured natural cork footbed wrapped in velvety sand suede for ergonomic indoor-outdoor support.",
            price = 64.0,
            costPrice = 22.0,
            stockCount = 32L,
            lowStockThreshold = 8L,
            warehouseBin = "AISLE-C · BIN-01",
            qrPayload = "SANU:SNU-SL-001",
            imageKey = "slipper",
            sizes = listOf("US 6", "US 7", "US 8", "US 9", "US 10", "US 11"),
            colorway = "Warm Sand",
            rating = 4.9,
            soldCount = 94L,
            featured = true
        ),
        "sku_sl_002" to Product(
            userId = uid,
            sku = "SNU-SL-002",
            name = "Nimbus House Mule",
            brand = "Sanu Cloud",
            category = ProductCategory.SLIPPERS.wireValue,
            subcategory = "House Slippers",
            description = "Shearling-lined closed-toe suede slipper mule with non-slip indoor/patio rubber sole.",
            price = 72.0,
            costPrice = 26.0,
            stockCount = 4L,
            lowStockThreshold = 6L,
            warehouseBin = "AISLE-C · BIN-06",
            qrPayload = "SANU:SNU-SL-002",
            imageKey = "slipper",
            sizes = listOf("US 6", "US 7", "US 8", "US 9", "US 10"),
            colorway = "Oat Suede / Wool",
            rating = 4.8,
            soldCount = 77L,
            featured = true
        ),
        "sku_sl_003" to Product(
            userId = uid,
            sku = "SNU-SL-003",
            name = "Kora Recovery Sandal",
            brand = "Sanu Motion",
            category = ProductCategory.SLIPPERS.wireValue,
            subcategory = "Recovery Sandals",
            description = "Dual-density EVA recovery slipper engineered to absorb heel impact after runs and workouts.",
            price = 52.0,
            costPrice = 17.0,
            stockCount = 27L,
            lowStockThreshold = 8L,
            warehouseBin = "AISLE-D · BIN-03",
            qrPayload = "SANU:SNU-SL-003",
            imageKey = "slipper",
            sizes = listOf("US 7", "US 8", "US 9", "US 10", "US 11", "US 12"),
            colorway = "Travertine Beige",
            rating = 4.7,
            soldCount = 61L,
            featured = false
        ),
        "sku_sl_004" to Product(
            userId = uid,
            sku = "SNU-SL-004",
            name = "Atelier Linen Slipper",
            brand = "Sanu Atelier",
            category = ProductCategory.SLIPPERS.wireValue,
            subcategory = "House Slippers",
            description = "Breathable Belgian linen strap slide with memory foam arch pillow and whisper-quiet sole.",
            price = 58.0,
            costPrice = 20.0,
            stockCount = 19L,
            lowStockThreshold = 6L,
            warehouseBin = "AISLE-D · BIN-08",
            qrPayload = "SANU:SNU-SL-004",
            imageKey = "slipper",
            sizes = listOf("US 6", "US 7", "US 8", "US 9", "US 10"),
            colorway = "Flax / Clay",
            rating = 4.8,
            soldCount = 46L,
            featured = false
        )
    )

    private fun defaultOrders(uid: String, customerName: String): List<Pair<String, ShopOrder>> = listOf(
        "ord_seed_1" to ShopOrder(
            userId = uid,
            orderNumber = "ORD-2026-4108",
            customerName = customerName,
            shippingAddress = "240 Market St, Apt 8A, San Francisco",
            itemsSummary = "Aether Court Sneaker (US 9) x1, Dune Suede Cork Slide (US 9) x1",
            primaryCategory = "MIXED",
            totalUnits = 2L,
            subtotal = 192.0,
            tax = 15.36,
            totalAmount = 207.36,
            estimatedProfit = 116.0,
            paymentMethod = PaymentMethod.CARD.wireValue,
            paymentReference = "PAY-VISA-4242-4108",
            paymentStatus = "PAID",
            trackingStatus = TrackingStatus.IN_TRANSIT.wireValue,
            trackingNote = TrackingStatus.IN_TRANSIT.defaultNote,
            assignedAgent = "AGT-104",
            etaText = TrackingStatus.IN_TRANSIT.defaultEta
        ),
        "ord_seed_2" to ShopOrder(
            userId = uid,
            orderNumber = "ORD-2026-4092",
            customerName = customerName,
            shippingAddress = "18 Cedar Lane, Austin",
            itemsSummary = "Solstice Penny Loafer (US 10) x1",
            primaryCategory = "SHOES",
            totalUnits = 1L,
            subtotal = 158.0,
            tax = 12.64,
            totalAmount = 170.64,
            estimatedProfit = 90.0,
            paymentMethod = PaymentMethod.UPI_WALLET.wireValue,
            paymentReference = "PAY-UPI-4092",
            paymentStatus = "PAID",
            trackingStatus = TrackingStatus.PACKED.wireValue,
            trackingNote = TrackingStatus.PACKED.defaultNote,
            assignedAgent = "AGT-104",
            etaText = TrackingStatus.PACKED.defaultEta
        ),
        "ord_seed_3" to ShopOrder(
            userId = uid,
            orderNumber = "ORD-2026-3980",
            customerName = customerName,
            shippingAddress = "905 Pine Ave, Seattle",
            itemsSummary = "Nimbus House Mule (US 8) x2",
            primaryCategory = "SLIPPERS",
            totalUnits = 2L,
            subtotal = 144.0,
            tax = 11.52,
            totalAmount = 155.52,
            estimatedProfit = 92.0,
            paymentMethod = PaymentMethod.CARD.wireValue,
            paymentReference = "PAY-MASTERCARD-8812-3980",
            paymentStatus = "PAID",
            trackingStatus = TrackingStatus.DELIVERED.wireValue,
            trackingNote = TrackingStatus.DELIVERED.defaultNote,
            assignedAgent = "AGT-102",
            etaText = TrackingStatus.DELIVERED.defaultEta
        )
    )

    private fun defaultNotifications(uid: String): List<Pair<String, ShopNotification>> = listOf(
        "notif_seed_1" to ShopNotification(
            userId = uid,
            title = "ORD-2026-4108 · In Transit",
            message = "Your Aether Court Sneaker & Dune Cork Slide parcel left Sanu Central Hub.",
            notifType = "ORDER",
            readFlag = false,
            referenceId = "ord_seed_1"
        ),
        "notif_seed_2" to ShopNotification(
            userId = uid,
            title = "Warehouse Low Stock · SNU-SL-002",
            message = "Nimbus House Mule (AISLE-C · BIN-06) has 4 pairs left in stock.",
            notifType = "INVENTORY",
            readFlag = false,
            referenceId = "sku_sl_002"
        )
    )
}
