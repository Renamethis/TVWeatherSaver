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
import kotlinx.coroutines.cancelChildren
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date


private const val startColor = 0x91D8F5;
private const val endColor = 0x377E9B;

operator fun JSONArray.iterator(): Iterator<JSONObject>
        = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

class DreamActivity : DreamService() {
    private lateinit var scope: CoroutineScope;
    private lateinit var fusedLocation: FusedLocation;
    private lateinit var weatherModule: Weather;
    private lateinit var enviroContainer: EnviroContainer;
    private lateinit var weatherRunnable: Runnable;
    private lateinit var enviroRunnable: Runnable;
    private val handler = Handler()

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
        fusedLocation = FusedLocation(applicationContext)
        weatherModule = Weather(scope, findViewById(R.id.cloud_view),
            findViewById(R.id.weather_view), findViewById(R.id.weather_description),
            findViewById(R.id.temperature_view))
        val layout = findViewById<ConstraintLayout>(R.id.dream_layout)
        enviroContainer = EnviroContainer(layout, applicationContext, scope, Color(
            startColor), layout.width, layout.height);
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
        handler.postDelayed(weatherRunnable, 1000)
        handler.postDelayed(timeRunnable,100)
    }
    
    private fun updateWeatherAndClouds(){
        Log.i("DECIK", "DECIWEATHER")
        val location = fusedLocation.getLocationTask();
        location?.addOnSuccessListener {
            if (it != null) {
                weatherModule.update(
                    applicationContext.getString(R.string.weather_backend_url) + "?lat=" +
                            it.latitude + "&lon=" + it.longitude + "&appid=" +
                            applicationContext.getString(R.string.apikey)
                )
            } else {
                Log.i("DECIK", "WTF");
                // TODO: Proceed delay
            }
        }
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