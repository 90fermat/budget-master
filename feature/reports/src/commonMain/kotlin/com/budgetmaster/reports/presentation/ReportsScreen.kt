package com.budgetmaster.reports.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.reports_title
import org.jetbrains.compose.resources.stringResource

/**
 * Composable screen representing financial Reports and charts.
 */
@Composable
fun ReportsScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.reports_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Button(onClick = {}) {
                Text("Export")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Timeframe tabs
        TabRow(
            selectedTabIndex = 1,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(selected = false, onClick = {}, text = { Text("Weekly") })
            Tab(selected = true, onClick = {}, text = { Text("Monthly") })
            Tab(selected = false, onClick = {}, text = { Text("Annual") })
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Custom Donut Chart (Canvas)
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                val r = w * 0.4f

                // Food (Amber) - 45% (162 degrees)
                drawArc(
                    color = Color(0xFFF59E0B),
                    startAngle = -90f,
                    sweepAngle = 162f,
                    useCenter = false,
                    style = Stroke(width = 24.dp.toPx()),
                    size = Size(r * 2f, r * 2f),
                    topLeft = Offset(cx - r, cy - r)
                )

                // Housing (Blue) - 35% (126 degrees)
                drawArc(
                    color = Color(0xFF3B82F6),
                    startAngle = 72f,
                    sweepAngle = 126f,
                    useCenter = false,
                    style = Stroke(width = 24.dp.toPx()),
                    size = Size(r * 2f, r * 2f),
                    topLeft = Offset(cx - r, cy - r)
                )

                // Shopping (Pink) - 20% (72 degrees)
                drawArc(
                    color = Color(0xFFEC4899),
                    startAngle = 198f,
                    sweepAngle = 72f,
                    useCenter = false,
                    style = Stroke(width = 24.dp.toPx()),
                    size = Size(r * 2f, r * 2f),
                    topLeft = Offset(cx - r, cy - r)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Text(
                    text = "$2,625",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFeatureSettings = "tnum"
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Breakdown categories list
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BreakdownRow(
                name = "Food & Dining",
                percent = "45%",
                amount = "$1,181.25",
                color = Color(0xFFF59E0B)
            )

            BreakdownRow(
                name = "Housing & Rent",
                percent = "35%",
                amount = "$918.75",
                color = Color(0xFF3B82F6)
            )

            BreakdownRow(
                name = "Shopping",
                percent = "20%",
                amount = "$525.00",
                color = Color(0xFFEC4899)
            )
        }
    }
}

@Composable
private fun BreakdownRow(
    name: String,
    percent: String,
    amount: String,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = percent,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFeatureSettings = "tnum"
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = amount,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFeatureSettings = "tnum"
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
