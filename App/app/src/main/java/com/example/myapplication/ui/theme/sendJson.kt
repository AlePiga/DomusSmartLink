package com.example.myapplication.ui.theme

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

fun sendLed(id: Int, status: Int) {

    Thread {

        try {
            val url = URL("http://wireself.duckdns.org:8080/led")
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "POST"
            conn.doOutput = true

            conn.setRequestProperty("Content-Type", "application/json")

            val json = """{"led":$id,"stato":$status}"""

            conn.outputStream.use {
                it.write(json.toByteArray())
            }

            val code = conn.responseCode
            Log.d("DEBUG", "CODE: $code")

            val response = conn.inputStream.bufferedReader().readText()
            Log.d("DEBUG", "RESPONSE: $response")

        } catch (e: Exception) {
            Log.e("DEBUG", "ERRORE", e)
        }
    }.start()
}
fun sendVentola(id: Int, status: Int) {

    Thread {

        try {
            val url = URL("http://wireself.duckdns.org:8080/ventilatore")
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "POST"
            conn.doOutput = true
            var json = "";
            conn.setRequestProperty("Content-Type", "application/json")
            if(status==0){
                json = """{"stato":"stop","velocita":$status}"""
            }else{
                json = """{"stato":"avanti","velocita":$status}"""
            }

            conn.outputStream.use {
                it.write(json.toByteArray())
            }

            val code = conn.responseCode
            Log.d("DEBUG", "CODE: $code")

            val response = conn.inputStream.bufferedReader().readText()
            Log.d("DEBUG", "RESPONSE: $response")

        } catch (e: Exception) {
            Log.e("DEBUG", "ERRORE", e)
        }
    }.start()
}