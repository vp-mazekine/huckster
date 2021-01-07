package com.broxus.huckster

import com.broxus.huckster.models.StrategyInput
import com.broxus.huckster.prices.adapters.BitcoinComRates
import com.broxus.huckster.prices.adapters.FixedRate
import com.broxus.huckster.prices.adapters.GoogleSheetRates
import com.broxus.huckster.strategies.BasicStrategy
import com.broxus.nova.client.NovaApiService
import com.broxus.nova.models.ApiConfig
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.broxus.utils.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    var command = ""
    var keysPath = ""
    var strategyPath = ""
    var priceAdapterPath = ""
    var priceAuthPath = ""
    var base = ""
    var counter = ""
    var refreshInterval = 30
    val version = "0.3 rev.2"

    val greeting =
        "                                                                  \n" +
        "██╗  ██╗██╗   ██╗ ██████╗██╗  ██╗███████╗████████╗███████╗██████╗ \n" +
        "██║  ██║██║   ██║██╔════╝██║ ██╔╝██╔════╝╚══██╔══╝██╔════╝██╔══██╗\n" +
        "███████║██║   ██║██║     █████╔╝ ███████╗   ██║   █████╗  ██████╔╝\n" +
        "██╔══██║██║   ██║██║     ██╔═██╗ ╚════██║   ██║   ██╔══╝  ██╔══██╗\n" +
        "██║  ██║╚██████╔╝╚██████╗██║  ██╗███████║   ██║   ███████╗██║  ██║\n" +
        "╚═╝  ╚═╝ ╚═════╝  ╚═════╝╚═╝  ╚═╝╚══════╝   ╚═╝   ╚══════╝╚═╝  ╚═╝\n" +
        "                                                                  ".yellow()

    val usage =
        "huckster v.$version\n\n".bold() +
        "USAGE:\n".brightGreen() +
        "\thuckster " + "<JOB> ".green() + "[PARAMS]\n".yellow() +
        "\t\n" +
        "\tJOBS:\n".green() +
        "\t\t" + "buyer".italic() + "\t\t\t\t\t\tBuy at the lowest possible price\n" +
        "\t\t" + "seller".italic() + "\t\t\t\t\t\tSell at the highest possible price\n" +
        "\t\t" + "orderbook".italic() + "\t\t\t\t\tDisplay available orderbook\n" +
        "\t\n" +
        "\tPARAMETERS:\n".yellow() +
        "\tOrders placement:\n".underline() +
        "\t\t-k, --keys <FILE>".italic() +
                "\t\t\tKeys to access Broxus Nova platform\n" +
        "\t\t-s, --strategy <FILE>".italic() +
                "\t\tStrategy configuration\n" +
        "\t\t-pad, --priceAdapter <FILE>".italic() +
                "\tPrice adapter configuration\n" +
        "\t\t[-pau, --priceAuth <FILE>]".italic() +
                "\tPrice adapter authentication file [adapter-dependent]\n\n" +
        "\tOrderbook:\n".underline() +
        "\t\t-p, --pair <PAIR>".italic() +
                "\t\t\tBase and counter currency tickers separated by a delimiter.\n" +
                "\t\t\t\t\t\t\t\t\tSupported delimiters: underscore(_), dash(-) and slash (/)\n" +
        "\t\t-k, --keys <FILE>".italic() +
                "\t\t\tKeys to access Broxus Nova platform\n" +
        "\t\t[-r, --refresh <SECONDS>]".italic() +
                "\tOrderbook refresh rate in seconds. Default: 10"

    val errors: MutableMap<String, MutableList<String>> = mutableMapOf()

    //  Parse command line arguments
    var i = 0
    while(i <= args.lastIndex) {
        when(args[i].toLowerCase()) {
            //  Subcommand
            "seller", "buyer", "orderbook" -> {
                command = args[i].toLowerCase()
            }

            //  Configuration to connect to Broxus
            "--keys", "-k" -> {
                if(i < args.lastIndex) {
                    if(File(args[i + 1]).exists()){
                        keysPath = args[i + 1]
                    } else {
                        if(!errors.containsKey("wrong_file")) errors["wrong_file"] = mutableListOf()

                        errors["wrong_file"]?.add(args[i+1].red())
                    }

                    i++
                }
            }

            //  Strategy configuration
            "--strategy", "-s" -> {
                if(i < args.lastIndex) {
                    if(File(args[i + 1]).exists()){
                        strategyPath = args[i + 1]
                    } else {
                        if(!errors.containsKey("wrong_file")) errors["wrong_file"] = mutableListOf()

                        errors["wrong_file"]?.add(args[i+1].red())
                    }

                    i++
                }
            }

            //  Configuration of the price adapter
            "--priceadapter", "-pad" -> {
                if(i < args.lastIndex) {
                    if(File(args[i + 1]).exists()){
                        priceAdapterPath = args[i + 1]
                    } else {
                        if(!errors.containsKey("wrong_file")) errors["wrong_file"] = mutableListOf()

                        errors["wrong_file"]?.add(args[i+1].red())
                    }

                    i++
                }
            }

            //  Configuration of the price adapter
            "--priceauth", "-pau" -> {
                if(i < args.lastIndex) {
                    if(File(args[i + 1]).exists()){
                        priceAuthPath = args[i + 1]
                    } else {
                        if(!errors.containsKey("wrong_file")) errors["wrong_file"] = mutableListOf()

                        errors["wrong_file"]?.add(args[i+1].red())
                    }

                    i++
                }
            }

            "--pair", "-p" -> {
                if(i < args.lastIndex) {
                    args[i + 1].split("/", "-", "_", ignoreCase = true, limit = 2).also {
                        if(it.count() == 2) {
                            base = it[0].toUpperCase()
                            counter = it[1].toUpperCase()
                        } else {
                            if(!errors.containsKey("wrong_orderbook")) errors["wrong_orderbook"] = mutableListOf()

                            errors["wrong_orderbook"]?.add("Wrong number of arguments to display orderbook")
                        }
                    }

                    i++
                }
            }

            "--refresh", "-r" -> {
                if(i < args.lastIndex) {
                    args[i + 1].toIntOrNull().let {
                        when(it) {
                            null -> {
                                if(!errors.containsKey("wrong_orderbook")) errors["wrong_orderbook"] = mutableListOf()

                                errors["wrong_orderbook"]?.add("Refresh rate is not an integer number")
                            }
                            else -> refreshInterval = it
                        }
                    }

                    i++
                }
            }

            "--help", "-h" -> {
                println(usage)
                exitProcess(-1)
            }

            else -> {
                if(!errors.containsKey("unknown")) errors["unknown"] = mutableListOf()

                errors["unknown"]?.add(args[i].red())
            }
        }
        i++
    }

    when(command) {
        "" -> {
            if(!errors.containsKey("job")) errors["job"] = mutableListOf()
            errors["job"]?.add("No job was specified")
        }
        "buyer" -> {
            if(!errors.containsKey("job")) errors["job"] = mutableListOf()
            errors["job"]?.add("buyer".italic() + " job is not yet supported")
        }
    }

    if(keysPath == "") {
        if (!errors.containsKey("parameter")) errors["parameter"] = mutableListOf()
        errors["parameter"]?.add("-k, --keys <KEYS_FILE>")
    }

    when(command) {
        "buyer", "seller" -> {
            if(strategyPath == "") {
                if (!errors.containsKey("parameter")) errors["parameter"] = mutableListOf()
                errors["parameter"]?.add("-s, --strategy <FILE>")
            }

            if(priceAdapterPath == "") {
                if(!errors.containsKey("parameter")) errors["parameter"] = mutableListOf()
                errors["parameter"]?.add("-p, --priceAdapter <FILE>")
            }
        }
    }

    if(errors.count() > 0) {
        var errorMessage =
            "ERRORS:".brightRed()

        errors.forEach{(key, value) ->
            if(value.isNotEmpty())
            errorMessage += "\n\t- " +
                when(key) {
                    "job" -> {
                        "Incorrect job configuration:"
                    }
                    "parameter" -> {
                        "Required argument(s) not specified:"
                    }
                    "wrong_file" -> {
                        "Indicated files were not found:"
                    }
                    "wrong_orderbook" -> {
                        "Incorrect orderbook configuration:"
                    }
                    else -> {
                        "Don't know what to do with this:"
                    }
                } + "\n" +
                value.joinToString("\n") { "\t\t$it".red() }

        }

        println(errorMessage + "\n\n" + usage)
        exitProcess(-1)
    }

    println(greeting)
    print("\nWelcome to Huckster!\nInitiating... ")

    val startTime = System.currentTimeMillis()

    //  Create JSON parser
    val gson = GsonBuilder().setLenient().create()

    //  Load Nova configuration
    val configData: ApiConfig = gson.fromJson(
        File(keysPath).bufferedReader(),
        ApiConfig::class.java
    )

    //  Init Nova connection
    NovaApiService.init(configData)

    //  Initialize orders queue
    OrdersQueue.init(NovaApiService)

    when(command) {
        "orderbook" -> {
            try {
                runBlocking { OrdersQueue.drawOrderBook(base, counter, refreshInterval) }
            } catch(e: Exception) {
                logger2(e.message + "\n" +
                    e.stackTrace.joinToString("\n").red())
                exitProcess(-1)
            }
        }

        "buyer", "seller" -> {
            //  Initialize rates adapter
            val priceFeedConfiguration = gson.fromJson(
                File(priceAdapterPath).bufferedReader(),
                JsonObject::class.java
            )

            val priceAdapter = try {
                when(priceFeedConfiguration["adapter"].asString) {
                    "fixed"         -> FixedRate(priceFeedConfiguration)
                    "googleSheet"   ->
                    {
                        if(!File(priceAuthPath).exists()) throw(Exception("Authentication file for Google Sheet price adapter doesn't exist. Terminating now..."))
                        GoogleSheetRates(priceFeedConfiguration, priceAuthPath)
                    }
                    "bitcoin.com"   ->
                    {
                        BitcoinComRates(priceFeedConfiguration)
                    }
                    else            ->
                    {
                        throw(Exception("Unknown price adapter (${priceFeedConfiguration["adapter"]}). Terminating now..."))
                    }
                }
            } catch(e: Exception) {
                logger2(e.stackTrace.joinToString("\n").red())
                exitProcess(-1)
            }

            //  Load strategy configuration
            val strategyConfiguration: StrategyInput = gson.fromJson(
                File(strategyPath).bufferedReader(),
                StrategyInput::class.java
            )

            val strategyAdapter = try {
                when(strategyConfiguration.configuration.adapter) {
                    "basic" -> BasicStrategy(strategyConfiguration, priceAdapter)
                    else -> {
                        throw(Exception("Unknown strategy adapter (${strategyConfiguration.configuration.adapter}). Terminating now..."))
                    }
                }
            } catch(e: Exception) {
                logger2(e.message + "\n" + e.stackTrace.joinToString("\n").red())
                exitProcess(-1)
            }

            val launchTime = Date(System.currentTimeMillis() - startTime)
            println(" launched in " +
                    SimpleDateFormat("HH:mm:ss").apply{
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(launchTime) + "\n"
            )

            runBlocking { strategyAdapter.run() }
        }
    }
}

fun logger2(message: Any?) {
    println("[" + now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)) + "] --> $message")
}