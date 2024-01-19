package com.example.tvweathersaver

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.service.dreams.DreamService
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.library.CloudView
import com.github.matteobattilana.weather.PrecipType
import com.github.matteobattilana.weather.WeatherView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import kotlin.math.cos
import kotlin.math.pow


private const val startColor = 0x91D8F5;
private const val endColor = 0x377E9B;

operator fun JSONArray.iterator(): Iterator<JSONObject>
        = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

class DreamActivity : DreamService() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var cloudView: CloudView;

    @SuppressLint("AppBundleLocaleChanges")
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.i(TAG, "onAttachedToWindow")
        // Setup
        isFullscreen = true
        isInteractive = true
        setContentView(R.layout.weather_view)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        cloudView = findViewById(R.id.cloud_view)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        //findViewById<WeatherView>(R.id.weather_view).setCustomBitmap(
        //    BitmapFactory.decodeResource(applicationContext.getResources(), R.drawable.snowflake))
        cloudView.setDefaults()
        cloudView.stopAnimations()
        val layout = findViewById<FrameLayout>(R.id.dream_layout)
        val handler = Handler()
        lateinit var weatherRunnable: Runnable
        lateinit var enviroRunnable: Runnable
        val timeRunnable = object : Runnable {
            @RequiresApi(Build.VERSION_CODES.Q)
            override fun run () {
                handler.removeCallbacksAndMessages(this)
                updateTime()
                if(!handler.hasCallbacks(enviroRunnable))
                    handler.postDelayed(enviroRunnable, 5000)
                if(!handler.hasCallbacks(weatherRunnable))
                    handler.postDelayed(weatherRunnable, 1000)
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
                updateEnviroViews(findViewById<ConstraintLayout>(R.id.dream_layout))
            }
        }
        handler.postDelayed(timeRunnable,100)
        layout.setBackgroundDrawable(calculateBackgroundGradient());
//      // layout.addView(testEnviroView)
        // Start playback, etc
    }

    private fun updateWeatherView() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                Log.i("DECIK", "DECIMETER");
                val response = HttpClient.get(
                    applicationContext.getString(R.string.weather_backend_url) + "?lat=" +
                            location?.latitude + "&lon=" + location?.longitude + "&appid=" +
                            applicationContext.getString(R.string.apikey))
                val currentWeather = response?.getJSONObject("current")
                val clouds = currentWeather?.getInt("clouds")
                val wind = currentWeather?.getDouble("wind_speed")
                val weather = currentWeather?.getJSONArray("weather")
                if (clouds != null) {
                    if(clouds > 20) {
                        cloudView.setPassTimeVariance(
                            (20000.0 * (1.0 - (wind?.div(20.0) ?: 1.0))).toInt()
                        )
                        cloudView.setBasePassTime(
                            (10000.0 * (1.0 - (wind?.div(20.0) ?: 1.0))).toInt()
                        )
                        cloudView.setCloudCount((10.0 * ((clouds?.div(100.0) ?: 0.0))).toInt())
                        if(!cloudView.isAnimationRequested)
                            cloudView.startAnimation()
                    }
                }
                weather?.let {
                    for (it in weather) {
                        val weatherMain = it.getString("main")
                        val weatherDescription = it.getString("description")
                        if(weatherMain == "Snow")
                            findViewById<WeatherView>(R.id.weather_view).setWeatherData(PrecipType.SNOW)
                        else if(weatherMain == "Rain")
                            findViewById<WeatherView>(R.id.weather_view).setWeatherData(PrecipType.RAIN)
//                            findViewById<WeatherView>(R.id.weather_view).emissionRate = 150.0f
                    }
                }
            }
    }
    private fun updateEnviroViews(layout: FrameLayout) {
        val response = HttpClient.get(
            applicationContext.getString(R.string.enviro_backend_url))
        response ?: return
        var height = layout.measuredHeight - 1110;
        var width = layout.measuredWidth - 1890;
        for(key in response!!.keys()) {
            if(key != "datetime") {
                val obj = response.getJSONObject(key);
                val value = obj.getDouble("value")
                val unit = obj.getString("unit")
                val limits = obj.getJSONArray("limits")
                if(layout.findViewById<EnviroModuleView>(key.hashCode()) == null) {
                    val drawableId = applicationContext.resources.getIdentifier(key, "drawable", "com.example.tvweathersaver");
                    val enviroView = EnviroModuleView(applicationContext,
                        drawableId, key,
                        Pair<Int, Int>(limits.getInt(0), limits.getInt(1)), value.toFloat(), unit, Point(width, height), Color(darkenColor(startColor, 0.2f))
                    )
                    enviroView.id = key.hashCode()
                    enviroView.layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT)
                    layout.addView(enviroView);
                } else {
                    val existsView = layout.findViewById(key.hashCode()) as? EnviroModuleView
                    existsView?.updateView(value.toFloat())
                }
                layout.invalidate()
            }
            height += 65
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
    private fun calculateBackgroundGradient(): GradientDrawable {
        val hours = Date().hours
        val darkFactor = 1 -  0.55f * cos(Math.PI * hours.toDouble() / 24).pow(2.0).toFloat()
        val gd = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(
                darkenColor(startColor, darkFactor),
                darkenColor(startColor, darkFactor))
        )
        gd.cornerRadius = 0f
        return gd
    }
    private fun darkenColor(color: Int, factor: Float): Int {
        val a: Int = color.alpha
        val r = Math.round(color.red * factor).toInt()
        val g = Math.round(color.green * factor).toInt()
        val b = Math.round(color.blue * factor).toInt()
        return Color(a, r, g, b).hashCode()
    }
    private fun formatTime(value: Int) : String {
        if(value < 10)
            return "0$value"
        return value.toString();
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        Log.i(TAG, "onDreamingStopped")
        // Stop playback, animations, etc
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.i(TAG, "onDetachedFromWindow")
        // Remove resources
    }

    companion object {
        private const val TAG = "DreamActivity"
    }

}