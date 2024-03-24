package com.example.tvweathersaver

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets


private fun readDataFromStream(responseCode: Int, inputStream: InputStream): JSONObject? {
    if (responseCode == HttpURLConnection.HTTP_OK) {
        return try {
            val stream: DataInputStream = DataInputStream(inputStream)
            val reader: BufferedReader = BufferedReader(InputStreamReader(stream))
            JSONObject(reader.readText());
        } catch (exception: Exception) {
            Log.i("TVErrorLog", "Unable to read from HTTP")
            null;
        }
    }
    return null;
}
class HttpClient {
    companion object {
        suspend fun get(stringUrl: String, authentication:String? = null): JSONObject? {
            val url = URL(stringUrl);
            with(url.openConnection() as HttpURLConnection) {
                val policy = ThreadPolicy.Builder().permitAll().build()
                StrictMode.setThreadPolicy(policy)
                requestMethod = "GET"
                connectTimeout = 2000;
                setRequestProperty("charset", "utf-8")
                if(authentication != null)
                    setRequestProperty("Authorization", authentication)
                try {
                    return readDataFromStream(responseCode, inputStream)
                } catch (_: ConnectException) {
                    Log.i("TVErrorLog", "Can't connect to server")
                    return null;
                } catch (_: SocketTimeoutException) {
                    Log.i("TVErrorLog", "Connection time out")
                    return null;
                } catch(_: java.io.IOException) {
                    Log.i("TVErrorLog", "IO Exception")
                    return null;
                }
            }
        }
        fun post(stringUrl: String, body: JSONObject, authentication:String? = null): JSONObject? {
            val url = URL(stringUrl);
            val byteBody: ByteArray = body.toString().toByteArray(StandardCharsets.UTF_8)
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "POST"
                connectTimeout = 200;
                setRequestProperty("charset", "utf-8")
                setRequestProperty("Content-length", byteBody.size.toString())
                setRequestProperty("Content-Type", "application/json")
                if(authentication != null)
                    setRequestProperty("Authorization", authentication)
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