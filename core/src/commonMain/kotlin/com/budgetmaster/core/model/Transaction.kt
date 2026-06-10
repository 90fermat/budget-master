package com.budgetmaster.core.model

/**
 * Domain entity representing a financial transaction.
 *
 * @property id Unique identifier of the transaction.
 * @property amount The monetary amount of the transaction.
 * @property category Category of the transaction (e.g., Food, Rent, Salary).
 * @property description Optional text description.
 * @property timestamp Epoch timestamp of when the transaction occurred.
 */
data class Transaction(
    val id: String,
    val amount: Double,
    val category: String,
    val description: String,
    val timestamp: Long
)
