package com.broxus.com.bitcoin.models

import com.google.gson.annotations.Expose

data class BitcoinComInput(
    @Expose val adapter: String,
    @Expose val configuration: BitcoinComConfiguration
)
