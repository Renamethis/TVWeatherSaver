package com.example.tvweathersaver

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.service.dreams.DreamService
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.github.matteobattilana.weather.PrecipType
import com.github.matteobattilana.weather.WeatherView
import java.util.Date


class DreamActivity : DreamService() {
    @SuppressLint("AppBundleLocaleChanges")
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.i(TAG, "onAttachedToWindow")
        // Setup
        isFullscreen = true
        isInteractive = true
        setContentView(R.layout.weather_view)
//        val mSunriseSunsetView = findViewById<View>(R.id.ssv) as SunriseSunsetView
//        mSunriseSunsetView.setSunriseTime(Time(9, 30))
//        mSunriseSunsetView.setSunsetTime(Time(18, 40))
//        mSunriseSunsetView.startAnimate()
        //findViewById<WeatherView>(R.id.weather_view).speed = 255
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        //findViewById<WeatherView>(R.id.weather_view).setCustomBitmap(
        //    BitmapFactory.decodeResource(applicationContext.getResources(), R.drawable.snowflake))
        findViewById<WeatherView>(R.id.weather_view).setWeatherData(PrecipType.RAIN)
        findViewById<WeatherView>(R.id.weather_view).emissionRate = 150.0f
        val handler = Handler()
        val runnable = object : Runnable {
            override fun run () {
                handler.removeCallbacksAndMessages(null)
                val txtCurrentTime = findViewById<View>(R.id.time) as TextView
                val dt = Date()
                val hours = formatTime(dt.hours)
                val minutes = formatTime(dt.minutes)
                val seconds = formatTime(dt.seconds)
                val curTime = "$hours:$minutes:$seconds"
                txtCurrentTime.text = curTime
                handler.postDelayed(this, 100)
            }
        }
        val layout = findViewById<ConstraintLayout>(R.id.dream_layout)
        handler.postDelayed(runnable,100)
        val gd = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x000000, 0xFFFFFF)
        )
        gd.cornerRadius = 0f
        layout.setBackgroundDrawable(gd);
        // Start playback, etc
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