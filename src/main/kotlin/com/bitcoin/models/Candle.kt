package com.bitcoin.models

import com.google.gson.annotations.Expose

data class Candle(
    @Expose val timestamp: String,
    @Expose val open: String,
    @Expose val min: String,
    @Expose val max: String,
    @Expose val close: String,
    @Expose val volume: String,
    @Expose val volumeQuote: String
)
