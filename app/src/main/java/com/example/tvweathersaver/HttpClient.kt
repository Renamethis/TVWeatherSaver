package com.example.tvweathersaver

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors


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
        @RequiresApi(Build.VERSION_CODES.N)
        suspend fun post(stringUrl: String, body: JSONObject, authentication:String? = null): JSONObject? {
            val url = URL(stringUrl);
            Log.i("TVErrorLog", url.toString())
//            val byteBody: ByteArray = body.toString().toByteArray(StandardCharsets.UTF_8)
//            with(url.openConnection() as HttpURLConnection) {
//                requestMethod = "POST"
//                connectTimeout = 10000;
//                readTimeout = 10000;
//                setRequestProperty("charset", "utf-8")
//                setRequestProperty("User-Agent", "Java/1.6.0_30")
//                setRequestProperty("Accept", "application/json")
//                setRequestProperty("Host", "192.168.88.254:5000")
//                setRequestProperty("Connection", "keep-alive")
//                doOutput = true;
//                Log.i("TVError", body.toString())
//                OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
//                    writer.write(
//                        body.toString().replace(" ", "").replace("\n", "")
//                    )
//                }
//                if(authentication != null)
//                    setRequestProperty("Authorization", authentication)
//                try {
////                    BufferedReader(
////                        InputStreamReader(inputStream, StandardCharsets.UTF_8)
////                    ).use { reader ->
////                        return JSONObject(reader.lines()
////                                .collect(Collectors.joining(System.lineSeparator())))
////                    }
//                    return readDataFromStream(responseCode, inputStream)
//                } catch (_: ConnectException) {
//                    Log.i("TVErrorLog", "Can't connect to server")
//                    return null;
//                } catch (_: SocketTimeoutException) {
//                    Log.i("TVErrorLog", "Connection time out")
//                    return null;
//                } catch(e: java.io.IOException) {
//                    Log.i("TVErrorLog", "IO Exception" + e.message)
//                    return null;
//                }
            val (request, response, result) = Fuel.post(stringUrl).jsonBody(body.toString()).response()
            return JSONObject(response.data.decodeToString())
        }
    }
}