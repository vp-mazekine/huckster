package com.broxus.nova.models

import com.google.gson.annotations.Expose

data class ExchangeOrderBookInput (
    @Expose val workspaceId: String? = null,
    @Expose val base: String,
    @Expose val counter: String
)