package com.budgetmaster.core.sync

/**
 * No sync on the web.
 *
 * Not an omission: the Wasm build's database lives in memory and is rebuilt on every page load, so
 * there is nothing here that outlives a refresh to reconcile with anything. Syncing from it would
 * push an empty database over data the user actually has.
 */
actual fun createRemoteSyncDataSource(uid: String): RemoteSyncDataSource? = null
