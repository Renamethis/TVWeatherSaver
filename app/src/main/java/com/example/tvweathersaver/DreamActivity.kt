package com.example.tvweathersaver

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.service.dreams.DreamService
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.library.CloudView
import com.github.matteobattilana.weather.PrecipType
import com.github.matteobattilana.weather.WeatherView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
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
    private lateinit var cloudView: CloudView;
    private lateinit var enviroContainer: EnviroContainer;
    private lateinit var weatherRunnable: Runnable
    private lateinit var enviroRunnable: Runnable
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
        cloudView = findViewById(R.id.cloud_view)
        enviroContainer = EnviroContainer(findViewById<ConstraintLayout>(R.id.dream_layout), applicationContext, scope, Color(
            startColor));
    }
    override fun onDreamingStarted() {
        super.onDreamingStarted()
        //findViewById<WeatherView>(R.id.weather_view).setCustomBitmap(
        //    BitmapFactory.decodeResource(applicationContext.getResources(), R.drawable.snowflake))
        cloudView.setDefaults()
        cloudView.setMinSize(300)
        cloudView.stopAnimations()
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
                getWeatherByLocation()
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

    private val mutex = Mutex()
    private fun updateWeather(url: String) {
        if(!mutex.tryLock())
            return
        scope.launch {
            val response = withContext(Dispatchers.IO) { HttpClient.get(url) }
            val currentWeather = response?.getJSONObject("current")
            val clouds = currentWeather?.getInt("clouds")
            val wind = currentWeather?.getDouble("wind_speed")
            val weather = currentWeather?.getJSONArray("weather")
            if (clouds != null) {
                if (clouds > 20 && !cloudView.isAnimating) { // TODO: Check if difference is enough to update animation
                    cloudView.setPassTimeVariance(
                        (20000.0 * (1.0 - (wind?.div(20.0) ?: 1.0))).toInt()
                    );
                    cloudView.setBasePassTime(
                        (10000.0 * (1.0 - (wind?.div(20.0) ?: 1.0))).toInt()
                    );
                    cloudView.setCloudCount((10.0 * ((clouds.div(100.0) ?: 0.0))).toInt())
                    if (!cloudView.isAnimating)
                        cloudView.startAnimation();
                } else if(clouds <= 20 && cloudView.isAnimating) {
                    cloudView.stopAnimations();
                }
            }
            weather?.let {
                for (it in weather!!) {
                    val weatherMain = it.getString("main")
                    val weatherDescription = it.getString("description")
                    if (weatherMain == "Snow")
                        findViewById<WeatherView>(R.id.weather_view).setWeatherData(PrecipType.SNOW)
                    else if (weatherMain == "Rain")
                        findViewById<WeatherView>(R.id.weather_view).setWeatherData(PrecipType.RAIN)
//                            findViewById<WeatherView>(R.id.weather_view).emissionRate = 150.0f
                }
            }
            mutex.unlock();
        }
    }
    @SuppressLint("MissingPermission")
    private fun getWeatherByLocation(){
        val location = fusedLocation.getLocation();
        if(location != null)
            updateWeather(
                applicationContext.getString(R.string.weather_backend_url) + "?lat=" +
                        location.latitude + "&lon=" + location.longitude + "&appid=" +
                        applicationContext.getString(R.string.apikey)
            )
        else
            getWeatherByLocation()
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