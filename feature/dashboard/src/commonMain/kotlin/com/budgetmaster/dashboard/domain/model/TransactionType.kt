package com.budgetmaster.dashboard.domain.model

/**
 * Classifies the direction and nature of a financial transaction.
 */
enum class TransactionType {
    /**
     * A monetary inflow (e.g., salary, interest, transfer received).
     */
    INCOME,

    /**
     * A monetary outflow (e.g., purchase, bill payment, transfer sent).
     */
    EXPENSE,

    /**
     * A movement of funds between accounts with no net gain or loss.
     */
    TRANSFER
}
