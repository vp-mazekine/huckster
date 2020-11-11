package com.broxus.huckster.interfaces

interface Strategy {
    //  Launch execution of the strategy
    suspend fun run()
}