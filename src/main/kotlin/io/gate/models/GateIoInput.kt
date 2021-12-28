package io.gate.models

import com.google.gson.annotations.Expose

data class GateIoInput (
    @Expose val adapter: String,
    @Expose val configuration: GateIoConfiguration
)