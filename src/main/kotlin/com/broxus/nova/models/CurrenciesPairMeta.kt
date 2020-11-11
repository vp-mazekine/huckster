package com.broxus.nova.models

import com.google.gson.annotations.Expose

/**
 * Input model for /meta/currencies_pairs method
 *
 * @property base Сurrency identifier or ticker. Can contain more than 3 letters.
 * @property counter Сurrency identifier or ticker. Can contain more than 3 letters.
 * @property min Amount of currency. Positive floating point number.
 * @property max Amount of currency. Positive floating point number.
 * @property baseScale
 * @property counterScale
 */
data class CurrenciesPairMeta (
    @Expose val base: String,
    @Expose val counter: String,
    @Expose val min: String,
    @Expose val max: String,
    @Expose val baseScale: Number,
    @Expose val counterScale: Number
)