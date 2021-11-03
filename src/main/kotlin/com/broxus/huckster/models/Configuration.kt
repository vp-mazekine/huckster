package com.broxus.huckster.models

import com.google.gson.annotations.Expose

/**
 * Basic configuration of the trading strategy
 *
 * @property adapter Name of the strategy adapter to use
 * @property hardFloor The amount on the account that should not participate in trading (overrides volumeLimit). If null, tradeable amount is defined by volumeLimit
 * @property minOrderSize Minimal allowed order lot
 * @property placementOffset Defines the shifted execution rules
 * @property refreshInterval Indicates how often the orderbook should be rebalanced
 * @property sourceCurrency Trading account currency
 * @property volumeLimit Relative proportion of account balance that can be tradeable
 */
data class Configuration(
    @Expose val adapter: String? = "basic",
    @Expose val hardFloor: String?,
    @Expose val minOrderSize: String,
    @Expose val placementOffset: List<PlacementOffset>,
    @Expose val refreshInterval: RefreshInterval,
    @Expose val sourceCurrency: String,
    @Expose val volumeLimit: String,
    @Expose val notification: NotificationConfig?,
    @Expose val faultTolerance: Float? = null
)