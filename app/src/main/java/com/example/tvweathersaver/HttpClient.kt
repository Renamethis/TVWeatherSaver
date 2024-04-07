package com.example.tvweathersaver

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import org.json.JSONObject
class HttpClient {
    companion object {
        suspend fun get(stringUrl: String, authentication:String? = null): JSONObject? {
            val (_, response, _) = Fuel.get(stringUrl)
                .authentication().bearer(authentication.toString())
                .response()
            return if(response.statusCode == 200)
                JSONObject(response.data.decodeToString())
            else
                null
        }
        @RequiresApi(Build.VERSION_CODES.N)
        suspend fun post(stringUrl: String, body: JSONObject, authentication:String? = null): JSONObject? {
            val (_, response, _) = Fuel.post(stringUrl)
                .jsonBody(body.toString())
                .authentication().bearer(authentication.toString())
                .response()
            return if(response.statusCode == 200)
                JSONObject(response.data.decodeToString())
            else
                null
        }
    }
}