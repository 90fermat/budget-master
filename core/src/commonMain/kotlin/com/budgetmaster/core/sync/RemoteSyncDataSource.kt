package com.budgetmaster.core.sync

/**
 * One row as it travels to and from a remote.
 *
 * The payload is the row serialised as JSON, reusing the `backup` models so a row has exactly one
 * wire shape in this codebase rather than two that can drift apart.
 *
 * @property updatedAt when the row was last edited, by whichever device edited it. This is the
 *   value last-write-wins compares, so it travels with the row rather than being re-stamped on
 *   arrival — a row that arrives and is immediately restamped would always look newest and would
 *   overwrite better data.
 * @property deviceId which device wrote it, used only to break an exact timestamp tie so that two
 *   devices resolve a collision the same way instead of ping-ponging.
 */
data class RemoteRecord(
    val tableName: String,
    val rowId: String,
    val updatedAt: Long,
    val deviceId: String,
    val payload: String,
)

/**
 * A row that was removed, so other devices stop holding it.
 *
 * Carries a [deviceId] for the same reason a record does. A delete and an edit landing in the same
 * clock tick is not exotic — the timestamps have second resolution — and both devices must break
 * that tie the same way, or one keeps the row while the other drops it and neither can tell.
 */
data class RemoteTombstone(
    val tableName: String,
    val rowId: String,
    val deletedAt: Long,
    val deviceId: String,
)

/**
 * Something the remote is handing back, tagged with **its own** arrival order.
 *
 * [seq] is not the row's edit time and the two must not be conflated — that conflation is a real
 * data-loss bug, not a tidiness point. A device that has pulled up to edit-time T will never ask
 * for anything older than T again; but another device can perfectly well publish a row it edited
 * *before* T, having been offline in between. Filtering on edit time drops that row permanently and
 * the two devices disagree forever, with nothing to indicate it.
 *
 * So the remote assigns a monotonically increasing [seq] as it accepts each write, and cursors
 * advance on that. Edit time keeps its own job — deciding which of two versions wins.
 *
 * Firestore supplies this directly: write a server timestamp field on each document and order by
 * it. It must be the server's, not the client's, for the same reason.
 */
data class RemoteChange<T>(val value: T, val seq: Long)

/**
 * The remote half of sync, kept behind an interface for a reason that matters more than tidiness:
 * the entire algorithm — ordering, conflict resolution, convergence — can then be proven on the
 * host against an in-memory fake, with no Firebase, no network and no device. The real
 * implementation becomes a thin adapter that cannot change the algorithm's behaviour.
 */
interface RemoteSyncDataSource {
    /**
     * Records the remote accepted after [sinceSeq], oldest first.
     *
     * Ordered and filtered by the remote's own sequence, never by the records' edit times.
     */
    suspend fun pull(sinceSeq: Long): List<RemoteChange<RemoteRecord>>

    /** Removals the remote accepted after [sinceSeq], oldest first. */
    suspend fun pullTombstones(sinceSeq: Long): List<RemoteChange<RemoteTombstone>>

    /**
     * Whether the account holds anything at all, for the sign-in decision.
     *
     * Its own method rather than `pull(0).isNotEmpty()`, because that reads every document in the
     * account to answer one boolean — on every sign-in, and billed per document read. An
     * implementation should ask for a single row and stop.
     */
    suspend fun hasAnyRecords(): Boolean

    /**
     * Publishes local changes, assigning each a sequence.
     *
     * Two obligations on implementations, both load-bearing:
     *
     * - **Idempotent per (tableName, rowId).** A push that succeeded remotely but whose
     *   acknowledgement was lost is retried on the next pass.
     * - **A tombstone removes the row's record, but only if it is at least as new as that record.**
     *   The first half matters because a deleted row must stop being pullable as a live one:
     *   otherwise a device whose cursor still sits behind that record pulls it and inserts the row
     *   straight back, a delete that quietly undoes itself on one device only. The second half
     *   matters just as much, and is easier to miss — deletes arrive late. A device that has been
     *   offline publishes a removal from an hour ago, and if that evicts a row someone has since
     *   re-created, the re-creation is lost on every device except the one that made it. The rule
     *   is the same one the engine applies locally: newer wins.
     *   On Firestore this is a transaction that reads the document's `updatedAt` before deleting.
     */
    suspend fun push(records: List<RemoteRecord>, tombstones: List<RemoteTombstone>)
}
