package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.FootwearProduct
import com.example.data.model.ProductCategory

@DrawableRes
fun resolveProductDrawable(imageKey: String, category: ProductCategory): Int {
    return when {
        imageKey.equals("loafer", ignoreCase = true) -> R.drawable.img_shoe_loafer
        imageKey.equals("slipper", ignoreCase = true) || category == ProductCategory.SLIPPERS ->
            R.drawable.img_slipper_slide
        else -> R.drawable.img_shoe_sneaker
    }
}

@Composable
fun StockStatusPill(
    product: FootwearProduct,
    modifier: Modifier = Modifier
) {
    val (bgColor, fgColor, label, icon) = when {
        product.stockCount <= 0L -> Quadruple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Out of Stock",
            Icons.Filled.Warning
        )
        product.isLowStock -> Quadruple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Low · ${product.stockCount} left",
            Icons.Filled.Warning
        )
        else -> Quadruple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "${product.stockCount} in stock",
            Icons.Filled.CheckCircle
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fgColor
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
