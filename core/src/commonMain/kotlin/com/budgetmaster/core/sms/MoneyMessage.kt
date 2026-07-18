package com.budgetmaster.core.sms

/** What a mobile-money message describes, which decides the sign and the wording shown. */
enum class MoneyMessageType {
    /** Money arrived from someone else. */
    TRANSFER_IN,

    /** Money sent to someone else. */
    TRANSFER_OUT,

    /** Paid a merchant. */
    PAYMENT,

    /** Cash out at an agent. */
    WITHDRAWAL,

    /** Cash in at an agent. */
    DEPOSIT,
}

/**
 * One mobile-money transaction, extracted from a provider message.
 *
 * Deliberately provider-neutral and free of app concepts: the parser's only job is to say what the
 * message *states*. Mapping this onto a ledger entry (category, wallet, fee handling) is the
 * caller's decision.
 *
 * @property externalId the provider's own transaction id — the dedup key. Providers re-send
 *   messages, and the same transaction must never land twice.
 * @property amount the principal, always positive; [type] carries the direction.
 * @property fee charged on top of [amount]. Mobile-money fees are large enough in these markets
 *   to be worth tracking as their own spend rather than folding into the total.
 * @property balanceAfter the balance the provider reports. Worth keeping: if it stops agreeing
 *   with the computed balance, a message was missed.
 * @property occurredAt when the transaction happened, derived from the id where the provider
 *   encodes it — which is more truthful than when the SMS was received.
 */
data class ParsedMoneyMessage(
    val provider: String,
    val externalId: String,
    val type: MoneyMessageType,
    val amount: Double,
    val fee: Double = 0.0,
    val currency: String,
    val counterpartyName: String? = null,
    val counterpartyMsisdn: String? = null,
    val balanceAfter: Double? = null,
    val occurredAt: Long? = null,
) {
    /** True when the entry should reduce the balance. */
    val isOutflow: Boolean
        get() = when (type) {
            MoneyMessageType.TRANSFER_OUT, MoneyMessageType.PAYMENT, MoneyMessageType.WITHDRAWAL -> true
            MoneyMessageType.TRANSFER_IN, MoneyMessageType.DEPOSIT -> false
        }
}

/**
 * Parses one provider's messages.
 *
 * @param ownerMsisdns the user's own numbers. Required, not optional: "Transfert de A vers B" is
 *   the same sentence whether money came or went — only knowing which side is the user resolves
 *   the direction.
 */
interface MoneyMessageParser {
    /** The provider this handles, e.g. `orange_money`. */
    val provider: String

    /** Sender ids/shortcodes whose messages this parser should be offered. */
    val senderPatterns: List<Regex>

    /** True if [sender] looks like this provider. */
    fun handlesSender(sender: String): Boolean =
        senderPatterns.any { it.containsMatchIn(sender) }

    /** @return the transaction, or null if this message isn't one (adverts, balance checks, OTPs). */
    fun parse(body: String, ownerMsisdns: Set<String>, receivedAt: Long): ParsedMoneyMessage?
}
