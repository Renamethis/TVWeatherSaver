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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date


private const val startColor = 0x91D8F5;
private const val endColor = 0x377E9B;

operator fun JSONArray.iterator(): Iterator<JSONObject>
        = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

val scope = CoroutineScope(Job() + Dispatchers.Main);

class DreamActivity : DreamService() {
    private lateinit var fusedLocation: FusedLocation;
    private lateinit var weatherModule: WeatherModule;
    private lateinit var enviroContainer: EnviroContainer;
    private lateinit var weatherRunnable: Runnable;
    private lateinit var enviroRunnable: Runnable;
    private val handler = Handler()

    @SuppressLint("AppBundleLocaleChanges")
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.i(TAG, "onAttachedToWindow")
        // Setup
        isFullscreen = true
        isInteractive = true
        setContentView(R.layout.weather_view)
        fusedLocation = FusedLocation(applicationContext)
        val layout = findViewById<ConstraintLayout>(R.id.dream_layout)
        weatherModule = WeatherModule(scope, findViewById(R.id.cloud_view),
            findViewById(R.id.weather_view))
        enviroContainer = EnviroContainer(layout, applicationContext, scope, Color(
            startColor));
    }
    override fun onDreamingStarted() {
        super.onDreamingStarted()
        val timeRunnable = object : Runnable {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun run () {
                handler.removeCallbacksAndMessages(this)
                updateTime()
                if(!handler.hasCallbacks(enviroRunnable))
                    handler.postDelayed(enviroRunnable, 50000)
                if(!handler.hasCallbacks(weatherRunnable))
                    handler.postDelayed(weatherRunnable, 100000)
                handler.postDelayed(this, 100)
            }
        }
        weatherRunnable = object: Runnable {
            override fun run() {
                handler.removeCallbacksAndMessages(this)
                updateWeatherAndClouds()
            }
        }
        enviroRunnable = object : Runnable {
            override fun run() {
                handler.removeCallbacksAndMessages(this)
                enviroContainer.update()
            }
        }
        handler.postDelayed(timeRunnable,100)
        handler.postDelayed(weatherRunnable, 1000)
    }
    
    private fun updateWeatherAndClouds(){
        val location = fusedLocation.getLocation();
        if(location != null)
            weatherModule.update(
                applicationContext.getString(R.string.weather_backend_url) + "?lat=" +
                        location.latitude + "&lon=" + location.longitude + "&appid=" +
                        applicationContext.getString(R.string.apikey)
            )
        else
            updateWeatherAndClouds()
    }
    private fun updateTime() {
        val txtCurrentTime = findViewById<View>(R.id.time) as TextView
        val dt = Date()
        val hours = formatTime(dt.hours)
        val minutes = formatTime(dt.minutes)
        val seconds = formatTime(dt.seconds)
        val curTime = "$hours:$minutes:$seconds"
        txtCurrentTime.text = curTime
    }
    private fun formatTime(value: Int) : String {
        if(value < 10)
            return "0$value"
        return value.toString();
    }

    // TODO: Realize when it's better to dispose
    override fun onDreamingStopped() {
        scope.cancel()
        super.onDreamingStopped()
        Log.i(TAG, "onDreamingStopped")
        // Stop playback, animations, etc
    }

    override fun onDetachedFromWindow() {
        scope.cancel()
        super.onDetachedFromWindow()
        Log.i(TAG, "onDetachedFromWindow")
        // Remove resources
    }

    companion object {
        private const val TAG = "DreamActivity"
    }

}