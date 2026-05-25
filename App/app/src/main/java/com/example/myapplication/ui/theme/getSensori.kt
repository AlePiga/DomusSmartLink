package com.example.myapplication.ui.theme

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

fun getSensori(onResult: (String) -> Unit) {

    Thread {
        try {
        val url = URL("http://wireself.duckdns.org:8080/sensori")
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "GET"

            val response = conn.inputStream.bufferedReader().readText()

            Log.d("API", response)

            onResult(response)

        } catch (e: Exception) {
            Log.e("API", "Errore", e)
        }
    }.start()
}