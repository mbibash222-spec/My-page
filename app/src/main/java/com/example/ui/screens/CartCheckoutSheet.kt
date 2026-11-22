package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.ShopOrder
import com.example.ui.components.resolveProductDrawable
import com.example.util.PaymentUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartCheckoutBottomSheet(
    cartItems: List<CartItem>,
    defaultCustomerName: String,
    isProcessing: Boolean,
    lastPlacedOrder: ShopOrder?,
    onUpdateQuantity: (productId: String, size: String, delta: Int) -> Unit,
    onCheckout: (
        customerName: String,
        shippingAddress: String,
        paymentMethod: PaymentMethod,
        cardOrWallet: String
    ) -> Unit,
    onViewTracking: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var customerName by remember(defaultCustomerName) { mutableStateOf(defaultCustomerName) }
    var shippingAddress by remember { mutableStateOf("240 Market St, Suite 400, San Francisco") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CARD) }
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("sanu.shopper@okaxis") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val subtotal = cartItems.sumOf { it.lineTotal }
    val tax = Math.round(subtotal * 0.08 * 100.0) / 100.0
    val total = subtotal + tax

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (lastPlacedOrder != null && cartItems.isEmpty()) {
                OrderReceiptView(
                    order = lastPlacedOrder,
                    onTrackOrder = onViewTracking,
                    onDismiss = onDismiss
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bag & Secure Payment",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "TLS 1.3",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                if (cartItems.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingBag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Your shopping bag is empty", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Add shoes or slippers from the boutique to check out.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    cartItems.forEach { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(
                                        id = resolveProductDrawable(item.product.imageKey, item.product.categoryEnum)
                                    ),
                                    contentDescription = item.product.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.product.name,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${item.selectedSize} · ${item.product.sku}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = PaymentUtils.formatCurrency(item.lineTotal),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onUpdateQuantity(item.product.id, item.selectedSize, -1) }
                                    ) {
                                        Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                                    }
                                    Text(
                                        text = "${item.quantity}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { onUpdateQuantity(item.product.id, item.selectedSize, 1) }
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                                    }
                                }
                            }
                        }
                    }

                    Text("Delivery Details", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Recipient Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = shippingAddress,
                        onValueChange = { shippingAddress = it },
                        label = { Text("Shipping Address") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Secure Payment Gateway", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMethod == PaymentMethod.CARD,
                            onClick = {
                                selectedMethod = PaymentMethod.CARD
                                validationError = null
                            },
                            label = { Text("Card") },
                            leadingIcon = {
                                Icon(Icons.Filled.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMethod == PaymentMethod.UPI_WALLET,
                            onClick = {
                                selectedMethod = PaymentMethod.UPI_WALLET
                                validationError = null
                            },
                            label = { Text("UPI / Wallet") },
                            leadingIcon = {
                                Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMethod == PaymentMethod.COD,
                            onClick = {
                                selectedMethod = PaymentMethod.COD
                                validationError = null
                            },
                            label = { Text("COD") },
                            leadingIcon = {
                                Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    when (selectedMethod) {
                        PaymentMethod.CARD -> {
                            val detectedBrand = PaymentUtils.detectCardBrand(cardNumber)
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Tokenized Card (${detectedBrand.label})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = {
                                            cardNumber = "4242424242424242"
                                            cardExpiry = "0828"
                                            cardCvv = "424"
                                            validationError = null
                                        },
                                        modifier = Modifier.testTag("fill_demo_card_button")
                                    ) {
                                        Text("Fill Test Visa")
                                    }
                                }
                                OutlinedTextField(
                                    value = PaymentUtils.formatCardDisplay(cardNumber),
                                    onValueChange = { input ->
                                        cardNumber = input.filter { it.isDigit() }.take(16)
                                    },
                                    label = { Text("Card Number") },
                                    placeholder = { Text("4242 4242 4242 4242") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("card_number_input")
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = cardExpiry,
                                        onValueChange = { cardExpiry = it.filter { c -> c.isDigit() }.take(4) },
                                        label = { Text("Expiry (MMYY)") },
                                        placeholder = { Text("0828") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = cardCvv,
                                        onValueChange = { cardCvv = it.filter { c -> c.isDigit() }.take(4) },
                                        label = { Text("CVV") },
                                        placeholder = { Text("424") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        PaymentMethod.UPI_WALLET -> {
                            OutlinedTextField(
                                value = upiId,
                                onValueChange = { upiId = it },
                                label = { Text("UPI / Digital Wallet ID") },
                                placeholder = { Text("name@bank") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        PaymentMethod.COD -> {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Pay securely via cash or mobile tap when the Sanu Express courier arrives.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    if (validationError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = validationError!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SummaryLine("Subtotal", PaymentUtils.formatCurrency(subtotal))
                        SummaryLine("Estimated Tax (8%)", PaymentUtils.formatCurrency(tax))
                        SummaryLine("Express Courier", "FREE")
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Due", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = PaymentUtils.formatCurrency(total),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val addr = shippingAddress.trim()
                            if (addr.length < 4) {
                                validationError = "Please enter a full delivery address."
                                return@Button
                            }
                            when (selectedMethod) {
                                PaymentMethod.CARD -> {
                                    if (!PaymentUtils.isValidLuhn(cardNumber)) {
                                        validationError = "Enter a valid card number (or tap Fill Test Visa)."
                                        return@Button
                                    }
                                    if (!PaymentUtils.isValidExpiry(cardExpiry)) {
                                        validationError = "Enter valid expiry in MMYY format (e.g. 0828)."
                                        return@Button
                                    }
                                    if (!PaymentUtils.isValidCvv(cardCvv)) {
                                        validationError = "Enter a valid 3 or 4 digit CVV."
                                        return@Button
                                    }
                                }
                                PaymentMethod.UPI_WALLET -> {
                                    if (!PaymentUtils.isValidUpi(upiId)) {
                                        validationError = "Enter a valid UPI / Wallet ID containing '@'."
                                        return@Button
                                    }
                                }
                                PaymentMethod.COD -> Unit
                            }
                            validationError = null
                            val tokenSource = if (selectedMethod == PaymentMethod.CARD) cardNumber else upiId
                            onCheckout(customerName, addr, selectedMethod, tokenSource)
                        },
                        enabled = !isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("pay_and_place_order_button")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Authorizing Payment...")
                        } else {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pay ${PaymentUtils.formatCurrency(total)} & Place Order")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderReceiptView(
    order: ShopOrder,
    onTrackOrder: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(56.dp)
        )
        Text("Payment Authorized!", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Order ${order.orderNumber} is live and syncing with Sanu Express.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SummaryLine("Payment Ref", order.paymentReference)
                SummaryLine("Method", order.paymentMethodEnum.displayName)
                SummaryLine("Status", order.paymentStatus)
                SummaryLine("Total Paid", PaymentUtils.formatCurrency(order.totalAmount))
                SummaryLine("Estimated Delivery", order.etaText)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Continue Shopping")
            }
            Button(
                onClick = onTrackOrder,
                modifier = Modifier
                    .weight(1f)
                    .testTag("receipt_track_order_button")
            ) {
                Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Track Live")
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
