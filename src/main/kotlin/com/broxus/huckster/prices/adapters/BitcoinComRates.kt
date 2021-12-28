package com.broxus.huckster.prices.adapters

import com.bitcoin.client.BitcoinComService
import com.broxus.huckster.interfaces.IPriceFeed
import com.bitcoin.models.BitcoinComInput
import com.broxus.huckster.logger2
import com.broxus.huckster.prices.models.Rate
import com.broxus.utils.red
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.IOException

class BitcoinComRates(feedConfiguration: JsonObject): IPriceFeed {
    private val FEED_CONFIGURATION: BitcoinComInput

    private var rates: MutableList<Rate> = mutableListOf()
    private var updateAt: Long?

    init {
        try {
            FEED_CONFIGURATION = Gson().fromJson(feedConfiguration, BitcoinComInput::class.java)
        } catch (e: IOException) {
            throw(IOException("Incorrect price feed configuration!\n${e.stackTrace}".red(), e))
        }

        BitcoinComService.init(FEED_CONFIGURATION.configuration)

        updateAt = when(FEED_CONFIGURATION.configuration.refreshRate) {
            null -> null
            else -> System.currentTimeMillis() + FEED_CONFIGURATION.configuration.refreshRate.toLong() * 1000
        }

        fetchData()
    }

    private fun fetchData() {
        val orderBook = BitcoinComService.getOrderBook(1, FEED_CONFIGURATION.configuration.symbols)
        val symbols = BitcoinComService.getSymbolDetails(FEED_CONFIGURATION.configuration.symbols)
        var i: Int?
        var r: Rate

        orderBook?.forEach {key, value ->
            symbols?.filter{ it.id == key }?.let {symbol ->
                i = rates.findRateIndex(
                    symbol[0].baseCurrency, symbol[0].quoteCurrency
                )

                try {
                    r = Rate(
                        if(i == null || i!! > 0) symbol[0].baseCurrency else symbol[0].quoteCurrency,
                        if(i == null || i!! > 0) symbol[0].quoteCurrency else symbol[0].baseCurrency,
                        (value.ask[0].price.toFloat() + value.bid[0].price.toFloat()) / 2
                        )
                } catch (e: Exception) {
                    logger2(("Error while extracting rate from Bitcoin.com:\n" +
                            e.message + "\n" + e.stackTrace.joinToString("\n")).red())
                    return@let
                }

                when(i) {
                    null -> rates.add(r)
                    else -> rates[i!!] = r
                }
            }
        }
    }

    override fun getPrice(fromCurrency: String, toCurrency: String, volume: Float?): Float? {
        updateAt?.let {
            if(System.currentTimeMillis() >= it) {
                fetchData()
                updateAt = when(FEED_CONFIGURATION.configuration.refreshRate) {
                    null -> null
                    else -> System.currentTimeMillis() + FEED_CONFIGURATION.configuration.refreshRate.toLong() * 1000
                }
            }
        }

        //  Translate the request to the exchange syntax
        val _fromCurrency = FEED_CONFIGURATION.configuration.dictionary?.get(fromCurrency) ?: fromCurrency
        val _toCurrency = FEED_CONFIGURATION.configuration.dictionary?.get(toCurrency) ?: toCurrency

        rates.forEach {
            when {
                (it.fromCurrency == _fromCurrency && it.toCurrency == _toCurrency) -> return it.rate
                (it.fromCurrency == _toCurrency && it.toCurrency == _fromCurrency) -> return 1/it.rate
            }
        }

        return null
    }

/*
    private fun MutableList<Rate>.findIndex(fromCurrency: String, toCurrency: String): Int? {
        this.forEachIndexed { index, rate ->
            if(rate.fromCurrency == fromCurrency && rate.toCurrency == toCurrency) return index
        }

        return null
    }
*/

}