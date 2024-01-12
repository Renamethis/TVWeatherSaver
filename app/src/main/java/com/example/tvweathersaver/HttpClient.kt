package com.example.tvweathersaver

import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class HttpClient {
    companion object {
        @JvmStatic
        fun get(stringUrl: String, query: Array<Pair<String, String>>): String? {
            return null;
        }
        @JvmStatic
        fun post(stringUrl: String, body: JSONObject): JSONObject? {
            val url = URL(stringUrl);
            val byteBody: ByteArray = body.toString().toByteArray(StandardCharsets.UTF_8)
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "POST"
                setRequestProperty("charset", "utf-8")
                setRequestProperty("Content-length", byteBody.size.toString())
                setRequestProperty("Content-Type", "application/json")
                try {
                    val outputStream: DataOutputStream = DataOutputStream(outputStream)
                    outputStream.write(byteBody)
                    outputStream.flush()
                } catch (exception: Exception) {
                    return null;
                }
                if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_CREATED) {
                    try {
                        val inputStream: DataInputStream = DataInputStream(inputStream)
                        val reader: BufferedReader = BufferedReader(InputStreamReader(inputStream))
                        return JSONObject(reader.readText());

                    } catch (exception: Exception) {
                        return null;
                    }
                }
            }
            return null;
        }
    }
}