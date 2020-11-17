package com.broxus.nova.models

import com.google.gson.annotations.Expose

data class ExchangeOrderBookOrder (
    @Expose val rate: String,
    @Expose val volume: String
)
