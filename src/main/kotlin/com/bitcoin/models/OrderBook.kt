package com.bitcoin.models

import com.google.gson.annotations.Expose

data class OrderBook(
    @Expose val symbol: String? = null,
    @Expose val timestamp: String? = null,
    @Expose val batchingTime: String? = null,
    @Expose val ask: List<OrderBookTrade>,
    @Expose val bid: List<OrderBookTrade>
)
