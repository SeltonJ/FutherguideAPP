package com.example.futherguideapp

import android.net.Uri
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

private val eBirdBASE_URL ="https://api.ebird.org/v2/data/obs/region/recent?r=ZA"
private val PARAM_METRIC = "metric"
private val METRIC_VALUE = "true"
private val PARAM_API_KEY = "key"
private val LOGGING_TAG = "URLWECREATED"

fun buildURLForBirds(): URL? {
    val buildUri: Uri = Uri.parse(eBirdBASE_URL).buildUpon()
        .appendQueryParameter(
            PARAM_API_KEY,
            BuildConfig.eBird_API_KEY
        ) // passing in api key
        .appendQueryParameter(
            PARAM_METRIC,
            METRIC_VALUE
        ) // passing in metric as measurement unit
        .build()
    var url: URL? = null
    try {
        url = URL(buildUri.toString())
    } catch (e: MalformedURLException) {
        e.printStackTrace()
    }
    Log.i(LOGGING_TAG, "buildURLForEBirds: $url")
    return url
}

fun getPlaceNameFromCoordinates(lat: Double, lng: Double): String {
    val apiUrl = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$lat&lon=$lng"
    val url = URL(apiUrl)

    val connection = url.openConnection() as HttpURLConnection

    return try {
        connection.connect()
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val inputStream = connection.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val response = reader.readText()
            val jsonResponse = JSONObject(response)

            if (jsonResponse.has("display_name")) {
                val displayName = jsonResponse.getString("display_name")
                displayName // Return the place name
            } else {
                "Place name not found"
            }
        } else {
            "Error response: $responseCode"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        "Error occurred while fetching place name"
    } finally {
        connection.disconnect()
    }
}


