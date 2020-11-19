package com.broxus.nova.models

import com.broxus.nova.types.AddressType
import com.google.gson.annotations.Expose

/**
 * Input model for /exchange/limit call
 *
 * @property id Id of transaction. UUID ver. 4 rfc
 * @property userAddress User address type. Case sensitive!
 * @property addressType The unique address of the user. Which value to specify the address depends on the addressType. Case sensitive!
 * @property workspaceId Id of workspace. UUID ver. 4 rfc
 * @property from Сurrency identifier or ticker. Can contain more than 3 letters.
 * @property to Сurrency identifier or ticker. Can contain more than 3 letters.
 * @property fromValue Amount of currency. Positive floating point number.
 * @property toValue Amount of currency. Positive floating point number
 * @property applicationId Id of application. Random string
 */
data class ExchangeLimitInput (
    @Expose val id: String,
    @Expose val userAddress: String,
    @Expose val addressType: String,
    @Expose val workspaceId: String?,
    @Expose val from: String,
    @Expose val to: String,
    @Expose val fromValue: String,
    @Expose val toValue: String,
    @Expose val applicationId: String?
)