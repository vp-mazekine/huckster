package io.gate.interfaces

import com.bitcoin.types.CandlesPeriods
import com.bitcoin.types.SortingOrder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*
import java.time.OffsetDateTime

interface GateIoInterface {
    @GET("public/candles")
    fun getCandles(
        @Query("period") period: CandlesPeriods?,
        @Query("sort") sort: SortingOrder?,
        @Query("from") from: OffsetDateTime?,
        @Query("till") till: OffsetDateTime?,
        @Query("limit") limit: Int?,
        @Query("offset") offset: Int?,
        @Query("symbols") symbols: String?
    ): Call<JsonObject>

    @GET("public/symbol")
    fun getSymbolDetails(
        @Query("symbols") symbols: String?
    ): Call<JsonArray>

    @GET("public/orderbook")
    fun getOrderBook(
        @Query("limit") limit: Int? = 100,
        @Query("symbols") symbols: String? = null
    ): Call<JsonObject>
}