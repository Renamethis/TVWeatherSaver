package com.example.tvweathersaver

import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private fun readDataFromStream(responseCode: Int, inputStream: InputStream): JSONObject? {
    if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_CREATED) {
        return try {
            val stream: DataInputStream = DataInputStream(inputStream)
            val reader: BufferedReader = BufferedReader(InputStreamReader(stream))
            JSONObject(reader.readText());
        } catch (exception: Exception) {
            null;
        }
    }
    return null;
}

class HttpClient {
    companion object {
        @JvmStatic
        fun get(stringUrl: String, query: Array<Pair<String, String>>?): JSONObject? {
            val url = URL(stringUrl);
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"
                setRequestProperty("charset", "utf-8")
                return readDataFromStream(responseCode, inputStream);
            }
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
                return readDataFromStream(responseCode, inputStream)
            }
        }
    }
}