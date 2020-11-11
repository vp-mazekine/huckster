package com.broxus.huckster.prices.models

import com.google.gson.annotations.Expose

data class Rate (
    @Expose val fromCurrency: String,
    @Expose val toCurrency: String,
    @Expose val rate: Float
)