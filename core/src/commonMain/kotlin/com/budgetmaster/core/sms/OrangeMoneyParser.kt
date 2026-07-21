@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.core.sms

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.ExperimentalTime

/**
 * Parser for Orange Money (Cameroon) transaction messages.
 *
 * **Field extraction, not whole-message patterns.** Real samples already vary the same field three
 * ways — `Montant :`, `Montant:`, `Montant Transaction:` — and the balance appears as both
 * `Nouveau solde :` and `Solde:`. A regex per message shape would break on the next wording tweak,
 * so the kind is detected from a couple of keywords and each field is then pulled out
 * independently with tolerant labels. A message that changes slightly loses one field instead of
 * failing entirely.
 *
 * Known shapes this covers:
 * - `Transfert de <msisdn> <name> vers <msisdn> <name> reussi. ... Montant Transaction: …`
 * - `Paiement Orange Money réussi. Montant : … ID : …` (merchant on the first line)
 * - `Paiement de <merchant> reussi par <msisdn> <name>. ID transaction:… Montant:…`
 */
class OrangeMoneyParser : MoneyMessageParser {

    override val provider: String = PROVIDER

    override val senderPatterns: List<Regex> = listOf(
        Regex("orange", RegexOption.IGNORE_CASE),
        Regex("^OM$", RegexOption.IGNORE_CASE),
    )

    override fun parse(body: String, ownerMsisdns: Set<String>, receivedAt: Long): ParsedMoneyMessage? {
        // A message with no transaction id is an advert, a balance check, or an OTP — not a
        // transaction. Requiring the id first is the cheapest way to reject all of those.
        val externalId = ID.find(body)?.groupValues?.get(1) ?: return null
        val amount = AMOUNT.findNumber(body) ?: return null

        val transfer = TRANSFER.find(body)
        val (type, name, msisdn) = when {
            transfer != null -> transfer.toTransfer(ownerMsisdns)
            PAYMENT_MARKER.containsMatchIn(body) -> Triple(
                MoneyMessageType.PAYMENT,
                merchantName(body),
                null,
            )
            else -> return null
        }

        return ParsedMoneyMessage(
            provider = PROVIDER,
            externalId = externalId,
            type = type,
            amount = amount,
            fee = FEE.findNumber(body) ?: 0.0,
            currency = CURRENCY,
            counterpartyName = name,
            counterpartyMsisdn = msisdn,
            balanceAfter = BALANCE.findNumber(body),
            // Falls back to arrival time only when the id carries no date — better an approximate
            // time than none, but the id is preferred because an SMS can arrive late.
            occurredAt = timestampFromId(externalId) ?: receivedAt,
        )
    }

    /**
     * "Transfert de A vers B" is the same sentence in both directions; only matching the user's own
     * number against each side says whether money came or went.
     */
    private fun MatchResult.toTransfer(
        ownerMsisdns: Set<String>,
    ): Triple<MoneyMessageType, String?, String?> {
        val fromMsisdn = groupValues[1]
        val fromName = groupValues[2].trim()
        val toMsisdn = groupValues[3]
        val toName = groupValues[4].trim()

        return if (fromMsisdn in ownerMsisdns) {
            Triple(MoneyMessageType.TRANSFER_OUT, toName, toMsisdn)
        } else {
            // Also the fallback when no owner number is configured: a transfer naming someone else
            // as sender is far more likely to be money arriving.
            Triple(MoneyMessageType.TRANSFER_IN, fromName, fromMsisdn)
        }
    }

    /**
     * The merchant sits in one of two places: a header line (`Orange Money CANAL+`) or inline
     * (`Paiement de Achat Max it3 reussi par …`).
     */
    private fun merchantName(body: String): String? {
        PAYMENT_INLINE_MERCHANT.find(body)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        return body.lineSequence().firstOrNull()
            ?.trim()
            ?.removePrefix("Orange Money")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    /**
     * Orange encodes the transaction time in the id: `MP250704.0013.A43583` is 2025-07-04 00:13.
     * That is when the transaction actually happened, which beats when the SMS was delivered.
     */
    private fun timestampFromId(id: String): Long? {
        val m = ID_TIMESTAMP.find(id) ?: return null
        val (yy, mm, dd, hh, min) = m.destructured
        return runCatching {
            LocalDateTime(2000 + yy.toInt(), mm.toInt(), dd.toInt(), hh.toInt(), min.toInt())
                .toInstant(TimeZone.currentSystemDefault())
                .toEpochMilliseconds()
        }.getOrNull() // A malformed date is not worth failing the whole parse over.
    }

    /** Finds this label's number, tolerating `1 000`, `100000FCFA` and `44.48 FCFA`. */
    private fun Regex.findNumber(body: String): Double? =
        find(body)?.groupValues?.get(1)?.let { raw ->
            raw.replace(" ", "")
                .replace(" ", "")
                // Only a trailing 1-2 digit group is a decimal; anything else is a separator.
                .let { if (Regex(",\\d{1,2}$").containsMatchIn(it)) it.replace(",", ".") else it.replace(",", "") }
                .toDoubleOrNull()
        }

    private companion object {
        const val PROVIDER = MoneyProviders.ORANGE_MONEY
        const val CURRENCY = "XAF"

        /** A number followed by the currency, e.g. `10000 FCFA`, `100000FCFA`, `44.48 FCFA`. */
        const val NUM = "([\\d\\u00A0 .,]+?)\\s*F?\\s*CFA"

        val ID = Regex("""ID\s*(?:de\s*)?(?:transaction)?\s*:\s*([A-Z]{2}\d{6}\.\d{4}\.[A-Z0-9]+)""", RegexOption.IGNORE_CASE)

        /** `MP` + `YYMMDD` + `.` + `HHmm` + `.` */
        val ID_TIMESTAMP = Regex("""^[A-Z]{2}(\d{2})(\d{2})(\d{2})\.(\d{2})(\d{2})\.""")

        /** Matches `Montant :` / `Montant:` / `Montant Transaction:` but deliberately not
         *  `Montant Net:`, which already includes the fee. */
        val AMOUNT = Regex("""Montant(?:\s+Transaction)?\s*:\s*$NUM""", RegexOption.IGNORE_CASE)
        val FEE = Regex("""Frais\s*:\s*$NUM""", RegexOption.IGNORE_CASE)
        val BALANCE = Regex("""(?:Nouveau\s+)?Solde\s*:\s*$NUM""", RegexOption.IGNORE_CASE)

        val TRANSFER = Regex(
            """Transfert\s+de\s+(\d{6,})\s+(.+?)\s+vers\s+(\d{6,})\s+(.+?)\s+r[ée]ussi""",
            RegexOption.IGNORE_CASE,
        )

        val PAYMENT_MARKER = Regex("""Paiement""", RegexOption.IGNORE_CASE)
        val PAYMENT_INLINE_MERCHANT = Regex(
            """Paiement\s+de\s+(.+?)\s+r[ée]ussi\s+par""",
            RegexOption.IGNORE_CASE,
        )
    }
}
