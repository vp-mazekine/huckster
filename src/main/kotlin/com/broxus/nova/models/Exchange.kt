package com.broxus.nova.models

import com.broxus.nova.types.ExchangeOrderStateType
import com.google.gson.annotations.Expose

/**
 * Output model for /users/exchanges method
 *
 * @property id Id of exchange order. UUID ver. 4 rfc
 * @property transactionId Id of transaction. UUID ver. 4 rfc
 * @property from Сurrency identifier or ticker. Can contain more than 3 letters.
 * @property to Сurrency identifier or ticker. Can contain more than 3 letters.
 * @property fromValue Amount of currency. Positive floating point number.
 * @property toValue Amount of currency. Positive floating point number.
 * @property fromExchangedValue Amount of currency. Positive floating point number.
 * @property toExchangedValue Amount of currency. Positive floating point number.
 * @property rate Rate from market
 * @property state Current Exchange Order state.
 * @property isAlive For open orders this flag is true
 * @property createdAt Unix timestamp in milliseconds.
 * @property updatedAt Unix timestamp in milliseconds.
 * @property expiresAt Unix timestamp in milliseconds.
 */
data class Exchange (
    @Expose val id: String,
    @Expose val transactionId: String,
    @Expose val from: String,
    @Expose val to: String,
    @Expose val fromValue: String,
    @Expose val toValue: String,
    @Expose val fromExchangedValue: String,
    @Expose val toExchangedValue: String,
    @Expose val rate: String,
    @Expose val state: ExchangeOrderStateType,
    @Expose val isAlive: Boolean,
    @Expose val createdAt: Long,
    @Expose val updatedAt: Long,
    @Expose val expiresAt: Long?
)