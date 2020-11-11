package com.broxus

import com.broxus.huckster.OrdersQueue
import com.broxus.huckster.models.StrategyInput
import com.broxus.huckster.prices.adapters.FixedRate
import com.broxus.huckster.prices.adapters.GoogleSheetRates
import com.broxus.huckster.strategies.BasicStrategy
import com.broxus.nova.client.NovaApiService
import com.broxus.nova.models.ApiConfig
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.importre.crayon.*
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import java.io.File
import java.time.LocalDateTime.now
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    var command = ""
    var keysPath = ""
    var strategyPath = ""
    var priceAdapterPath = ""
    var priceAuthPath = ""

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
        "USAGE:\n".brightGreen() +
        "\thuckster " + "<JOB> ".green() + "[PARAMS]\n".yellow() +
        "\t\n" +
        "\tJobs:\n".green() +
        "\t\t" + "buyer".italic() + "\t\t\t\t\t\tBuy at the lowest possible price\n" +
        "\t\t" + "seller".italic() + "\t\t\t\t\t\tSell at the highest possible price\n" +
        "\t\n" +
        "\tParameters:\n".yellow() +
        "\t\t-k, --keys <FILE>".italic() +
                "\t\t\tKeys to access Broxus Nova platform\n" +
        "\t\t-s, --strategy <FILE>".italic() +
                "\t\tStrategy configuration\n" +
        "\t\t-pad, --priceAdapter <FILE>".italic() +
                "\tPrice adapter configuration\n" +
        "\t\t-pau, --priceAuth <FILE>".italic() +
                "\tPrice adapter authentication file [optional]\n"

    val errors: MutableMap<String, MutableList<String>> = mutableMapOf()

    //  Parse command line arguments
    var i = 0
    while(i <= args.lastIndex) {
        when(args[i]) {
            //  Subsommand
            "seller", "buyer" -> {
                if(args[i] == "buyer") {
                    logger2(("buyer".italic() + " job is not yet supported. Terminating now...").red())
                    exitProcess(-1)
                }

                command = args[i]
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
            "--priceAdapter", "-pad" -> {
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
            "--priceAuth", "-pau" -> {
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

    if(command == "") {
        if(!errors.containsKey("job")) errors["job"] = mutableListOf()

        errors["job"]?.add("No job was specified")
    }


    if(keysPath == "") {
        if(!errors.containsKey("parameter")) errors["parameter"] = mutableListOf()

        errors["parameter"]?.add("-k, --keys <KEYS_FILE>")
    }

    if(strategyPath == "") {
        if(!errors.containsKey("parameter")) errors["parameter"] = mutableListOf()

        errors["parameter"]?.add("-s, --strategy <FILE>")
    }

    if(priceAdapterPath == "") {
        if(!errors.containsKey("parameter")) errors["parameter"] = mutableListOf()

        errors["parameter"]?.add("-p, --priceAdapter <FILE>")
    }

    if(errors.count() > 0) {
        var errorMessage =
            "ERRORS:".brightRed()

        errors.forEach{(key, value) ->
            errorMessage += "\n\t- " +
                    when(key) {
                        "job" -> {
                            "Job was not specified"
                        }
                        "parameter" -> {
                            "Required argument(s) not specified:\n" +
                                    value.joinToString("\n") { "\t\t$it".red() }
                        }
                        "wrong_file" -> {
                            "Indicated files were not found:\n" +
                                    value.joinToString("\n") { "\t\t$it".red() }
                        }
                        else -> {
                            "Don't know what to do with this:\n" +
                                    value.joinToString("\n") { "\t\t$it".red() }
                        }
                    }
        }

        println(errorMessage + "\n\n" + usage)
        exitProcess(-1)
    }

    println(greeting)
    println("\nWelcome to Huckster!\nInitiating...\n")

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

    //  Initialize rates adapter
    val priceFeedConfiguration = gson.fromJson(
        File(priceAdapterPath).bufferedReader(),
        JsonObject::class.java
    )

    val priceAdapter = try {
            when(priceFeedConfiguration["adapter"].asString) {
                "fixed"         -> FixedRate(priceFeedConfiguration)
                "googleSheet" -> {
                    if(!File(priceAuthPath).exists()) throw(Exception("Authentication file for Google Sheet price adapter doesn't exist. Terminating now..."))
                    GoogleSheetRates(priceFeedConfiguration, priceAuthPath)
                }
                else            -> {
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
        logger2(e.stackTrace.joinToString("\n").red())
        exitProcess(-1)
    }

    //  Launch orders placement
    runBlocking {
        strategyAdapter.run()
    }
}

fun logger2(message: Any?) {
    println("[" + now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)) + "] --> $message")
}