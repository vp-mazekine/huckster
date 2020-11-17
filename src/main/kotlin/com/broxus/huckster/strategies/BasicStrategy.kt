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
                OrdersQueue.flush(
                    strategy.account.userAddress,
                    strategy.account.addressType,
                    strategy.account.workspaceId,
                    currency = strategy.configuration.sourceCurrency
                )

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

                logger2("Available balance: ".green() + "${availableBalance!!} ${strategy.configuration.sourceCurrency}")

                var remainingBalance = availableBalance

                //  TODO: Perform checks on strategy data validity

                //  Calculate and place orders
                strategy.configuration.placementOffset.forEachIndexed offset@{ offsetIndex, offset ->
                    offsetPart = offset.volumePart.toFloat()

                    strategy.strategies.forEach strategy@{ s ->
                        currencyPart = s.volumePart.toFloat()

                        s.sizeStructure.forEachIndexed size@{ index, size ->
                            structuralPart = size.toFloat()

                            val fromAmount =
                                (remainingBalance ?: 0.0F) *
                                        (offsetPart ?: 1.0F) *
                                        (currencyPart ?: 1.0F) *
                                        (structuralPart ?: 1.0F)

                            basePrice = priceAdapter.getPrice(
                                strategy.configuration.sourceCurrency,
                                s.targetCurrency,
                                fromAmount
                            )

                            if (basePrice == null) return@size

                            //logger2("Basic exchange price: 1 ${strategy.configuration.sourceCurrency} = $basePrice ${s.targetCurrency}")

                            val toAmount = fromAmount * basePrice!! *
                                    (1.0F + s.spreadStructure[index].toFloat()) //  TODO: Add a check if spread and size structure are of a different length

                            //  In case the order in structure is less then it is allowed by the strategy, it is skipped
                            if (fromAmount < strategy.configuration.minOrderSize.toFloat()) {
                                logger2(
                                    "Order too small to place: ".yellow() +
                                            "$fromAmount ${strategy.configuration.sourceCurrency} --> $toAmount ${s.targetCurrency}"
                                )
                                return@size
                            }

                            remainingBalance = remainingBalance?.minus(fromAmount)

                            launch {
                                OrdersQueue.enqueue(
                                    PlaceOrderEvent(
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
                                    ),
                                    offsetIndex
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