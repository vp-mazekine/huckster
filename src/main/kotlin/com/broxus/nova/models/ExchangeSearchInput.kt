package com.broxus.nova.models

import com.broxus.nova.types.AddressType
import com.broxus.nova.types.ExchangeOrderStateType
import com.broxus.nova.types.OrderSideType
import com.google.gson.annotations.Expose

/**
 * Input model for /users/exchanges method
 *
 * @property id Id of exchange order. UUID ver. 4 rfc
 * @property userAddress User address type. Case sensitive!
 * @property addressType The unique address of the user. Which value to specify the address depends on the addressType. Case sensitive!
 * @property workspaceId Id of workspace. UUID ver. 4 rfc
 * @property base Сurrency identifier or ticker. Can contain more than 3 letters.
 * @property counter Сurrency identifier or ticker. Can contain more than 3 letters.
 * @property orderSide
 * @property state Current Exchange Order state.
 * @property isAlive For open orders this flag is true
 * @property offset
 * @property limit Max 500
 * @property from Unix timestamp.
 * @property to Unix timestamp.
 */
data class ExchangeSearchInput (
    @Expose val id: String?,
    @Expose val userAddress: String,
    @Expose val addressType: String,
    @Expose val workspaceId: String?,
    @Expose val base: String?,
    @Expose val counter: String?,
    @Expose val orderSide: OrderSideType?,
    @Expose val state: ExchangeOrderStateType?,
    @Expose val isAlive: Boolean?,
    @Expose val offset: Number?,
    @Expose val limit: Number?,
    @Expose val from: Long?,
    @Expose val to: Long?
)