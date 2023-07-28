package com.example.dronemr

/**
 * Created by Rohan Jahagirdar on 07-02-2018.
 */


import android.widget.Toast
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.create
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import org.json.JSONObject


class OkHttpRequest(client: OkHttpClient, mainActivity: MainActivity) {
    private var client = client
    var lastMessage : String = ""
    private var mainActivity = mainActivity

    fun sendMessage(jsonMessage: String, url : String) {

        // Lancement de la coroutine principale


        runBlocking {
            launch(IO) {
                post(jsonMessage, url)
            }
        }
    }

    fun getData(url: String) {
        runBlocking {
            launch(IO) {
                println("try")
                get(url)
                //get("catfact.ninja/fact")
            }
        }

    }

    private suspend fun get(url: String) {

        val request = Request.Builder()
            .url(url)
            .build()


        withContext(IO) {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                println("JSON envoyé avec succès au serveur.")

                mainActivity.runOnUiThread {
                    lastMessage = response.body!!.string()
                    //Toast.makeText(mainActivity, lastMessage, Toast.LENGTH_SHORT).show()
                }

            } else {
                println("Erreur .+lors de l'envoi du JSON au serveur: ${response.code}")
            }

        }

        /**
        val request = Request.Builder()
            .url(url)
            .post("test".toRequestBody(MEDIA_TYPE_MARKDOWN))
            .get()



        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                //lastMessage = e.toString()
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    //lastMessage = response.toString()
                    /**
                    for ((name, value) in response.headers) {

                        println("$name: $value")
                    }

                    println(response.body!!.string())
                    */
                }
            }

        })
        */

    }

    private suspend fun post(jsonMessage: String, url : String){

        val request = Request.Builder()
            .url(url)
            .post(jsonMessage.toRequestBody(MEDIA_TYPE_MARKDOWN))
            .build()

        withContext(IO) {

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                println("JSON envoyé avec succès au serveur.")

                mainActivity.runOnUiThread {
                    lastMessage = response.body!!.string()
                }


            } else {
                println("Erreur lors de l'envoi du JSON au serveur: ${response.code}")
            }


        }



        /**
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            //println(response.body!!.string())
        }
        */
    }

    companion object {
        val MEDIA_TYPE_MARKDOWN = "application/json; charset=utf-8".toMediaType()
    }


}