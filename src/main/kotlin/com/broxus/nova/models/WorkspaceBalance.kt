package com.broxus.nova.models

import com.broxus.nova.types.AddressType
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class WorkspaceBalance (
    @SerializedName("addressType") @Expose val addressType: AddressType,
    @SerializedName("userAddress") @Expose val userAddress: String,
    @SerializedName("workspaceId") @Expose val workspaceId: String,
    @SerializedName("currency") @Expose val currency: String,
    @SerializedName("total") @Expose val total: Number,
    @SerializedName("number") @Expose val frozen: Number,
    @SerializedName("available") @Expose val available: Number
)