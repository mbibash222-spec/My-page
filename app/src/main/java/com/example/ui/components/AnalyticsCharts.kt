package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyRevenuePoint
import com.example.util.PaymentUtils

@Composable
fun RevenueComparisonBarChart(
    points: List<DailyRevenuePoint>,
    modifier: Modifier = Modifier
) {
    val shoesColor = MaterialTheme.colorScheme.primary
    val slippersColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "7-Day Revenue Trend",
                style = MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot(color = shoesColor, label = "Shoes")
                LegendDot(color = slippersColor, label = "Slippers")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (points.isEmpty()) {
            Text(
                text = "No sales data yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        val maxTotal = points.maxOf { it.shoesRevenue + it.slippersRevenue }.coerceAtLeast(100.0)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            val chartHeight = size.height - 12f
            val slotWidth = size.width / points.size.toFloat()
            val barWidth = (slotWidth * 0.48f).coerceAtMost(44f)

            for (step in 1..3) {
                val y = chartHeight * (step / 4f)
                drawLine(
                    color = gridColor.copy(alpha = 0.6f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2f
                )
            }

            points.forEachIndexed { index, pt ->
                val centerX = slotWidth * index + slotWidth / 2f
                val left = centerX - barWidth / 2f

                val total = pt.shoesRevenue + pt.slippersRevenue
                val totalHeight = ((total / maxTotal).toFloat() * (chartHeight * 0.9f)).coerceAtLeast(8f)
                val shoesHeight = if (total > 0) (pt.shoesRevenue / total).toFloat() * totalHeight else totalHeight / 2f
                val slippersHeight = (totalHeight - shoesHeight).coerceAtLeast(4f)

                val topY = chartHeight - totalHeight

                // Slippers segment (top)
                drawRoundRect(
                    color = slippersColor,
                    topLeft = Offset(left, topY),
                    size = Size(barWidth, slippersHeight + 6f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                // Shoes segment (bottom)
                drawRoundRect(
                    color = shoesColor,
                    topLeft = Offset(left, topY + slippersHeight),
                    size = Size(barWidth, shoesHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CategorySplitDonutChart(
    shoesRevenue: Double,
    slippersRevenue: Double,
    modifier: Modifier = Modifier
) {
    val shoesColor = MaterialTheme.colorScheme.primary
    val slippersColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val total = (shoesRevenue + slippersRevenue).coerceAtLeast(0.01)
    val shoesRatio = (shoesRevenue / total).toFloat().coerceIn(0f, 1f)
    val shoesPct = (shoesRatio * 100).toInt()
    val slippersPct = 100 - shoesPct

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(
            modifier = Modifier.size(124.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(118.dp)) {
                val stroke = Stroke(width = 24f, cap = StrokeCap.Round)
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke
                )
                val shoesSweep = (shoesRatio * 360f).coerceAtLeast(8f)
                val slippersSweep = (360f - shoesSweep).coerceAtLeast(8f)

                drawArc(
                    color = shoesColor,
                    startAngle = -90f,
                    sweepAngle = shoesSweep - 6f,
                    useCenter = false,
                    style = stroke
                )
                drawArc(
                    color = slippersColor,
                    startAngle = -90f + shoesSweep,
                    sweepAngle = slippersSweep - 6f,
                    useCenter = false,
                    style = stroke
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Mix",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$shoesPct/$slippersPct",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CategoryMetricRow(
                color = shoesColor,
                title = "Shoes Sales ($shoesPct%)",
                amount = PaymentUtils.formatCurrency(shoesRevenue)
            )
            CategoryMetricRow(
                color = slippersColor,
                title = "Slippers Sales ($slippersPct%)",
                amount = PaymentUtils.formatCurrency(slippersRevenue)
            )
        }
    }
}

@Composable
private fun CategoryMetricRow(color: Color, title: String, amount: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
