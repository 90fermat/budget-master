package com.budgetmaster.core.sync

/** Android and iOS both talk to Firestore through the same binding. */
actual fun createRemoteSyncDataSource(uid: String): RemoteSyncDataSource? = FirestoreSyncDataSource(uid)
