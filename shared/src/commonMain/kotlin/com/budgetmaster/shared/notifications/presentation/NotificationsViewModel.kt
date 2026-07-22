package com.budgetmaster.shared.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmaster.core.notifications.AppNotification
import com.budgetmaster.core.notifications.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the notifications inbox.
 *
 * Lives in `:shared` rather than a feature because the inbox is a cross-cutting surface over the
 * `:core` notification store — budget alerts and import results from different features all land
 * in the one list — and the architecture reserves cross-feature orchestration for the shell.
 *
 * Read state comes straight from the repository's flow; the write actions are fire-and-forget
 * because the list is observed, so a mark-read or delete reflects itself without a manual refresh.
 */
class NotificationsViewModel(
    private val repository: NotificationRepository,
) : ViewModel() {

    val notifications: StateFlow<List<AppNotification>> =
        repository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun markRead(id: String) {
        viewModelScope.launch { repository.markRead(id) }
    }

    fun markAllRead() {
        viewModelScope.launch {
            // Snapshot the current unread ids; the flow updates itself as each write lands.
            notifications.value.filter { !it.isRead }.forEach { repository.markRead(it.id) }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }
}
