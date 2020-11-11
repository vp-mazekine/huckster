package com.broxus.huckster.models

import com.google.gson.annotations.Expose

/**
 * Input model for trading strategy builder
 *
 * @property account Specification of the account to trade on
 * @property configuration Generic trading strategy rules
 * @property strategies List of strategies per target currency
 */
data class StrategyInput(
    @Expose val account: Account,
    @Expose val configuration: Configuration,
    @Expose val strategies: List<Strategy>
)