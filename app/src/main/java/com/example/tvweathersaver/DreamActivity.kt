package com.example.tvweathersaver

import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import android.service.dreams.DreamService
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.tv.material3.ExperimentalTvMaterial3Api
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date


operator fun JSONArray.iterator(): Iterator<JSONObject>
        = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

class DreamActivity : DreamService() {
    private lateinit var scope: CoroutineScope;
    private lateinit var fusedLocation: FusedLocation;
    private lateinit var weatherModule: Weather;
    private lateinit var enviroContainer: EnviroContainer;
    private lateinit var weatherRunnable: Runnable;
    private lateinit var enviroRunnable: Runnable;
    private var weatherToken: String? = null
    private val handler = Handler()
    private val environment = dotenv {
        directory = "/assets"
        filename = "env"
    }
    init {
    }

    override fun onCreate() {
        super.onCreate()
    }
    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("AppBundleLocaleChanges")
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.i(TAG, "onAttachedToWindow")
        if(!this::scope.isInitialized)
            scope = CoroutineScope(Job() + Dispatchers.Main);
        // Setup
        isFullscreen = true
        isInteractive = true
        setContentView(R.layout.weather_view)
        val job = scope.launch {
            val response = withContext(Dispatchers.IO) {
                HttpClient.post(
                    applicationContext.getString(R.string.enviro_backend_url) + "/auth/login",
                    JSONObject(
                        mapOf(
                            "nickname" to "WeatherAdmin",
                            "password" to environment["API_PASSWORD"]
                        )
                    )
                )
            }
            weatherToken = response?.getString("auth_token")
            if(weatherToken == null)
                weatherToken = "null"
        }
        job.invokeOnCompletion {
            fusedLocation = FusedLocation(applicationContext)
            weatherModule = Weather(
                applicationContext, scope, findViewById(R.id.cloud_view),
                findViewById(R.id.weather_view), findViewById(R.id.weather_description),
                findViewById(R.id.temperature_view), environment["API_KEY"], weatherToken
            )
            val layout = findViewById<ConstraintLayout>(R.id.dream_layout)
            enviroContainer = EnviroContainer(
                layout, applicationContext, scope, Color(
                    resources.getColor(com.example.library.R.color.startColor)
                ), weatherToken
            )
        }
    }
    override fun onDreamingStarted() {
        super.onDreamingStarted()
        val timeRunnable = object : Runnable {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun run () {
                handler.removeCallbacksAndMessages(this)
                updateTime()
                if(!handler.hasCallbacks(enviroRunnable))
                    handler.postDelayed(enviroRunnable, 100000)
                if(!handler.hasCallbacks(weatherRunnable))
                    handler.postDelayed(weatherRunnable, 300000)
                handler.postDelayed(this, 100)
            }
        }
        weatherRunnable = object: Runnable {
            override fun run() {
                handler.removeCallbacksAndMessages(this)
                if(weatherToken != null)
                    updateWeatherAndClouds()
            }
        }
        enviroRunnable = object : Runnable {
            override fun run() {
                handler.removeCallbacksAndMessages(this)
                if(weatherToken != null)
                    enviroContainer.update()
            }
        }
        handler.postDelayed(weatherRunnable, 1000)
        handler.postDelayed(timeRunnable,1000)
        handler.postDelayed(enviroRunnable, 2000)
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateWeatherAndClouds(){
        val location = fusedLocation.getLocationTask();
        location?.addOnSuccessListener {
            if (it != null) {
                weatherModule.update(it)
            } else {
                // TODO: Proceed delay
            }
        }
    }
    private fun updateTime() {
        val txtCurrentTime = findViewById<View>(R.id.time) as TextView
        val dt = Date()
        val hours = formatTime(dt.hours)
        val minutes = formatTime(dt.minutes)
        val curTime = "$hours:$minutes"
        txtCurrentTime.text = curTime
    }
    private fun formatTime(value: Int) : String {
        if(value < 10)
            return "0$value"
        return value.toString();
    }

    // TODO: Realize when it's better to dispose
    override fun onDreamingStopped() {
        scope.coroutineContext.cancelChildren()
        super.onDreamingStopped()
        Log.i(TAG, "onDreamingStopped")
        // Stop playback, animations, etc
    }

    override fun onDetachedFromWindow() {
        scope.coroutineContext.cancelChildren()
        super.onDetachedFromWindow()
        Log.i(TAG, "onDetachedFromWindow")
        // Remove resources
    }

    companion object {
        private const val TAG = "DreamActivity"
    }

}