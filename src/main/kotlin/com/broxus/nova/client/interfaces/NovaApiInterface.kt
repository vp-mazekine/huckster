package com.broxus.nova.client.interfaces

import com.broxus.nova.models.*
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

interface NovaApiInterface {

    @POST("/v1/static_addresses/renew")
    fun getStaticAddressByUser(
        @Body request: StaticAddressRenewInput,
        @Header("api-key") apiKey: String,
        @Header("nonce") nonce: Long,
        @Header("sign") signature: String
    ): Call<JsonObject>

    @POST("/v1/users/balances")
    fun getWorkspaceUsersBalances(
        @Body request: WorkspaceBalanceInput,
        @Header("api-key") apiKey: String,
        @Header("nonce") nonce: Long,
        @Header("sign") signature: String
    ): Call<JsonArray>

    @GET("/v1/meta/currencies_pairs")
    fun getCurrenciesPairs(
        @Header("api-key") apiKey: String
    ): Call<JsonArray>

    @POST("/v1/users/balance")
    fun getSpecificUserBalance(
        @Body request: UserAccountInput,
        @Header("api-key") apiKey: String,
        @Header("nonce") nonce: Long,
        @Header("sign") signature: String
    ): Call<JsonArray>

    @POST("/v1/exchange/limit")
    fun createLimitOrder(
        @Body request: ExchangeLimitInput,
        @Header("api-key") apiKey: String,
        @Header("nonce") nonce: Long,
        @Header("sign") signature: String
    ): Call<JsonObject>

    @POST("/v1/users/exchanges")
    fun getSpecificUserOrders(
        @Body request: ExchangeSearchInput,
        @Header("api-key") apiKey: String,
        @Header("nonce") nonce: Long,
        @Header("sign") signature: String
    ): Call<JsonArray>

    @DELETE("/v1/exchange/limit/{transactionId}")
    fun cancelOrder(
        @Path("transactionId") transactionId: String,
        @Header("api-key") apiKey: String
    ): Call<String>

    @POST("/v1/exchange/order_book")
    fun getOrderBook(
        @Body request: ExchangeOrderBookInput,
        @Header("api-key") apiKey: String,
        @Header("nonce") nonce: Long,
        @Header("sign") signature: String
    ): Call<JsonObject>
}