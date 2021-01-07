package com.broxus.huckster.strategies

import com.broxus.huckster.OrdersQueue
import com.broxus.huckster.interfaces.PriceFeed
import com.broxus.huckster.interfaces.Strategy
import com.broxus.huckster.models.PlaceOrderEvent
import com.broxus.huckster.models.StrategyInput
import com.broxus.huckster.logger2
import com.broxus.nova.client.NovaApiService
import com.broxus.utils.green
import com.broxus.utils.red
import com.broxus.utils.yellow
import kotlinx.coroutines.*
import kotlin.system.exitProcess

class BasicStrategy(
    val strategy: StrategyInput,
    val priceAdapter: PriceFeed
): Strategy {

    override suspend fun run() {
        var offsetPart: Float?
        var currencyPart: Float?
        var structuralPart: Float?
        var basePrice: Float?

        while(true) {
            val thread = GlobalScope.launch {
                //  Flush all current orders
                strategy.strategies.forEach {
                    OrdersQueue.flush(
                        strategy.account.userAddress,
                        strategy.account.addressType,
                        strategy.account.workspaceId,
                        strategy.configuration.sourceCurrency,
                        it.targetCurrency
                    )
                }

                //  Get balance for specific currency from the list of all available ones
                var availableBalance: Float? = null

                NovaApiService.getSpecificUserBalance(
                    strategy.account.userAddress,
                    strategy.account.addressType,
                    strategy.account.workspaceId
                )?.forEach {
                    if (it.currency == strategy.configuration.sourceCurrency) {
                        availableBalance = it.available.toFloat()
                        return@forEach
                    }
                }

                //  Check that the balance is sufficient to run the selected strategy
                if (
                    (availableBalance == null)
                    || (strategy.configuration.hardFloor != null &&
                            availableBalance!! * strategy.configuration.volumeLimit.toFloat()
                            < strategy.configuration.hardFloor.toFloat())
                    || (availableBalance!! * strategy.configuration.volumeLimit.toFloat()
                            < strategy.configuration.minOrderSize.toFloat())
                ) {
                    logger2("Source balance in ${strategy.configuration.sourceCurrency} is insufficient to place orders.\nTerminating now...".red())
                    exitProcess(-1)
                }

                availableBalance = availableBalance?.times(strategy.configuration.volumeLimit.toFloat())

                logger2("Available balance: ".green() + "${availableBalance!!} ${strategy.configuration.sourceCurrency}")

                //  TODO: Perform checks on strategy data validity

                //  Calculate and place orders
                strategy.configuration.placementOffset.forEachIndexed offset@{ offsetIndex, offset ->
                    offsetPart = offset.volumePart.toFloat()

                    strategy.strategies.forEach strategy@{ s ->
                        currencyPart = s.volumePart.toFloat()

                        s.sizeStructure.forEachIndexed size@{ index, size ->
                            structuralPart = size.toFloat()

                            val fromAmount =
                                (availableBalance ?: 0.0F) *
                                        (offsetPart ?: 1.0F) *
                                        (currencyPart ?: 1.0F) *
                                        (structuralPart ?: 1.0F)

                            basePrice = priceAdapter.getPrice(
                                strategy.configuration.sourceCurrency,
                                s.targetCurrency,
                                fromAmount
                            )

                            if (basePrice == null) return@size

                            val toAmount = fromAmount * basePrice!! *
                                    (1.0F + s.spreadStructure[index].toFloat()) //  TODO: Add a check if spread and size structure are of a different length

                            //  In case the order in structure is less then it is allowed by the strategy, it is skipped
                            if (fromAmount < strategy.configuration.minOrderSize.toFloat()) {
                                logger2(
                                    "[$offsetIndex] Order is too small to place: ".yellow() +
                                            "$fromAmount ${strategy.configuration.sourceCurrency} --> $toAmount ${s.targetCurrency}"
                                )
                                return@size
                            }

                            val event = PlaceOrderEvent(
                                strategy.account.userAddress,
                                strategy.account.addressType,
                                strategy.account.workspaceId,
                                fromAmount.toString(),
                                toAmount.toString(),
                                strategy.configuration.sourceCurrency,
                                s.targetCurrency,
                                "Huckster MarketMaker",
                                (offset.offset.toFloat() * 1000).toLong(),
                                (strategy.configuration.refreshInterval.soft.toFloat() * 1000).toLong()
                            )

                            try {
                                launch {
                                    OrdersQueue.enqueue(
                                        event,
                                        offsetIndex
                                    )
                                }
                            } catch(e: Exception) {
                                logger2(
                                    "[$offsetIndex] Coroutine failed to launch at this event:\n$event\n".red() +
                                            "Details:" + e.message + "\n" + e.stackTrace.joinToString("\n").red()
                                )
                            }
                        }
                    }
                }
            }

            delay((strategy.configuration.refreshInterval.hard.toFloat() * 1000).toLong())
            thread.cancelAndJoin()
        }
    }
}