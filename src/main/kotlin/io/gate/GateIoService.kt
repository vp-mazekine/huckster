package io.gate

import arrow.core.Either
import com.bitcoin.models.Symbol
import com.broxus.huckster.interfaces.Unfoldable
import com.google.gson.JsonArray
import io.gate.models.GateIoConfiguration
import io.gate.gateapi.ApiClient
import io.gate.gateapi.ApiException
import io.gate.gateapi.Configuration
import io.gate.gateapi.GateApiException
import io.gate.gateapi.api.SpotApi
import io.gate.gateapi.models.CurrencyPair
import io.gate.gateapi.models.OrderBook
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Response

object GateIoService : Unfoldable() {
    private val client: ApiClient = Configuration.getDefaultApiClient()
    private var config: GateIoConfiguration? = null

    var api: SpotApi? = null
        private set

    fun init(config: GateIoConfiguration) {
        this.config = config
        client.basePath = config.apiPath
        api = SpotApi(client)
    }

    fun getOrderBooks(
        limit: Int = 100,
        symbols: List<String>
    ): Map<String, OrderBook>? {
        if(api == null) {
            logger.error("Gate.io API was not properly initialized!")
            return null
        }

        if(symbols.isEmpty()) {
            logger.error("[getOrderBook] Symbols not specified")
            return null
        }

        val result: MutableMap<String, OrderBook> = mutableMapOf()

        runBlocking {
            symbols.forEach { symbol ->
                launch {
                    //  Make three attempts to get the orderbook of specified symbol
                    for(i in 1..3) {
                        val ob = getOrderBook(symbol, limit)
                        if(ob != null) {
                            result[symbol] = ob
                            break
                        }
                    }
                }
            }
        }

        return if(result.isEmpty()) null else result.toMap()
    }

    /**
     * Get meta info about currency pairs
     *
     * @param symbols   List of pairs
     * @return
     */
    fun getCurrencyPairs(
        symbols: List<String>
    ): Map<String, CurrencyPair>? {
        if(api == null) {
            logger.error("Gate.io API was not properly initialized!")
            return null
        }

        if(symbols.isEmpty()) {
            logger.error("[getOrderBook] Symbols not specified")
            return null
        }

        val result: MutableMap<String, CurrencyPair> = mutableMapOf()

        runBlocking {
            symbols.forEach { symbol ->
                launch {
                    //  Make three attempts to get the info of specified symbol
                    for(i in 1..3) {
                        val pair = getCurrencyPair(symbol)
                        if(pair != null) {
                            result[symbol] = pair
                            break
                        }
                    }
                }
            }
        }

        return if(result.isEmpty()) null else result.toMap()
    }

    /**
     * Gets the single orderbook
     *
     * @param pair  Trading pair
     * @param limit How deep to return the orderbook
     * @return
     */
    private fun getOrderBook(pair: String, limit: Int): OrderBook? {
        return try {
            api!!
                .listOrderBook(pair)
                .interval("0")
                .limit(limit)
                .withId(false)
                .execute()
        } catch (e: GateApiException) {
            logger.error("Gate.io API exception\nLabel: ${e.errorLabel}\nMessage: ${e.message}\n${e.stackTraceToString()}")
            null
        } catch (e: ApiException) {
            logger.error("Exception when calling SpotApi.listOrderBook\nStatus code: ${e.code}\nHeaders: ${e.responseHeaders}\n${e.stackTraceToString()}")
            null
        } catch (e: Exception) {
            logger.error("Exception when calling SpotApi.listOrderBook\n${e.message}\n${e.stackTraceToString()}")
            null
        }
    }

    private fun getCurrencyPair(pair: String): CurrencyPair? {
        return try {
            api!!.getCurrencyPair(pair)
        } catch (e: GateApiException) {
            logger.error("Gate.io API exception\nLabel: ${e.errorLabel}\nMessage: ${e.message}\n${e.stackTraceToString()}")
            null
        } catch (e: ApiException) {
            logger.error("Exception when calling SpotApi.listOrderBook\nStatus code: ${e.code}\nHeaders: ${e.responseHeaders}\n${e.stackTraceToString()}")
            null
        } catch (e: Exception) {
            logger.error("Exception when calling SpotApi.listOrderBook\n${e.message}\n${e.stackTraceToString()}")
            null
        }
    }

}