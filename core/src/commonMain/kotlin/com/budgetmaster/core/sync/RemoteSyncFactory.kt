package com.budgetmaster.core.sync

/**
 * The remote for a given signed-in user, or `null` where sync does not apply.
 *
 * Android and iOS return the Firestore binding. Wasm returns `null`, and that is a statement about
 * the platform rather than a gap waiting to be filled: the web build recreates its database on
 * every page load, so there is no durable local state for sync to reconcile — pushing from it would
 * mean uploading a blank slate over the user's real data.
 *
 * Returning `null` rather than throwing lets the caller say "not available here" plainly, which is
 * what the UI needs to show.
 */
expect fun createRemoteSyncDataSource(uid: String): RemoteSyncDataSource?
