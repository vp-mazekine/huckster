package com.bitcoin.client

import arrow.core.Either
import com.bitcoin.client.interfaces.BitcoinComInterface
import com.bitcoin.models.BitcoinComConfiguration
import com.bitcoin.models.Candle
import com.bitcoin.models.OrderBook
import com.bitcoin.models.Symbol
import com.bitcoin.types.CandlesPeriods
import com.bitcoin.types.SortingOrder
import com.broxus.huckster.interfaces.Unfoldable
import com.broxus.huckster.logger2
import com.broxus.utils.red
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.ZonedDateTime

object BitcoinComService : Unfoldable() {
    private var api: BitcoinComInterface? = null
    private var apiConfig: BitcoinComConfiguration? = null
    //private var gson: Gson? = null
    //private val logger = LogManager.getLogger(this::class.java)

    fun init(config: BitcoinComConfiguration) {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(config.apiPath)
            .build()

        this.api = retrofit.create(BitcoinComInterface::class.java)
        this.apiConfig = config
        //this.gson = Gson()
    }

    fun getCandles(
        period: CandlesPeriods = CandlesPeriods.M5,
        sort: SortingOrder = SortingOrder.ASC,
        from: ZonedDateTime? = null,
        till: ZonedDateTime? = null,
        limit: Int? = 100,
        offset: Int? = 0,
        symbols: List<String>?
    ): Map<String, List<Candle>>? {

        val response: Response<JsonObject>

        try {
            //  Perform request
            response = this.api!!.getCandles(
                period,
                sort,
                from?.toOffsetDateTime(),
                till?.toOffsetDateTime(),
                limit,
                offset,
                symbols?.joinToString()
            ).execute()
        } catch (e: Exception) {
            this.logger.error("Bitcoin.com API service was not properly initialized!", e)
            return null
        }

        if (!response.isSuccessful || response.body() == null) return null

        val result: MutableMap<String, List<Candle>> = mutableMapOf()

        response.body()?.let {
            it.keySet().forEach { key ->
                val tempList: MutableList<Candle> = mutableListOf()
                try {
                    when (it[key]) {
                        is JsonArray -> {
                            it[key].asJsonArray.forEach {symbol ->
                                tempList.add(
                                    this.gson.fromJson(symbol, Candle::class.java)
                                )
                            }
                        }
                        else -> {
                            tempList.add(
                                this.gson.fromJson(it[key], Candle::class.java)
                            )
                        }
                    }
                } catch (e: Exception) {
                    logger2((
                        "Invalid response from Bitcoin.com:\n" +
                        e.message + "\n" +
                        e.stackTrace.joinToString("\n")
                    ).red())
                }

                if(tempList.isNotEmpty()) result[key] = tempList.toList()
            }
        }

        return if(result.isNotEmpty()) result.toMap() else null
    }

    fun getSymbolDetails(
        symbols: List<String>?
    ): List<Symbol>? {
        val result: Response<JsonArray>

        try {
            //  Perform request
            result = api!!.getSymbolDetails(symbols?.joinToString(",")).execute()
        } catch(e: Exception) {
            logger.error("Bitcoin.com API service was not properly initialized!", e)
            return null
        }

        //  Transform server response
        unfoldResponse(result, List::class.java).apply {
            return when(this) {
                is Either.Right -> this.b.castJsonArrayToType<Symbol>()
                is Either.Left -> {
                    logger.error(this.a.toString())
                    null
                }
                else -> null
            }
        }
    }

    fun getOrderBook(
        limit: Int? = 100,
        symbols: List<String>? = null
    ): Map<String, OrderBook>? {
        val response: Response<JsonObject>

        try {
            //  Perform request
            response = api!!.getOrderBook(
                limit,
                symbols?.joinToString(",")
            ).execute()
        } catch (e: Exception) {
            this.logger.error("Bitcoin.com API service was not properly initialized!", e)
            return null
        }

        if (!response.isSuccessful || response.body() == null) return null

        val result: MutableMap<String, OrderBook> = mutableMapOf()

        response.body()?.let {
            it.keySet().forEach { key ->
                result[key] = gson.fromJson(it[key], OrderBook::class.java)
            }
        }

        return if(result.isEmpty()) null else result.toMap()
    }

    /**
     * Parse the server response and convert it to the appropriate type or return error description
     *
     * @param T Expected response type
     * @param r Response object
     * @param t Expected response type's class
     * @return Either<ErrorDescription, T>
     */
/*
    private inline fun <reified T> unfoldResponse(
        r: Response<out JsonElement>,
        t: Class<T>
    ): Either<ErrorDescription, T>? {
        return try {
            when (r.body()) {
                //  If the response body is void
                null -> Left(
                    when (r.message()) {
                        //  Error without details
                        null -> ErrorDescription(
                            when (r.code()) {
                                in 400..499 -> "Request error"
                                in 500..599 -> "Server error"
                                else -> "Unknown error"
                            },
                            r.code().toString()
                        )
                        else -> {
                            ErrorDescription(
                                r.message(),
                                r.code().toString()
                            )
                        }
                    }
                )

                //  If the server returned the content
                else -> {
                    if (r.isSuccessful) {
                        //  Try to apply the requested model
                        Right(this.gson!!.fromJson(r.body()!!, t))
                    } else {
                        //  Return the error
                        Left(this.gson!!.fromJson(r.body()!!, ErrorDescription::class.java))
                    }
                }
            }
        } catch (e: Exception) {
            //  Handle unexpected errors
            Left(
                ErrorDescription(
                    e.localizedMessage,
                    r.code().toString()
                )
            )
        }
    }
*/

    /**
     * Transform the returned LinkedTreeMap to the desired type
     *
     * @param T Type to cast
     * @return List<T>
     */
/*
    private inline fun <reified T> List<*>.castJsonArrayToType(): List<T> {
        val result: MutableList<T> = mutableListOf()
        val t = object : TypeToken<T>() {}.type

        this.forEach {
            try {
                result.add(
                    this@BitcoinComService.gson!!.fromJson(it.toString(), t)
                )
            } catch (e: Exception) {
                this@BitcoinComService.logger.error("Got the error while casting to $t", e)
            }
        }

        return result.toList()
    }
*/

}