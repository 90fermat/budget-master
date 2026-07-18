package com.budgetmaster.core.sms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Golden corpus of **real** Orange Money (Cameroon) messages.
 *
 * This is the highest-value test suite in the app: a parser regression doesn't throw, it silently
 * writes wrong numbers into someone's ledger. Every sample here is a message that actually
 * arrived, with the owner's own details kept because direction depends on them.
 */
class OrangeMoneyParserTest {

    private val parser = OrangeMoneyParser()

    /** The account holder — the number the parser compares against to resolve direction. */
    private val owner = setOf("659228030")

    private val receivedAt = 1_752_753_600_000L

    private fun parse(body: String) = parser.parse(body, owner, receivedAt)

    // ── Merchant payment, header-line merchant ───────────────────────────────

    private val canalPayment = """
        Orange Money CANAL+
        Paiement Orange Money réussi. Montant : 10000 FCFA Frais : 0 FCFA
        ID : MP250704.0013.A43583 Nouveau solde : 53931.29 FCFA
    """.trimIndent()

    @Test
    fun `parses a merchant payment with the merchant on the header line`() {
        val m = assertNotNull(parse(canalPayment))

        assertEquals(MoneyMessageType.PAYMENT, m.type)
        assertEquals(10000.0, m.amount)
        assertEquals(0.0, m.fee)
        assertEquals("CANAL+", m.counterpartyName)
        assertEquals("MP250704.0013.A43583", m.externalId)
        assertEquals(53931.29, m.balanceAfter)
        assertEquals("XAF", m.currency)
        assertTrue(m.isOutflow)
    }

    // ── Incoming transfer ────────────────────────────────────────────────────

    private val incomingTransfer =
        "Transfert de 690440480 TCHOUKEN vers 659228030 FOYANG reussi. Details: " +
            "ID transaction: PP260322.2159.B05939, Montant Transaction: 100000FCFA, " +
            "Frais: 0 FCFA, Commission: 0 FCFA, Montant Net: 100000 FCFA, " +
            "Nouveau Solde: 116010.99 FCFA."

    @Test
    fun `a transfer to the owner is money in, attributed to the sender`() {
        val m = assertNotNull(parse(incomingTransfer))

        assertEquals(MoneyMessageType.TRANSFER_IN, m.type)
        assertEquals(100000.0, m.amount, "amount runs together with the currency: 100000FCFA")
        assertEquals("TCHOUKEN", m.counterpartyName)
        assertEquals("690440480", m.counterpartyMsisdn)
        assertEquals(116010.99, m.balanceAfter)
        assertTrue(!m.isOutflow)
    }

    // ── Outgoing transfer, with a fee ────────────────────────────────────────

    private val outgoingTransfer =
        "Transfert de 659228030 FOYANG vers 699014204 MIKAM reussi. " +
            "ID transaction: PP260621.0702.D51614, Montant Transaction: 20244 FCFA, " +
            "Frais: 44.48 FCFA, Commission: 0 FCFA, Montant Net: 20288.48 FCFA, " +
            "Nouveau Solde: 29713.53 FCFA."

    @Test
    fun `a transfer from the owner is money out, attributed to the recipient`() {
        val m = assertNotNull(parse(outgoingTransfer))

        assertEquals(MoneyMessageType.TRANSFER_OUT, m.type)
        assertEquals("MIKAM", m.counterpartyName)
        assertEquals("699014204", m.counterpartyMsisdn)
        assertTrue(m.isOutflow)
    }

    @Test
    fun `the principal is taken, not Montant Net which already includes the fee`() {
        val m = assertNotNull(parse(outgoingTransfer))

        // Montant Net (20288.48) = principal + fee. Recording that as the amount would double-count
        // the fee once it is also stored as its own entry.
        assertEquals(20244.0, m.amount)
        assertEquals(44.48, m.fee)
    }

    /**
     * The principal must be chosen *structurally*, not because it happens to appear first.
     *
     * In the real messages `Montant Transaction:` precedes `Montant Net:`, so a regex loose enough
     * to match both would still return the right number by accident. This reorders them so that
     * only a pattern which genuinely refuses `Montant Net` can pass.
     */
    @Test
    fun `Montant Net is rejected even when it appears before the principal`() {
        val reordered =
            "Transfert de 659228030 FOYANG vers 699014204 MIKAM reussi. " +
                "ID transaction: PP260621.0702.D51614, Montant Net: 20288.48 FCFA, " +
                "Frais: 44.48 FCFA, Montant Transaction: 20244 FCFA, " +
                "Nouveau Solde: 29713.53 FCFA."

        assertEquals(20244.0, assertNotNull(parse(reordered)).amount)
    }

    /** The same sentence in both directions — only the owner's number disambiguates it. */
    @Test
    fun `direction flips when the owner is the other party`() {
        val asRecipient = parser.parse(outgoingTransfer, setOf("699014204"), receivedAt)

        assertEquals(MoneyMessageType.TRANSFER_IN, assertNotNull(asRecipient).type)
    }

    // ── Merchant payment, inline merchant ────────────────────────────────────

    private val inlinePayment =
        "Paiement de Achat Max it3 reussi par 659228030 FOYANG. " +
            "ID transaction:MP260609.0808.C00367, Montant:1000 FCFA. Solde: 116866.01 FCFA."

    @Test
    fun `parses a payment with the merchant inline and no spaces after labels`() {
        val m = assertNotNull(parse(inlinePayment))

        assertEquals(MoneyMessageType.PAYMENT, m.type)
        assertEquals(1000.0, m.amount)
        assertEquals("Achat Max it3", m.counterpartyName)
        assertEquals("MP260609.0808.C00367", m.externalId, "no space after 'ID transaction:'")
        assertEquals(116866.01, m.balanceAfter, "labelled 'Solde:' here, not 'Nouveau Solde:'")
        assertEquals(0.0, m.fee, "no Frais field in this shape")
    }

    // ── The id carries the real transaction time ─────────────────────────────

    @Test
    fun `derives the transaction time from the id rather than the delivery time`() {
        // MP250704.0013 → 2025-07-04 00:13, which is nothing like receivedAt (2026-07-17).
        val m = assertNotNull(parse(canalPayment))

        assertNotNull(m.occurredAt)
        assertTrue(m.occurredAt!! < receivedAt, "the id's date predates delivery")
        // Same instant re-derived, so a timezone-dependent literal isn't baked into the assertion.
        assertEquals(m.occurredAt, parse(canalPayment)?.occurredAt)
    }

    @Test
    fun `different ids give different times`() {
        val a = assertNotNull(parse(canalPayment)).occurredAt
        val b = assertNotNull(parse(incomingTransfer)).occurredAt

        assertTrue(a != b)
    }

    // ── Dedup ────────────────────────────────────────────────────────────────

    /** Providers re-send; the same transaction must resolve to the same key. */
    @Test
    fun `a re-sent message yields the same external id`() {
        assertEquals(parse(outgoingTransfer)?.externalId, parse(outgoingTransfer)?.externalId)
        assertEquals("PP260621.0702.D51614", parse(outgoingTransfer)?.externalId)
    }

    // ── Non-transactions must be rejected ────────────────────────────────────

    @Test
    fun `ignores messages that are not transactions`() {
        assertNull(parse("Votre solde est de 5000 FCFA."), "a balance check is not a transaction")
        assertNull(parse("Votre code de confirmation est 123456"), "an OTP is not a transaction")
        assertNull(parse("Rechargez votre compte Orange Money et gagnez des prix!"), "an advert")
        assertNull(parse(""))
    }

    @Test
    fun `ignores a transaction-shaped message with no id`() {
        assertNull(parse("Paiement reussi. Montant : 5000 FCFA Nouveau solde : 100 FCFA"))
    }

    // ── Sender routing ───────────────────────────────────────────────────────

    @Test
    fun `claims Orange senders only`() {
        assertTrue(parser.handlesSender("OrangeMoney"))
        assertTrue(parser.handlesSender("Orange"))
        assertTrue(parser.handlesSender("OM"))
        assertTrue(!parser.handlesSender("MTNMoMo"))
        assertTrue(!parser.handlesSender("Safaricom"))
    }
}
