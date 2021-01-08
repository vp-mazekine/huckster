package com.broxus.huckster.models

import com.google.gson.annotations.Expose

data class NotificationConfig (
    @Expose val soft: String?,
    @Expose val hard: String?
)
