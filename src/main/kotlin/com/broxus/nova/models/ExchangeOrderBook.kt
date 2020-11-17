package com.broxus.nova.models

import com.google.gson.annotations.Expose

data class ExchangeOrderBook (
    @Expose val bids: List<ExchangeOrderBookOrder>,
    @Expose val asks: List<ExchangeOrderBookOrder>
)