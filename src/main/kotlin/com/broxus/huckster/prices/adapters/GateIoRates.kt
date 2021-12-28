package com.broxus.huckster.prices.adapters

import com.broxus.huckster.interfaces.IPriceFeed
import com.broxus.huckster.logger2
import io.gate.models.GateIoInput
import com.broxus.huckster.prices.models.Rate
import com.broxus.utils.red
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.gate.GateIoService
import java.io.IOException
import kotlin.math.absoluteValue

class GateIoRates(feedConfiguration: JsonObject) : IPriceFeed {
    private val FEED_CONFIGURATION: GateIoInput

    private var rates: MutableList<Rate> = mutableListOf()
    private var updateAt: Long?

    init {
        try {
            FEED_CONFIGURATION = Gson().fromJson(feedConfiguration, GateIoInput::class.java)
        } catch (e: IOException) {
            throw(IOException("Incorrect price feed configuration!\n${e.stackTrace}".red(), e))
        }

        GateIoService.init(FEED_CONFIGURATION.configuration)

        updateAt = when (FEED_CONFIGURATION.configuration.refreshRate) {
            null -> null
            else -> System.currentTimeMillis() + FEED_CONFIGURATION.configuration.refreshRate.toLong() * 1000
        }

        fetchData()
    }

    /**
     * Fetches data from API and loads into `rates` variable
     */
    private fun fetchData() {
        val orderBook = GateIoService.getOrderBooks(1, FEED_CONFIGURATION.configuration.symbols)
        val symbols = GateIoService.getCurrencyPairs(FEED_CONFIGURATION.configuration.symbols)
        var i: Int?
        var r: Rate?

        orderBook?.forEach { (key, value) ->
            if (symbols?.containsKey(key) != true) return@forEach

            val pair = symbols[key] ?: return@forEach
            val base = pair.base ?: return@forEach
            val quote = pair.quote ?: return@forEach
            val lowestAskPrice = value.asks[0]?.get(0)?.toFloatOrNull() ?: return@forEach
            val highestBidPrice = value.bids[0]?.get(0)?.toFloatOrNull() ?: return@forEach
            val rate = (lowestAskPrice + highestBidPrice) / 2

            i = rates.findRateIndex(
                base, quote
            )

            try {
                r = Rate(
                    if(i == null || i!! > 0) base else quote,
                    if(i == null || i!! > 0) quote else base,
                    rate
                )
            } catch (e: Exception) {
                logger2(
                    ("Error while extracting rate from Gate.io:\n" +
                            e.message + "\n" + e.stackTrace.joinToString("\n")).red()
                )
                return@forEach
            }

            when (i) {
                null -> rates.add(r ?: return@forEach)
                else -> rates[i!!.absoluteValue] = r ?: return@forEach
            }
        }
    }

    override fun getPrice(fromCurrency: String, toCurrency: String, volume: Float?): Float? {
        updateAt?.let {
            if (System.currentTimeMillis() >= it) {
                fetchData()
                updateAt = when (FEED_CONFIGURATION.configuration.refreshRate) {
                    null -> null
                    else -> System.currentTimeMillis() + FEED_CONFIGURATION.configuration.refreshRate.toLong() * 1000
                }
            }
        }

        //  Translate the request to the exchange syntax
        val _fromCurrency = FEED_CONFIGURATION.configuration.dictionary?.get(fromCurrency) ?: fromCurrency
        val _toCurrency   = FEED_CONFIGURATION.configuration.dictionary?.get(toCurrency)   ?: toCurrency

        rates.forEach {
            when {
                (it.fromCurrency == _fromCurrency && it.toCurrency == _toCurrency) -> return it.rate
                (it.fromCurrency == _toCurrency && it.toCurrency == _fromCurrency) -> return 1 / it.rate
            }
        }

        logger2("Direct price not found for pair ${_fromCurrency}_${_toCurrency}. Attempting to find a cross-exchange route...")

        return rates.findOptimalRoute(_fromCurrency, _toCurrency, volume ?: 1F)?.let {
            //  Cache the optimal route
            rates.add(Rate(
                fromCurrency, toCurrency, it.second / (volume ?: 1F)
            ))
            it.second
        }
    }
}