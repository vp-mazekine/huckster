package com.broxus.huckster.prices.adapters

import com.broxus.huckster.interfaces.PriceFeed
import com.broxus.huckster.prices.models.GoogleSheetInput
import com.broxus.huckster.prices.models.Rate
import com.broxus.logger2
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.importre.crayon.red
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.jvm.Throws

class GoogleSheetRates(feedConfiguration: JsonObject, authDataPath: String): PriceFeed {
    private val HTTP_TRANSPORT by lazy { GoogleNetHttpTransport.newTrustedTransport() }
    private val JSON_FACTORY by lazy { JacksonFactory.getDefaultInstance() }
    private val TOKENS_DIRECTORY_PATH = "tokens"
    private val AUTH_DATA_PATH = authDataPath
    private val FEED_CONFIGURATION: GoogleSheetInput
    private val SCOPES = listOf(SheetsScopes.SPREADSHEETS_READONLY)

    private var rates: MutableList<Rate> = mutableListOf()
    private var updateAt: Long?

    init {
        try {
            FEED_CONFIGURATION = Gson().fromJson(feedConfiguration, GoogleSheetInput::class.java)
        } catch (e: IOException) {
            throw(IOException("Incorrect price feed configuration!\n${e.stackTrace}".red(), e))
        }

        updateAt = when(FEED_CONFIGURATION.configuration.refreshRate) {
            null -> null
            else -> System.currentTimeMillis() + FEED_CONFIGURATION.configuration.refreshRate.toLong() * 1000
        }

        fetchData()
    }

    override fun getPrice(fromCurrency: String, toCurrency: String, volume: Float?): Float? {
        updateAt?.let {
            if(System.currentTimeMillis() >= it) {
                fetchData()
                updateAt = when(FEED_CONFIGURATION.configuration.refreshRate) {
                    null -> null
                    else -> System.currentTimeMillis() + FEED_CONFIGURATION.configuration.refreshRate.toLong() * 1000
                }
            }
        }

        rates.forEach {
            if(it.fromCurrency == fromCurrency && it.toCurrency == toCurrency) return it.rate
        }

        return null
    }

    /**
     * Extracts the data from a Google Sheet to the local storage
     */
    private fun fetchData() {
        val gService = Sheets.Builder(
            HTTP_TRANSPORT,
            JSON_FACTORY,
            getCredentials()
        ).setApplicationName("Huckster")
         .build()

        val response = gService.spreadsheets().values()
            .get(
                FEED_CONFIGURATION.configuration.sheetId,
                FEED_CONFIGURATION.configuration.sourceDataSheet + "!" +
                        FEED_CONFIGURATION.configuration.sourceDataRange
            ).execute()

        val result = response.getValues()
        var fromCurrency: String
        var toCurrency: String
        var rate: Float?
        var r: Rate
        var i: Int?

        if(result.isNullOrEmpty()) {
            logger2("Empty price feed from the Google Sheet...".red())
        } else {
            result.forEachIndexed { index, row ->
                //  Suppose the source data contain at least three columns
                if(row.count() < 3) {
                    logger2("Invalid price feed line $index: ".red() +
                            row.joinToString(
                                "] [",
                                "[",
                                "]"
                            )
                    )
                } else {
                    rate = row[2].toString().toFloatOrNull()
                    if(rate != null) {
                        fromCurrency = row[0].toString()
                        toCurrency = row[1].toString()

                        i = rates.findIndex(
                            fromCurrency, toCurrency
                        )

                        r = Rate(fromCurrency, toCurrency, rate!!)

                        when(i) {
                            null -> rates.add(r)
                            else -> rates[i!!] = r
                        }
                    }
                }
            }
        }
    }

    private fun MutableList<Rate>.findIndex(fromCurrency: String, toCurrency: String): Int? {
        this.forEachIndexed { index, rate ->
            if(rate.fromCurrency == fromCurrency && rate.toCurrency == toCurrency) return index
        }

        return null
    }

    /**
     * As per Google manual https://developers.google.com/sheets/api/quickstart/java
     *
     * @return
     */
    @Throws(IOException::class)
    private fun getCredentials(): Credential {
        if(!File(AUTH_DATA_PATH).exists()) {
            throw FileNotFoundException("Authentication data file not found: $AUTH_DATA_PATH");
        }

        val clientSecrets = GoogleClientSecrets.load(
            JSON_FACTORY,
            File(AUTH_DATA_PATH).inputStream().reader()
        )

        //clientSecrets.web = clientSecrets.details

        val flow = GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT,
            JSON_FACTORY,
            clientSecrets,
            SCOPES
        ).setDataStoreFactory(
            FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH))
        ).setAccessType("offline")
         .setApprovalPrompt("auto")
         .build()

        val receiver = LocalServerReceiver.Builder().setPort(3107).build()

        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }
}