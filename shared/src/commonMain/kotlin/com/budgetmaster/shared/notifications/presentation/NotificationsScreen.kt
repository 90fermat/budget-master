@file:OptIn(ExperimentalMaterial3Api::class)

package com.budgetmaster.shared.notifications.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.notifications_back
import budgetmaster.core.generated.resources.notifications_delete
import budgetmaster.core.generated.resources.notifications_empty_subtitle
import budgetmaster.core.generated.resources.notifications_empty_title
import budgetmaster.core.generated.resources.notifications_mark_all_read
import budgetmaster.core.generated.resources.notifications_title
import budgetmaster.core.generated.resources.transactions_today
import budgetmaster.core.generated.resources.transactions_yesterday
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.SurfaceLevel
import com.budgetmaster.core.designsystem.components.AppCard
import com.budgetmaster.core.designsystem.components.EmptyState
import com.budgetmaster.core.notifications.AppNotification
import com.budgetmaster.core.util.DateUtils
import com.budgetmaster.core.util.RelativeDay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The notifications inbox.
 *
 * Reached from the dashboard bell. Unread items sit on a [SurfaceLevel.Raised] card and read
 * items recede to [SurfaceLevel.Flat], so "what is new" is legible at a glance without a separate
 * section. Tapping an item marks it read; the close button removes it.
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = koinViewModel(),
) {
    val notifications by viewModel.notifications.collectAsState()
    NotificationsContent(
        notifications = notifications,
        onBack = onBack,
        onMarkAllRead = viewModel::markAllRead,
        onItemClick = { item -> if (!item.isRead) viewModel.markRead(item.id) },
        onDelete = viewModel::delete,
    )
}

/**
 * Stateless inbox, split from [NotificationsScreen] so it can be rendered from a screenshot test
 * and a preview without a ViewModel or Koin — the same split as `DashboardContent`.
 */
@Composable
fun NotificationsContent(
    notifications: List<AppNotification>,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onItemClick: (AppNotification) -> Unit,
    onDelete: (String) -> Unit,
) {
    val hasUnread = notifications.any { !it.isRead }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.notifications_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.notifications_back),
                        )
                    }
                },
                actions = {
                    if (hasUnread) {
                        IconButton(onClick = onMarkAllRead) {
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = stringResource(Res.string.notifications_mark_all_read),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (notifications.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.NotificationsNone,
                    title = stringResource(Res.string.notifications_empty_title),
                    subtitle = stringResource(Res.string.notifications_empty_subtitle),
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    items(notifications, key = { it.id }) { notification ->
                        NotificationRow(
                            notification = notification,
                            onClick = { onItemClick(notification) },
                            onDelete = { onDelete(notification.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: AppNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    AppCard(
        level = if (notification.isRead) SurfaceLevel.Flat else SurfaceLevel.Raised,
        onClick = onClick,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    // Unread reads bolder; read settles back to normal weight.
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = relativeLabel(notification.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(Res.string.notifications_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Today / Yesterday / the date, reusing the same relative-day logic as the transactions list. */
@Composable
private fun relativeLabel(timestamp: Long): String = when (DateUtils.relativeDay(timestamp)) {
    RelativeDay.TODAY -> stringResource(Res.string.transactions_today)
    RelativeDay.YESTERDAY -> stringResource(Res.string.transactions_yesterday)
    // The absolute date for anything older; the exact instant is not what a reader needs here.
    RelativeDay.OLDER -> DateUtils.isoDate(timestamp)
}
