@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.core.backup

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.budgetmaster.core.db.BudgetMasterDatabase
import com.budgetmaster.core.db.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Proves a backup actually restores.
 *
 * Runs against a real SQLite driver rather than fakes, because the claim being tested is that data
 * survives a write to the database, a read back out, encryption, decryption, and a write into a
 * *different* database. A fake handing its own objects back would demonstrate none of that.
 *
 * The Android `javax.crypto` path is the one exercised here, and it is the one that ships.
 */
class BackupRoundTripTest {

    private val passphrase = "correct horse battery staple"

    private fun freshDatabase(): BudgetMasterDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BudgetMasterDatabase.Schema.synchronous().create(driver)
        return BudgetMasterDatabase(driver)
    }

    private fun service(db: BudgetMasterDatabase) =
        BackupService(DatabaseProvider(db), Dispatchers.Unconfined)

    /** Seeds one of everything, including the rows that are easy to lose. */
    private suspend fun seed(db: BudgetMasterDatabase) {
        val q = db.budgetMasterDatabaseQueries
        q.insertUser("u1", "Cyrille", "c@example.com", "XAF", 100L)
        q.insertAccount("acc1", "u1", "Everyday", "CASH", 1_000.0, "XAF", 100L, 0, 1)
        q.insertAccount("acc2", "u1", "Epargne", "SAVINGS", 5_000.0, "XAF", 100L, 1, 1)
        q.insertCategory("cat_custom", "u1", "Tontine", "💰", "#123456", 0)
        q.insertImportedTransaction(
            id = "t1",
            accountId = "acc1",
            categoryId = "cat_custom",
            amount = -20_244.0,
            description = "FOYANG CYRILLE",
            timestamp = 1_752_753_600_000L,
            notes = "a note",
            tags = "tag1,tag2",
            isRecurring = 0,
            transferGroupId = null,
            externalId = "OM250717.1200.A1",
            source = "SMS",
        )
        q.insertBudget("b1", "u1", "cat_custom", 50_000.0, 20_244.0, 0L, Long.MAX_VALUE)
        q.insertSavingsGoal("g1", "u1", "Moto", 500_000.0, 120_000.0, 2_000_000_000_000L, 100L)
        q.insertRecurringTransaction("r1", "acc1", "cat_custom", -5_000.0, "Loyer", "MONTHLY", 0L, 100L, 1)
        q.insertNotification("n1", "u1", "Orange Money", "imported", 100L, 0)
        q.insertImportedMessage(
            hash = "h1",
            userId = "u1",
            provider = "orange_money",
            sender = "OrangeMoney",
            receivedAt = 100L,
            status = "PENDING_REVIEW",
            transactionId = "t1",
            externalId = "OM250717.1200.A1",
            pendingAccountId = "acc1",
            pendingAmount = -20_244.0,
            pendingFee = 44.48,
            pendingDescription = "FOYANG CYRILLE",
            pendingOccurredAt = 100L,
        )
    }

    @Test
    fun `a backup restores every table into an empty database`(): Unit = runBlocking {
        val source = freshDatabase()
        seed(source)

        val file = BackupFile.write(service(source).export(), passphrase)

        // A different, empty database — the real restore scenario, not a self-comparison.
        val target = freshDatabase()
        val envelope = BackupFile.read(file, passphrase).getOrThrow()
        service(target).restore(envelope)

        val q = target.budgetMasterDatabaseQueries
        assertEquals(1, q.selectAllUsersForBackup().awaitAsList().size)
        assertEquals(2, q.selectAllAccountsForBackup().awaitAsList().size)
        assertEquals(1, q.selectAllCategoriesForBackup().awaitAsList().size)
        assertEquals(1, q.selectAllBudgetsForBackup().awaitAsList().size)
        assertEquals(1, q.selectAllGoalsForBackup().awaitAsList().size)
        assertEquals(1, q.selectAllRecurringTransactions().awaitAsList().size)
        assertEquals(1, q.selectAllNotificationsForBackup().awaitAsList().size)
        assertEquals(1, q.selectAllImportedMessagesForBackup().awaitAsList().size)

        val restored = q.selectAllTransactions().awaitAsList().single()
        assertEquals(-20_244.0, restored.amount)
        assertEquals("FOYANG CYRILLE", restored.description)
        assertEquals("a note", restored.notes)
        assertEquals("tag1,tag2", restored.tags)
        // The two that would silently break mobile-money dedup if a restore dropped them.
        assertEquals("OM250717.1200.A1", restored.externalId)
        assertEquals("SMS", restored.source)
    }

    @Test
    fun `restore replaces what was there rather than merging`(): Unit = runBlocking {
        val source = freshDatabase()
        seed(source)
        val file = BackupFile.write(service(source).export(), passphrase)

        // A target holding *different* data, which must be gone afterwards.
        val target = freshDatabase()
        val tq = target.budgetMasterDatabaseQueries
        tq.insertUser("other", "Someone", "other@example.com", "USD", 1L)
        tq.insertAccount("accX", "other", "Stale wallet", "CASH", 42.0, "USD", 1L, 0, 1)

        service(target).restore(BackupFile.read(file, passphrase).getOrThrow())

        val accounts = tq.selectAllAccountsForBackup().awaitAsList()
        assertEquals(2, accounts.size)
        assertTrue(accounts.none { it.id == "accX" }, "the pre-existing wallet must not survive")
        assertNull(tq.selectUserById("other").awaitAsList().firstOrNull())
    }

    @Test
    fun `the wrong passphrase does not decrypt`(): Unit = runBlocking {
        val source = freshDatabase()
        seed(source)
        val file = BackupFile.write(service(source).export(), passphrase)

        val failure = BackupFile.read(file, "not the passphrase").exceptionOrNull()
        assertIs<BackupException>(failure)
        assertEquals(BackupReadError.WrongPassphrase, failure.error)
    }

    @Test
    fun `a tampered file is refused`(): Unit = runBlocking {
        val source = freshDatabase()
        seed(source)
        val file = BackupFile.write(service(source).export(), passphrase)

        // Flip a character in the ciphertext; GCM's tag must catch it.
        val parts = file.split('.')
        val body = parts[3]
        val flipped = body.take(10) + (if (body[10] == 'A') 'B' else 'A') + body.drop(11)
        val tampered = listOf(parts[0], parts[1], parts[2], flipped).joinToString(".")

        val failure = BackupFile.read(tampered, passphrase).exceptionOrNull()
        assertIs<BackupException>(failure)
        assertEquals(BackupReadError.WrongPassphrase, failure.error)
    }

    @Test
    fun `a file that is not a backup is named as such`() {
        val failure = BackupFile.read("just some text the user picked", passphrase).exceptionOrNull()
        assertIs<BackupException>(failure)
        assertEquals(BackupReadError.NotABackup, failure.error)
    }

    @Test
    fun `a newer format version is refused rather than misread`() {
        val fromTheFuture = "BMBAK99.AAAA.BBBB.CCCC"
        val failure = BackupFile.read(fromTheFuture, passphrase).exceptionOrNull()
        assertIs<BackupException>(failure)
        assertEquals(BackupReadError.UnsupportedVersion(99), failure.error)
    }

    @Test
    fun `the same data encrypted twice produces different files`(): Unit = runBlocking {
        val source = freshDatabase()
        seed(source)
        val envelope = service(source).export()

        // Per-file salt and IV, so two backups of identical data cannot be compared for equality
        // or recognised as the same content by anyone holding both.
        assertNotEquals(BackupFile.write(envelope, passphrase), BackupFile.write(envelope, passphrase))
    }
}
