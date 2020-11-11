package com.broxus.huckster.prices.models

import com.google.gson.annotations.Expose

data class FixedRateConfiguration (
    @Expose val rates: List<Rate>
)
