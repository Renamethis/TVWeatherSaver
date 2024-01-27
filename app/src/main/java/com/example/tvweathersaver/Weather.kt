package com.example.tvweathersaver

import android.widget.TextView
import com.example.library.CloudView
import com.github.matteobattilana.weather.PrecipType
import com.github.matteobattilana.weather.WeatherView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Suppress("NAME_SHADOWING")
class Weather(
    private val scope: CoroutineScope,
    private val cloudView: CloudView,
    private val weatherView: WeatherView,
    private val weatherDescription: TextView
) {
    private val mutex = Mutex()
    init {
        cloudView.setDefaults()
        cloudView.setMinSize(300)
        cloudView.stopAnimations()
    }
    private fun updateClouds(currentWeather: JSONObject?) {
        val clouds = currentWeather?.getInt("clouds")
        val wind = currentWeather?.getDouble("wind_speed")
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
    }
    private fun updateWeather(currentWeather: JSONObject?) {
        val weather = currentWeather?.getJSONArray("weather")
        weather?.let {
            for (it in weather) {
                val weatherMain = it.getString("main")
                weatherDescription.text = it.getString("description")
                if (weatherMain == "Snow") {
                    weatherView.setWeatherData(PrecipType.SNOW) // TDOO: Check is it correct
                    val percipation = currentWeather.getJSONObject("snow").getDouble("1h")
                    weatherView.emissionRate = (percipation*150.0).toFloat()
                } else if (weatherMain == "Rain") {
                    weatherView.setWeatherData(PrecipType.RAIN)
                    val rain = currentWeather.getJSONObject("rain").getDouble("1h")
                    weatherView.emissionRate = (rain*150.0).toFloat()
                } else
                    weatherView.resetWeather()
            }
        }
    }
    fun update(url: String) {
        if(!mutex.tryLock())
            return
        scope.launch {
            val response = withContext(Dispatchers.IO) { HttpClient.get(url) }
            val currentWeather = response?.getJSONObject("current")
            updateClouds(currentWeather);
            updateWeather(currentWeather)
            mutex.unlock();
        }
    }

}