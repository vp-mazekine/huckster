package com.broxus.huckster.prices.adapters

import com.broxus.huckster.interfaces.PriceFeed
import com.broxus.huckster.prices.models.FixedRateInput
import com.broxus.huckster.prices.models.Rate
import com.broxus.logger2
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.importre.crayon.red
import java.io.IOException

/**
 * Adapter for fixed rates configuration
 */
class FixedRate(feedConfiguration: JsonObject) : PriceFeed {
    private var rates: MutableList<Rate> = mutableListOf()

    init {
        val configuration: FixedRateInput

        try {
            configuration = Gson().fromJson(feedConfiguration, FixedRateInput::class.java)
        } catch(e: IOException) {
            throw(IOException("Incorrect price feed configuration!\n${e.localizedMessage}".red(), e))
        }

        rates = configuration.configuration.rates.toMutableList()
    }

    override fun getPrice(fromCurrency: String, toCurrency: String, volume: Float?): Float? {
        rates.forEach {
            if(it.fromCurrency == fromCurrency && it.toCurrency == toCurrency) return it.rate
        }

        return null
    }
}