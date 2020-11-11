package com.broxus.huckster.interfaces

import com.broxus.huckster.prices.models.Rate
import com.google.gson.JsonObject

interface PriceFeed {
    /**
     * Get rate for a specified currency pair
     *
     * @param fromCurrency
     * @param toCurrency
     * @param volume
     * @return Requested rate, or null if it is unavailable
     */
    fun getPrice(fromCurrency: String, toCurrency: String, volume: Float?): Float?
}