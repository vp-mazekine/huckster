package com.broxus.nova.client

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import com.broxus.huckster.logger2
import com.broxus.nova.client.interfaces.NovaApiInterface
import com.broxus.nova.models.*
import com.broxus.nova.types.AddressType
import com.broxus.nova.types.ExchangeOrderStateType
import com.broxus.nova.types.OrderSideType
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.broxus.utils.green
import com.broxus.utils.red
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object NovaApiService {
    private var api: NovaApiInterface? = null
    private var apiConfig: ApiConfig? = null
    private var gson: Gson? = null
    private val logger: Logger = getLogger(NovaApiService::class.java)

    /**
     * Creates a Retrofit instance of NovaApiService
     *
     * @param config API connection configuration
     * @return
     */
    fun init(config: ApiConfig) {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(config.apiPath)
            .build()

        api = retrofit.create(NovaApiInterface::class.java)
        apiConfig = config
        gson = Gson()
    }

    /**
     * Signs the request for Broxus
     *
     * @param method Path to a method called
     * @param content Request body to be sent
     * @return
     */
    private fun sign(method: String, content: String): Pair<Long, String> {
        val nonce = System.currentTimeMillis()
        val salt: String = nonce.toString() + method + content
        val secretKeySpec = SecretKeySpec(apiConfig!!.apiSecret.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)
        val signature: ByteArray = mac.doFinal(salt.toByteArray())
        val base64: String = Base64.getEncoder().encodeToString(signature)
        return Pair(nonce, base64)
    }

    /**
     * Parse the server response and convert it to the appropriate type or return error description
     *
     * @param T Expected response type
     * @param r Response object
     * @param t Expected response type's class
     * @return Either<ErrorDescription, T>
     */
    private inline fun <reified T> unfoldResponse(r: Response<out JsonElement>, t: Class<T>): Either<ErrorDescription, T>? {
        return try {
            when(r.body()) {
                //  If the response body is void
                null -> Left(
                    when(r.message()){
                        //  Error without details
                        null -> ErrorDescription(
                            when(r.code()){
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
                    })

                //  If the server returned the content
                else -> {
                    if(r.isSuccessful) {
                        //  Try to apply the requested model
                        Right(gson!!.fromJson(r.body()!!, t))
                    } else {
                        //  Return the error
                        Left(gson!!.fromJson(r.body()!!, ErrorDescription::class.java))
                    }
                }
            }
        } catch(e: Exception) {
            //  Handle unexpected errors
            Left(
                ErrorDescription(
                    e.localizedMessage,
                    r.code().toString()
                )
            )
        }
    }

    /**
     * Transform the returned LinkedTreeMap to the desired type
     *
     * @param T Type to cast
     * @return List<T>
     */
    private inline fun <reified T> List<*>.castJsonArrayToType(): List<T> {
        val result: MutableList<T> = mutableListOf()
        val t = object : TypeToken<T>(){}.type

        this.forEach {
            try {
                result.add(
                    gson!!.fromJson(it.toString(), t)
                )
            } catch (e: Exception) {
                logger.error("Got the error while casting to $t", e)
            }
        }

        return result.toList()
    }

    /**
     * Get static address by the specified user or generate a new one
     *
     * @param currency
     * @param addressType
     * @param userAddress
     * @param workspaceId
     * @return
     */
    fun getStaticAddressByUser(
        currency: String,
        userAddress: String,
        addressType: AddressType,
        workspaceId: String
    ): StaticAddress? {

        val result: Response<JsonObject>

        try {
            //  Input model for the REST call
            val input = StaticAddressRenewInput(
                            currency,
                            addressType,
                            userAddress,
                            workspaceId
                        )

            //  Prepare the request signature
            val (nonce, signature) = sign(
                "/v1/static_addresses/renew",
                gson!!.toJson(input).toString()
            )

            //  Perform request
            result = api!!.getStaticAddressByUser(input, apiConfig!!.apiKey, nonce, signature).execute()
        } catch(e: Exception) {
            logger.error("Nova API service was not properly initialized!", e)
            return null
        }

        //  Transform server response
        unfoldResponse(result, StaticAddress::class.java).apply {
            return when(this) {
                is Either.Right -> this.b
                is Either.Left -> {
                    logger.error(this.a.toString())
                    null
                }
                else -> null
            }
        }
    }

    /**
     * Returns balances of all workspace users
     *
     * @param workspaceId Unique identifier of the workspace
     * @return WorkspaceBalance object or null in case of error
     */
    @Suppress("UNCHECKED_CAST")
    fun getWorkspaceUsersBalances(workspaceId: String): List<WorkspaceBalance>? {

        val result: Response<JsonArray>

        try {
            //  Input model for the REST call
            val input = WorkspaceBalanceInput(workspaceId)

            //  Prepare the request signature
            val (nonce, signature) = sign(
                "/v1/users/balances",
                gson!!.toJson(input).toString()
            )

            //  Perform request
            result = api!!.getWorkspaceUsersBalances(input, apiConfig!!.apiKey, nonce, signature).execute()
        } catch(e: Exception) {
            logger.error("Nova API service was not properly initialized!", e)
            return null
        }

        //  Transform server response
        unfoldResponse(result, List::class.java).apply {
            return when(this) {
                is Either.Right -> this.b.castJsonArrayToType<WorkspaceBalance>()
                is Either.Left -> {
                    logger.error(this.a.toString())
                    null
                }
                else -> null
            }
        }
    }

    /**
     * This method allows you to see which cryptocurrency trading pairs can be exchanged.
     *
     * @return List of CurrenciesPairMeta items
     */
    @Suppress("UNCHECKED_CAST")
    fun getCurrenciesPairs(): List<CurrenciesPairMeta>? {
        val result: Response<JsonArray>

        try {
             //  Perform request
            result = api!!.getCurrenciesPairs(apiConfig!!.apiKey).execute()
        } catch(e: Exception) {
            logger.error("Nova API service was not properly initialized!", e)
            return null
        }

        //  Transform server response
        unfoldResponse(result, List::class.java).apply {
            return when(this) {
                is Either.Right -> this.b.castJsonArrayToType<CurrenciesPairMeta>()
                is Either.Left -> {
                    logger.error(this.a.toString())
                    null
                }
                else -> null
            }
        }
    }

    /**
     * Returns balance of the specific user in different currencies
     *
     * @param userAddress The unique address of the user. Which value to specify the address depends on the addressType. Case sensitive!
     * @param addressType User address type. Case sensitive!
     * @param workspaceId Id of workspace. UUID ver. 4 rfc
     * @return
     */
    @Suppress("UNCHECKED_CAST")
    fun getSpecificUserBalance(
        userAddress: String,
        addressType: AddressType,
        workspaceId: String? = null
    ): List<AccountBalance>? {

        val result: Response<JsonArray>

        try {
            //  Input model for the REST call
            val input = UserAccountInput(userAddress, addressType, workspaceId)

            //  Prepare the request signature
            val (nonce, signature) = sign(
                "/v1/users/balance",
                gson!!.toJson(input).toString()
            )

            //  Perform request
            result = api!!.getSpecificUserBalance(input, apiConfig!!.apiKey, nonce, signature).execute()
        } catch(e: Exception) {
            logger.error("Nova API service was not properly initialized!", e)
            return null
        }

        //  Transform server response
        unfoldResponse(result, List::class.java).apply {
            return when(this) {
                is Either.Right -> this.b.castJsonArrayToType<AccountBalance>()
                is Either.Left -> {
                    logger.error(this.a.toString())
                    null
                }
                else -> null
            }
        }
    }

    /**
     * Returns all orders of the specific user with filter
     *
     * @param id Id of exchange order. UUID ver. 4 rfc
     * @param userAddress User address type. Case sensitive!
     * @param addressType The unique address of the user. Which value to specify the address depends on the addressType. Case sensitive!
     * @param workspaceId Id of workspace. UUID ver. 4 rfc
     * @param base Сurrency identifier or ticker. Can contain more than 3 letters.
     * @param counter Сurrency identifier or ticker. Can contain more than 3 letters.
     * @param orderSide
     * @param state Current Exchange Order state.
     * @param isAlive For open orders this flag is true
     * @param offset
     * @param limit Max 500
     * @param from Unix timestamp in milliseconds
     * @param to Unix timestamp in milliseconds
     *
     * @return List of Exchange items
     */
    @Suppress("UNCHECKED_CAST")
    fun getSpecificUserOrders(
        id: String? = null,
        userAddress: String,
        addressType: AddressType,
        workspaceId: String? = null,
        base: String? = null,
        counter: String? = null,
        orderSide: OrderSideType? = null,
        state: ExchangeOrderStateType? = null,
        isAlive: Boolean? = null,
        offset: Number? = null,
        limit: Number? = null,
        from: Long? = null,
        to: Long? = null
    ): List<Exchange>? {

        val result: Response<JsonArray>

        try {
            //  Input model for the REST call
            val input = ExchangeSearchInput(id, userAddress, addressType, workspaceId, base, counter, orderSide, state, isAlive, offset, limit, from, to)

            //  Prepare the request signature
            val (nonce, signature) = sign(
                "/v1/users/exchanges",
                gson!!.toJson(input).toString()
            )

            //  Perform request
            result = api!!.getSpecificUserOrders(input, apiConfig!!.apiKey, nonce, signature).execute()
        } catch(e: Exception) {
            logger.error("Nova API service was not properly initialized!", e)
            return null
        }

        //  Transform server response
        unfoldResponse(result, List::class.java).apply {
            return when(this) {
                is Either.Right -> this.b.castJsonArrayToType<Exchange>()
                is Either.Left -> {
                    logger.error(this.a.toString())
                    null
                }
                else -> null
            }
        }
    }

    /**
     * Creates a limit order from a specific user
     *
     * @param id Id of transaction. UUID ver. 4 rfc
     * @param userAddress User address type. Case sensitive!
     * @param addressType The unique address of the user. Which value to specify the address depends on the addressType. Case sensitive!
     * @param workspaceId Id of workspace. UUID ver. 4 rfc
     * @param from Сurrency identifier or ticker. Can contain more than 3 letters.
     * @param to Сurrency identifier or ticker. Can contain more than 3 letters.
     * @param fromValue Amount of currency. Positive floating point number.
     * @param toValue Amount of currency. Positive floating point number.
     * @param applicationId Id of application. Random string
     * @return
     */
    fun createLimitOrder(
        id: String,
        userAddress: String,
        addressType: AddressType,
        workspaceId: String? = null,
        from: String,
        to: String,
        fromValue: String,
        toValue: String,
        applicationId: String? = null
    ): ExchangeTransactionId? {

        val result: Response<JsonObject>

        try {
            //  Input model for the REST call
            val input = ExchangeLimitInput(id, userAddress, addressType, workspaceId, from, to, fromValue, toValue, applicationId)

            //  Prepare the request signature
            val (nonce, signature) = sign(
                "/v1/exchange/limit",
                gson!!.toJson(input).toString()
            )

            //  Perform request
            result = api!!.createLimitOrder(input, apiConfig!!.apiKey, nonce, signature).execute()
        } catch(e: Exception) {
            logger.error("Nova API service was not properly initialized!", e)
            return null
        }

        //  Transform server response
        unfoldResponse(result, ExchangeTransactionId::class.java).apply {
            return when(this) {
                is Either.Right -> this.b
                is Either.Left -> {
                    logger.error(this.a.toString())
                    null
                }
                else -> null
            }
        }
    }

    /**
     * Cancels selected order
     *
     * @param transactionId
     * @return
     */
    fun cancelOrder(transactionId: String): Boolean {

        val result: Response<String>

        try {
            //  Perform request
            result = api!!.cancelOrder(transactionId, apiConfig!!.apiKey).execute()
        } catch(e: Exception) {
            logger.error("Nova API service was not properly initialized!", e)
            return false
        }

        if(result.isSuccessful){
            logger2("Cancelled order $transactionId".green())
        }else{
            logger.error(ErrorDescription(result.message(), result.code().toString()).toString().red())
        }

        return result.isSuccessful
    }

    fun getOrderBook(
        base: String,
        counter: String,
        workspaceId: String? = null
    ): ExchangeOrderBook? {

        val result: Response<JsonObject>

        try {
            //  Input model for the REST call
            val input = ExchangeOrderBookInput(workspaceId, base, counter)

            //  Prepare the request signature
            val (nonce, signature) = sign(
                "/v1/exchange/order_book",
                gson!!.toJson(input).toString()
            )

            //  Perform request
            result = api!!.getOrderBook(input, apiConfig!!.apiKey, nonce, signature).execute()
        } catch(e: Exception) {
            logger.error("Nova API service was not properly initialized!", e)
            return null
        }

        //  Transform server response
        unfoldResponse(result, ExchangeOrderBook::class.java).apply {
            return when(this) {
                is Either.Right -> this.b
                is Either.Left -> {
                    logger.error(this.a.toString())
                    null
                }
                else -> null
            }
        }
    }

}