package com.bitcoin.models

import com.google.gson.annotations.Expose

data class Symbol(
    @Expose val id: String,
    @Expose val baseCurrency: String,
    @Expose val quoteCurrency: String,
    @Expose val quantityIncrement: String,
    @Expose val tickSize: String,
    @Expose val takeLiquidityRate: String,
    @Expose val provideLiquidityRate: String,
    @Expose val feeCurrency: String
)
