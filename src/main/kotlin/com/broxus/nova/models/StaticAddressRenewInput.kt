package com.broxus.nova.models

import com.broxus.nova.types.AddressType
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * A model for input to the static_addresses/renew method
 *
 * @property currency Currency ticker, e.g. TON
 * @property addressType One of the allowed address types
 * @property userAddress User identifier or the address
 * @property workspaceId Identifier of the workspace
 */
data class StaticAddressRenewInput (
    @SerializedName("currency") @Expose val currency: String,
    @SerializedName("addressType") @Expose val addressType: AddressType,
    @SerializedName("userAddress") @Expose val userAddress: String,
    @SerializedName("workspaceId") @Expose val workspaceId: String
)