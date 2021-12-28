package com.bitcoin.models

import com.google.gson.annotations.Expose

data class OrderBookTrade (
    @Expose val price: String,
    @Expose val size: String
)
