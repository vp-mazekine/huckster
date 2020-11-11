package com.broxus.nova.models

import com.google.gson.annotations.Expose

/**
 * Output model for /users/balance method
 *
 * @property currency Сurrency identifier or ticker. Can contain more than 3 letters.
 * @property total Amount of currency. Positive floating point number.
 * @property frozen Amount of currency. Positive floating point number.
 * @property available Amount of currency. Positive floating point number.
 */
data class AccountBalance (
    @Expose val currency: String,
    @Expose val total: String,
    @Expose val frozen: String,
    @Expose val available: String
)