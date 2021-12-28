package com.broxus.huckster.interfaces

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import com.broxus.utils.ErrorDescription
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import retrofit2.Response

abstract class Unfoldable {
    protected val gson: Gson = GsonBuilder().setLenient().setPrettyPrinting().create()
    protected val logger: Logger =  LogManager.getLogger(this::class.java)

    /**
     * Parse the server response and convert it to the appropriate type or return error description
     *
     * @param T Expected response type
     * @param r Response object
     * @param t Expected response type's class
     * @return Either<ErrorDescription, T>
     */
    protected inline fun <reified T> unfoldResponse(
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
                        Right(this.gson.fromJson(r.body()!!, t))
                    } else {
                        //  Return the error
                        Left(this.gson.fromJson(r.body()!!, ErrorDescription::class.java))
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

    /**
     * Transform the returned LinkedTreeMap to the desired type
     *
     * @param T Type to cast
     * @return List<T>
     */
    protected inline fun <reified T> List<*>.castJsonArrayToType(): List<T> {
        val result: MutableList<T> = mutableListOf()
        val t = object : TypeToken<T>() {}.type

        this.forEach {
            try {
                result.add(
                    gson.fromJson(it.toString(), t)
                )
            } catch (e: Exception) {
                logger.error("Got the error while casting to $t", e)
            }
        }

        return result.toList()
    }
}