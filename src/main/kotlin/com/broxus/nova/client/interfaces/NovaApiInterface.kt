package com.broxus.nova.client.interfaces

import com.broxus.nova.models.*
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

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

    companion object {
        /**
         * Creates a Retrofit instance of NovaApiService
         *
         * @param key Nova API key provided by Broxus
         * @param secret Nova API secret for signing requests
         * @return
         */
        fun create(baseUrl: String, key: String, secret: String): NovaApiInterface {
            val retrofit = Retrofit.Builder()
                //.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(baseUrl)
                .build()

            return retrofit.create(NovaApiInterface::class.java)
        }

        /**
         * Signs the request for Broxus
         *
         * @param secret A secret key issued to sign messages for Broxus
         * @param method Path to a method called
         * @param content Request body to be sent
         * @return
         */
        fun sign(secret: String, method: String, content: String): Pair<Long, String> {
            val nonce = System.currentTimeMillis()
            val salt: String = nonce.toString() + method + content
            val secretKeySpec = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(secretKeySpec)
            val signature: ByteArray = mac.doFinal(salt.toByteArray())
            val base64: String = Base64.getEncoder().encodeToString(signature)
            return Pair(nonce, base64)
        }
    }
}