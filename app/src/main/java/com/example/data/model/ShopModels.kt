package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class ProductCategory(val wireValue: String, val label: String) {
    SHOES("SHOES", "Shoes"),
    SLIPPERS("SLIPPERS", "Slippers");

    companion object {
        fun fromWire(raw: String): ProductCategory =
            entries.firstOrNull { it.wireValue == raw } ?: SHOES
    }
}

enum class TrackingStatus(
    val wireValue: String,
    val title: String,
    val defaultNote: String,
    val defaultEta: String,
    val stepIndex: Int
) {
    PLACED(
        wireValue = "PLACED",
        title = "Order Placed",
        defaultNote = "Payment confirmed & order queued for warehouse picking.",
        defaultEta = "Est. 2 Days",
        stepIndex = 0
    ),
    PACKED(
        wireValue = "PACKED",
        title = "Packed in Warehouse",
        defaultNote = "Footwear inspected, QR-verified, and sealed at Sanu Hub.",
        defaultEta = "Est. Tomorrow, 6:00 PM",
        stepIndex = 1
    ),
    IN_TRANSIT(
        wireValue = "IN_TRANSIT",
        title = "In Transit",
        defaultNote = "Dispatched with Sanu Express Courier toward local hub.",
        defaultEta = "Est. Tomorrow, 2:30 PM",
        stepIndex = 2
    ),
    OUT_FOR_DELIVERY(
        wireValue = "OUT_FOR_DELIVERY",
        title = "Out for Delivery",
        defaultNote = "Courier is in your neighborhood with your parcel.",
        defaultEta = "Arriving Today, 45 mins",
        stepIndex = 3
    ),
    DELIVERED(
        wireValue = "DELIVERED",
        title = "Delivered",
        defaultNote = "Package handed over and signed off.",
        defaultEta = "Delivered",
        stepIndex = 4
    );

    fun nextStatus(): TrackingStatus? = when (this) {
        PLACED -> PACKED
        PACKED -> IN_TRANSIT
        IN_TRANSIT -> OUT_FOR_DELIVERY
        OUT_FOR_DELIVERY -> DELIVERED
        DELIVERED -> null
    }

    companion object {
        fun fromWire(raw: String): TrackingStatus =
            entries.firstOrNull { it.wireValue == raw } ?: PLACED
    }
}

enum class PaymentMethod(val wireValue: String, val displayName: String) {
    CARD("CARD", "Credit / Debit Card"),
    UPI_WALLET("UPI_WALLET", "Digital Wallet / UPI"),
    COD("COD", "Cash on Delivery");

    companion object {
        fun fromWire(raw: String): PaymentMethod =
            entries.firstOrNull { it.wireValue == raw } ?: CARD
    }
}

@Keep
@IgnoreExtraProperties
data class ShopUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val agentMode: Boolean = false,
    val agentBadgeId: String = "",
    val storeBranch: String = "Sanu Flagship Store",
    val seeded: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.take(80).ifBlank { "Sanu Shopper" },
        "email" to email.take(120),
        "agentMode" to agentMode,
        "agentBadgeId" to agentBadgeId.take(32),
        "storeBranch" to storeBranch.take(60).ifBlank { "Sanu Flagship Store" },
        "seeded" to seeded,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}


@Keep
@IgnoreExtraProperties
data class ShopOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val customerName: String = "",
    val shippingAddress: String = "",
    val itemsSummary: String = "",
    val primaryCategory: String = "SHOES",
    val totalUnits: Long = 1L,
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val totalAmount: Double = 0.0,
    val estimatedProfit: Double = 0.0,
    val paymentMethod: String = PaymentMethod.CARD.wireValue,
    val paymentReference: String = "",
    val paymentStatus: String = "PAID",
    val trackingStatus: String = TrackingStatus.PLACED.wireValue,
    val trackingNote: String = "",
    val assignedAgent: String = "",
    val etaText: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val trackingStatusEnum: TrackingStatus
        get() = TrackingStatus.fromWire(trackingStatus)

    val paymentMethodEnum: PaymentMethod
        get() = PaymentMethod.fromWire(paymentMethod)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "orderNumber" to orderNumber.take(32),
        "customerName" to customerName.take(80),
        "shippingAddress" to shippingAddress.take(200),
        "itemsSummary" to itemsSummary.take(300),
        "primaryCategory" to primaryCategory,
        "totalUnits" to totalUnits.coerceIn(1L, 1000L),
        "subtotal" to subtotal.coerceAtLeast(0.0),
        "tax" to tax.coerceAtLeast(0.0),
        "totalAmount" to totalAmount.coerceAtLeast(0.01),
        "estimatedProfit" to estimatedProfit.coerceAtLeast(0.0),
        "paymentMethod" to paymentMethodEnum.wireValue,
        "paymentReference" to paymentReference.take(64),
        "paymentStatus" to paymentStatus,
        "trackingStatus" to trackingStatusEnum.wireValue,
        "trackingNote" to trackingNote.take(200),
        "assignedAgent" to assignedAgent.take(64),
        "etaText" to etaText.take(60),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ShopNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val notifType: String = "ORDER",
    val readFlag: Boolean = false,
    val referenceId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title.take(100),
        "message" to message.take(300),
        "notifType" to notifType,
        "readFlag" to readFlag,
        "referenceId" to referenceId.take(64),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

data class CartItem(
    val product: FootwearProduct,
    val selectedSize: String,
    val quantity: Int = 1
) {
    val lineTotal: Double get() = product.price * quantity
    val lineProfit: Double get() = (product.price - product.costPrice).coerceAtLeast(0.0) * quantity
}

data class DailyRevenuePoint(
    val label: String,
    val shoesRevenue: Double,
    val slippersRevenue: Double
)

data class AnalyticsSummary(
    val totalRevenue: Double = 0.0,
    val totalProfit: Double = 0.0,
    val totalOrders: Int = 0,
    val totalUnitsSold: Long = 0L,
    val averageOrderValue: Double = 0.0,
    val shoesRevenue: Double = 0.0,
    val slippersRevenue: Double = 0.0,
    val lowStockCount: Int = 0,
    val totalInventoryUnits: Long = 0L,
    val topProducts: List<FootwearProduct> = emptyList(),
    val dailySeries: List<DailyRevenuePoint> = emptyList()
)
