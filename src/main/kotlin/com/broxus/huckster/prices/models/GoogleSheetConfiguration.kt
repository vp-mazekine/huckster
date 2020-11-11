package com.broxus.huckster.prices.models

import com.google.gson.annotations.Expose

data class GoogleSheetConfiguration(
    @Expose val refreshRate: String? = null,
    @Expose val sheetId: String,
    @Expose val sourceDataRange: String,
    @Expose val sourceDataSheet: String
)