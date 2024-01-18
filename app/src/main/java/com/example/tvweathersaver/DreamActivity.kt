package com.example.tvweathersaver

import android.R.drawable
import android.annotation.SuppressLint
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.service.dreams.DreamService
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.example.library.CloudView
import com.github.matteobattilana.weather.PrecipType
import com.github.matteobattilana.weather.WeatherView
import java.util.Date


private const val startColor = 0x87CEEB;
private const val endColor = 0x377E9B;

operator fun JSONArray.iterator(): Iterator<JSONObject>
        = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

class DreamActivity : DreamService() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationManager: LocationManager? = null
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
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?;
        val layout = findViewById<FrameLayout>(R.id.dream_layout)
        val handler = Handler()
        var enviroRunnable: Runnable? = null
        val timeRunnable = object : Runnable {
            var count = 0;
            override fun run () {
                handler.removeCallbacksAndMessages(null)
                updateTime()
                if(count % 100 == 0) {
                    enviroRunnable?.let { handler.postDelayed(it, 100) }
                    count = 0;
                }
                count++;
                handler.postDelayed(this, 100)
            }
        }
        enviroRunnable = object : Runnable {
            override fun run() {
                handler.removeCallbacksAndMessages(null)
                updateEnviroViews(findViewById<ConstraintLayout>(R.id.dream_layout))
                handler.postDelayed(timeRunnable, 100)
            }
        }
        handler.postDelayed(timeRunnable,100)
        handler.postDelayed(enviroRunnable, 100)
        val layout = findViewById<ConstraintLayout>(R.id.dream_layout)
        layout.setBackgroundDrawable(calculateBackgroundGradient());

//      // layout.addView(testEnviroView)
        // Start playback, etc
    }

    private fun updateEnviroViews(layout: ConstraintLayout) {
        val response = HttpClient.get(
            applicationContext.getString(R.string.enviro_backend_url))
        response ?: return
        var height = layout.measuredHeight - 1100;
        var width = layout.measuredWidth - 1890;
        for(key in response!!.keys()) {
            if(key != "datetime") {
                val obj = response.getJSONObject(key);
                val value = obj.getDouble("value")
                val unit = obj.getString("unit")
                val limits = obj.getJSONArray("limits")
                if(layout.getViewById(key.hashCode()) == null) {
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
                    val existsView = layout.getViewById(key.hashCode()) as? EnviroModuleView
                    existsView?.updateView(value.toFloat())
                }
                layout.invalidate()
            }
            height += 60
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
        val darkFactor = (1 - (24 - Date().hours)/24f)*2;
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