package com.broxus.huckster.interfaces

import com.broxus.huckster.logger2
import com.broxus.huckster.prices.models.Rate
import kotlinx.coroutines.*

interface IPriceFeed {
    /**
     * Get rate for a specified currency pair
     *
     * @param fromCurrency
     * @param toCurrency
     * @param volume
     * @return Requested rate, or null if it is unavailable
     */
    fun getPrice(fromCurrency: String, toCurrency: String, volume: Float?): Float?

    fun MutableList<Rate>.findRateIndex(fromCurrency: String, toCurrency: String): Int? {
        this.forEachIndexed { index, rate ->
            if (rate.fromCurrency == fromCurrency && rate.toCurrency == toCurrency) return index
            if (rate.fromCurrency == toCurrency && rate.toCurrency == fromCurrency) return -index
        }

        return null
    }

    /**
     * Finds the best exchange route for a given pair
     *
     * @param fromCurrency
     * @param toCurrency
     * @return  Execution route and amount of resulting currency
     */
    fun MutableList<Rate>.findOptimalRoute(fromCurrency: String, toCurrency: String, amount: Float): Pair<List<DirectionalEdge>, Float>? {
        if (amount <= 0F) return null

        val vertices: MutableList<String> = mutableListOf()

        //  Init vertices
        this.forEach { rate ->
            if(!vertices.contains(rate.fromCurrency)) vertices.add(rate.fromCurrency)
            if(!vertices.contains(rate.toCurrency)) vertices.add(rate.toCurrency)
        }

        if (!vertices.contains(fromCurrency) || !vertices.contains(toCurrency)) return null

        val edges: MutableList<DirectionalEdge> = mutableListOf()

        //  Init directional edges
        this.forEachIndexed { index, rate ->
            if (rate.rate <= 0F) return@forEachIndexed

            val edgeDirect = DirectionalEdge(
                index, rate.fromCurrency, rate.toCurrency, Direction.STRAIGHT, rate.rate
            )
            val edgeReverse = DirectionalEdge(
                index, rate.toCurrency, rate.fromCurrency, Direction.REVERSE, 1 / rate.rate
            )

            if(!edges.contains(edgeDirect)) edges.add(edgeDirect)
            if(!edges.contains(edgeReverse)) edges.add(edgeReverse)
        }

        //  Attempt finding possible execution routes
        var routes: MutableList<MutableList<DirectionalEdge>> = runBlocking {
            findSubRoute(fromCurrency, toCurrency, edges)
        } ?: return null

        routes = routes.map{ it.asReversed() }.toMutableList()

        var product: Float? = null
        var optimalRoute: List<DirectionalEdge>? = null

        routes.forEach { route ->
            var tempProduct = 1F
            route.forEach { tempProduct *= it.length }

            if(product == null) {
                product = tempProduct
                optimalRoute = route
            } else {
                if(tempProduct < product!!) {
                    product = tempProduct
                    optimalRoute = route
                }
            }
        }

        if(optimalRoute == null || product == null) return null

        return Pair(optimalRoute!!, product!!)
    }

    /**
     * Fractal route builder
     *
     * @param from  Source currency
     * @param to    Target currency
     * @param edges List of graph edges
     * @return
     */
    private suspend fun findSubRoute(
        from: String,
        to: String,
        edges: List<DirectionalEdge>
    ): MutableList<MutableList<DirectionalEdge>>? {
        //  Check that at least one edge contains the starting point
        edges.firstOrNull { it.from == from } ?: return null

        //  Check that at least one edge contains the final point
        edges.firstOrNull { it.to == to } ?: return null

        val result: MutableList<MutableList<DirectionalEdge>> = mutableListOf()

        //  Checking if a direct route is available
        edges
            .firstOrNull { it.from == from && it.to == to }
            ?.let {
                result.add(mutableListOf<DirectionalEdge>(it))
            }

        //  Iterate through suitable edges
        val deferred = withContext(Dispatchers.Default) {
            edges
                .filter { it.from == from }
                .map { edge ->
                    async {
                        findSubRoute(edge.to, to, edges.filter { it.from != edge.from && it.to != edge.from })?.forEach { route ->
                            result.add(
                                route.apply {
                                    add(edge)
                                }
                            )
                        }
                    }
                }
        }

        deferred.awaitAll()

        return if(result.isEmpty()) null else result
    }

    enum class Direction { STRAIGHT, REVERSE }

    /**
     * Directional edge for building the execution graph
     *
     * @property index  Index of the rate in the rate storage
     * @property from   Source currency
     * @property to     Target currency
     * @property direction  Exchange direction (compared to the rate storage)
     * @property length Length of the graph edge (price of the asset)
     * @constructor Create empty Directional edge
     */
    data class DirectionalEdge(
        val index: Int,
        val from: String,
        val to: String,
        val direction: Direction,
        val length: Float
    )
}