package com.broxus.huckster.prices.models

import com.google.gson.annotations.Expose

data class FixedRateInput (
    @Expose val adapter: String,
    @Expose val configuration: FixedRateConfiguration
)