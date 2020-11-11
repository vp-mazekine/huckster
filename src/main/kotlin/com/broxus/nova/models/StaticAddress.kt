package com.broxus.nova.models

import com.broxus.nova.types.AddressType
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class StaticAddress (
    @SerializedName("id") @Expose val id: String,
    @SerializedName("addressType") @Expose val addressType: AddressType,
    @SerializedName("userAddress") @Expose val userAddress: String,
    @SerializedName("workspaceId") @Expose val workspaceId: String,
    @SerializedName("currency") @Expose val currency: String,
    @SerializedName("blockchainAddress") @Expose val blockchainAddress: String,
    @SerializedName("createdAt") @Expose val createdAt: Long
)