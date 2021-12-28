package io.gate.models

import com.google.gson.annotations.Expose

data class GateIoConfiguration (
    @Expose val apiPath: String,
    @Expose val refreshRate: Int? = 10,
    @Expose val symbols: List<String>,
    @Expose val dictionary: Map<String, String>?
)
