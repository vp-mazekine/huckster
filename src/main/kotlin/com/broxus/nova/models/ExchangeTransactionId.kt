package com.broxus.nova.models

import com.google.gson.annotations.Expose

/**
 * Output model for /exchange/limit method
 *
 * @property transactionId Id of transaction. UUID ver. 4 rfc
 */
data class ExchangeTransactionId (
    @Expose val transactionId: String
)