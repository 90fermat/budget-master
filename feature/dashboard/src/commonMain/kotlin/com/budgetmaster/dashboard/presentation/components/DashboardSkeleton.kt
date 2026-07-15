package com.budgetmaster.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Full-page loading skeleton for the Dashboard screen.
 *
 * Matches the exact vertical rhythm, spacing, and component heights of the live dashboard.
 * Every placeholder uses the animated [Brush.shimmerBrush] extension from [AiInsightsWidget].
 */
@Composable
fun DashboardSkeleton(
    modifier: Modifier = Modifier
) {
    val shimmer = androidx.compose.ui.graphics.Brush.shimmerBrush()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // ── Header skeleton ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(shimmer)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmer)
                    )
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmer)
                    )
                }
            }
            // Notification bell
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(shimmer)
            )
        }

        // ── Balance card skeleton ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(shimmer)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Quick actions skeleton (3 pills) ─────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(shimmer)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Section label skeleton ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(shimmer)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Chart card skeleton ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(shimmer)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Section label skeleton ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(shimmer)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Budget progress skeleton (3 items) ───────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(shimmer)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Section label skeleton ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(shimmer)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Transaction row skeletons ────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                SkeletonTransactionRow(shimmer = shimmer)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── AI Insights label skeleton ─────────────────────────────────────
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(shimmer)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Insight card skeletons ─────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(shimmer)
                )
            }
        }
    }
}

/**
 * A single shimmer placeholder row mimicking a real transaction item.
 *
 * @param shimmer The animated shimmer [Brush] to paint the placeholder.
 */
@Composable
private fun SkeletonTransactionRow(shimmer: androidx.compose.ui.graphics.Brush) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(shimmer)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row {
            // Category circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                )
            }
        }
        // Amount
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        )
    }
}

@PreviewLightDark
@Composable
fun DashboardSkeletonPreview() {
    MaterialTheme {
        DashboardSkeleton()
    }
}
