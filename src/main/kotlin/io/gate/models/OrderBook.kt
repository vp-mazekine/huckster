package io.gate.models

import com.google.gson.annotations.Expose

/**
 * Gate.io orderbook
 *
 * @property id         String?             Order book ID, which is updated whenever the order book is changed. Valid only when `with_id` is set to `true`
 * @property current    Int?                The timestamp of the response data being generated (in milliseconds)
 * @property update     Int?                The timestamp of when the orderbook last changed (in milliseconds)
 * @property asks       List<List<String>>  Asks order depth
 * @property bids       List<List<String>>  Bids order depth
 * @constructor Create empty Order book
 */
data class OrderBook (
    @Expose val id: String? = null,
    @Expose val current: Int? = null,
    @Expose val update: Int? = null,
    @Expose val asks: List<List<String>>,
    @Expose val bids: List<List<String>>
)