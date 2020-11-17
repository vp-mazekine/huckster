package com.broxus.huckster.interfaces

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