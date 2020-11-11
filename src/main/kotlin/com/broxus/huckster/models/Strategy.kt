package com.broxus.huckster.models

import com.google.gson.annotations.Expose

/**
 * Defines execution strategy per target currency
 *
 * @property orderbookFee Orderbook maker fee
 * @property sizeStructure  Distribution of the source amount per orderbook
 * @property spreadStructure Distribution of the price per orderbook, number of items should be equal to sizeStructure
 * @property targetCurrency Currency to exchange to
 * @property volumePart Share of the tradeable amount to include in the strategy
 */
data class Strategy(
    @Expose val orderbookFee: OrderbookFee,
    @Expose val sizeStructure: List<String>,
    @Expose val spreadStructure: List<String>,
    @Expose val targetCurrency: String,
    @Expose val volumePart: String
)