package com.broxus.nova.adapters.retrofit

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import com.broxus.nova.models.ErrorDescription
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import okhttp3.Request
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

private class EitherCall<R>(
        private val delegate: Call<R>,
        private val successType: Type
    ) : Call<Either<ErrorDescription, R>> {

        override fun enqueue(callback: Callback<Either<ErrorDescription, R>>) = delegate.enqueue(
            object : Callback<R> {

                override fun onResponse(call: Call<R>, response: Response<R>) {
                    callback.onResponse(this@EitherCall, Response.success(response.toEither()))
                }

                private fun Response<R>.toEither(): Either<ErrorDescription, R> {
                    val gson = GsonBuilder()
                        .setLenient()
                        .create()

                    // Http error response (4xx - 5xx)
                    if (!isSuccessful) {
                        val errorBody = gson.fromJson(errorBody().toString(), ErrorDescription::class.java)
                        return Left(errorBody)
                    }

                    // Http success response with body
                    body()?.let { body ->
                        val result = gson.fromJson(body.toString(), object : TypeToken<R>() {}.type::class.java)
                        //val result = gson.fromJson(body.toString(), class<R>::class.java)
                        return Right(body)
                    }

                    // if we defined Unit as success type it means we expected no response body
                    // e.g. in case of 204 No Content
                    return if (successType == Unit::class.java) {
                        @Suppress("UNCHECKED_CAST")
                        Right(Unit) as Either<ErrorDescription, R>
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        Left(UnknownError("Response body was null")) as Either<ErrorDescription, R>
                    }
                }

                override fun onFailure(call: Call<R>, throwable: Throwable) {
                    val error = ErrorDescription(throwable.localizedMessage) /*when (throwable) {
                        is IOException -> {
                            NetworkError(throwable)
                        }
                        else -> UnknownApiError(throwable)
                    }*/
                    callback.onResponse(this@EitherCall, Response.success(Left(error)))
                }
            }
        )

    override fun clone(): Call<Either<ErrorDescription, R>> {
        TODO("Not yet implemented")
    }

    override fun execute(): Response<Either<ErrorDescription, R>> {
        TODO("Not yet implemented")
    }

    override fun isExecuted(): Boolean {
        TODO("Not yet implemented")
    }

    override fun cancel() {
        TODO("Not yet implemented")
    }

    override fun isCanceled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun request(): Request {
        TODO("Not yet implemented")
    }


    // override remaining methods - trivial
}