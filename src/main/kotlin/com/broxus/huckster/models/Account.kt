package com.broxus.huckster.models

import com.broxus.nova.types.AddressType
import com.google.gson.annotations.Expose

/**
 * Trading account specification
 *
 * @property userAddress User account address
 * @property addressType User account address type
 * @property workspaceId Unique workspace identifier, UUID v.4 rfc
 */
data class Account(
    @Expose val userAddress: String,
    @Expose val addressType: String,
    @Expose val workspaceId: String
)