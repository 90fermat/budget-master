package com.budgetmaster.core.sync

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.Serializable

/**
 * A record as it is stored in Firestore.
 *
 * [seq] is written as [Timestamp.ServerTimestamp] and comes back as a real [Timestamp] — the
 * server's own arrival order, which is what pull cursors advance on. It must be the server's: a
 * client clock would reintroduce exactly the bug the engine was built to avoid, where a device that
 * has been offline publishes changes stamped earlier than the cursor and they are never seen again.
 *
 * [updatedAt] is separate and stays the client's, because it answers a different question — which
 * of two versions of a row wins, not what has been seen.
 */
@Serializable
internal data class FirestoreRecord(
    val tableName: String = "",
    val rowId: String = "",
    val updatedAt: Long = 0,
    val deviceId: String = "",
    val payload: String = "",
    // Non-null on the way out, and that is load-bearing. A nullable field serialises through a
    // different path, and the server-timestamp sentinel has to reach Firestore intact or the write
    // carries an ordinary value — which the security rules reject, because they require `seq` to
    // equal request.time precisely so a client cannot choose its own sequence.
    val seq: BaseTimestamp = Timestamp.ServerTimestamp,
)

/**
 * The same document on the way back, where `seq` may legitimately be absent.
 *
 * Between a local write and the server acknowledging it, the field has no value yet. Reading is
 * therefore the one direction that must tolerate null — and writing is the one that must not.
 */
@Serializable
internal data class FirestoreRecordSnapshot(
    val tableName: String = "",
    val rowId: String = "",
    val updatedAt: Long = 0,
    val deviceId: String = "",
    val payload: String = "",
    val seq: BaseTimestamp? = null,
)

@Serializable
internal data class FirestoreTombstone(
    val tableName: String = "",
    val rowId: String = "",
    val deletedAt: Long = 0,
    val deviceId: String = "",
    val seq: BaseTimestamp = Timestamp.ServerTimestamp,
)

@Serializable
internal data class FirestoreTombstoneSnapshot(
    val tableName: String = "",
    val rowId: String = "",
    val deletedAt: Long = 0,
    val deviceId: String = "",
    val seq: BaseTimestamp? = null,
)

/**
 * The real remote: one Firestore subtree per user.
 *
 * ```
 * users/{uid}/records/{tableName}__{rowId}
 * users/{uid}/tombstones/{tableName}__{rowId}
 * ```
 *
 * Everything lives under the signed-in uid so the security rules are a single ownership check
 * rather than a per-document policy, and one user's data cannot be addressed by another at all.
 *
 * This class is deliberately thin. Every ordering and conflict decision was settled in [SyncEngine]
 * and proven on the host against an in-memory fake; nothing here is allowed to re-decide any of it.
 * The one judgement it does make is the one the interface requires of every remote — a tombstone
 * removes the row's record only when it is not older than that record — and it makes it inside a
 * transaction, because between reading and deleting, another device can write.
 */
class FirestoreSyncDataSource(
    private val uid: String,
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : RemoteSyncDataSource {

    private val records get() = firestore.collection("users").document(uid).collection("records")
    private val tombstones get() = firestore.collection("users").document(uid).collection("tombstones")

    override suspend fun pull(sinceSeq: Long): List<RemoteChange<RemoteRecord>> =
        records.where { SEQ greaterThan sinceSeq.asTimestamp() }
            .orderBy(SEQ)
            .get()
            .documents
            .mapNotNull { snapshot ->
                val record = snapshot.data(FirestoreRecordSnapshot.serializer())
                // A write this client made moments ago is visible locally before the server has
                // assigned its timestamp, so `seq` is briefly null. Skipping it is correct rather
                // than merely safe: it has no position in the order yet, and it will arrive with
                // one on the next pull.
                record.seq.epochNanos()?.let { seq ->
                    RemoteChange(
                        RemoteRecord(record.tableName, record.rowId, record.updatedAt, record.deviceId, record.payload),
                        seq,
                    )
                }
            }

    override suspend fun pullTombstones(sinceSeq: Long): List<RemoteChange<RemoteTombstone>> =
        tombstones.where { SEQ greaterThan sinceSeq.asTimestamp() }
            .orderBy(SEQ)
            .get()
            .documents
            .mapNotNull { snapshot ->
                val tombstone = snapshot.data(FirestoreTombstoneSnapshot.serializer())
                tombstone.seq.epochNanos()?.let { seq ->
                    RemoteChange(
                        RemoteTombstone(tombstone.tableName, tombstone.rowId, tombstone.deletedAt, tombstone.deviceId),
                        seq,
                    )
                }
            }

    override suspend fun hasAnyRecords(): Boolean =
        records.limit(1).get().documents.isNotEmpty()

    override suspend fun push(records: List<RemoteRecord>, tombstones: List<RemoteTombstone>) {
        records.forEach { record ->
            this.records.document(docId(record.tableName, record.rowId)).set(
                FirestoreRecord.serializer(),
                FirestoreRecord(
                    tableName = record.tableName,
                    rowId = record.rowId,
                    updatedAt = record.updatedAt,
                    deviceId = record.deviceId,
                    payload = record.payload,
                    seq = Timestamp.ServerTimestamp,
                ),
            ) { encodeDefaults = true }
        }

        tombstones.forEach { tombstone ->
            val id = docId(tombstone.tableName, tombstone.rowId)
            val recordRef = this.records.document(id)
            firestore.runTransaction {
                val held = get(recordRef)
                val heldUpdatedAt = if (held.exists) {
                    held.data(FirestoreRecordSnapshot.serializer()).updatedAt
                } else {
                    null
                }
                // Deletes arrive late. A device that has been offline publishes a removal from an
                // hour ago, and evicting a row that has since been re-created would lose the
                // re-creation everywhere except the device that made it.
                if (heldUpdatedAt != null && tombstone.deletedAt >= heldUpdatedAt) {
                    delete(recordRef)
                }
                set(
                    this@FirestoreSyncDataSource.tombstones.document(id),
                    FirestoreTombstone.serializer(),
                    FirestoreTombstone(
                        tableName = tombstone.tableName,
                        rowId = tombstone.rowId,
                        deletedAt = tombstone.deletedAt,
                        deviceId = tombstone.deviceId,
                        seq = Timestamp.ServerTimestamp,
                    ),
                ) { encodeDefaults = true }
            }
        }
    }

    /**
     * Epoch nanoseconds, so a cursor keeps the full precision Firestore stores.
     *
     * Rounding to milliseconds would be a data-loss bug rather than an approximation: two documents
     * written within the same millisecond would share a cursor value, and the second would never
     * again satisfy `seq > cursor`. It would be invisible, permanent, and would look like the row
     * had simply never been created.
     */
    private fun BaseTimestamp?.epochNanos(): Long? =
        (this as? Timestamp)?.let { it.seconds * NANOS_PER_SECOND + it.nanoseconds }

    private fun Long.asTimestamp() = Timestamp(this / NANOS_PER_SECOND, (this % NANOS_PER_SECOND).toInt())

    /** One document per row, so a push is naturally idempotent per (table, row). */
    private fun docId(tableName: String, rowId: String) = "${tableName}__$rowId"

    private companion object {
        const val SEQ = "seq"
        const val NANOS_PER_SECOND = 1_000_000_000L
    }
}
