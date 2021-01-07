package com.broxus.utils

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Model returned by Nova API in case of error
 *
 * @property description
 * @property code
 * @property fieldErrors
 */
data class ErrorDescription (
    @SerializedName("description") @Expose val description: String? = null,
    @SerializedName("code") @Expose val code: String? = null,
    @SerializedName("fieldErrors") @Expose val fieldErrors: Map<String, Array<ValidationError>>? = mapOf()
)

data class ValidationError(
    val code: String,
    val message: String? = "",
    val params: Map<String, String>? = mapOf()
)