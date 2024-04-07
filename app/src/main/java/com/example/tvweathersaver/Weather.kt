package com.example.tvweathersaver

import android.content.Context
import android.location.Location
import android.os.Build
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.example.library.CloudView
import com.github.matteobattilana.weather.PrecipType
import com.github.matteobattilana.weather.WeatherView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.roundToInt

@Suppress("NAME_SHADOWING")
class Weather(
    private val context: Context,
    private val scope: CoroutineScope,
    private val cloudView: CloudView,
    private val weatherView: WeatherView,
    private val weatherDescriptionView: TextView,
    private val regionTemperatureView: TextView,
    private val apiKey: String,
    private val weatherToken: String?,
) {
    private var defaultUrl = context.getString(R.string.enviro_backend_url)
    private var directUrl = context.getString(R.string.openweathermap_url) + "?appid=" + apiKey
    private val mutex = Mutex()
    private var isWeatherAnimating = false;
    private lateinit var weatherDescriptionString: String;
    init {
        cloudView.setDefaults()
        cloudView.setMinSize(300)
        cloudView.stopAnimations()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateClouds(currentWeather: JSONObject?) {
        val clouds = currentWeather?.getInt("clouds")
        val wind = currentWeather?.getDouble("wind_speed")
        if (clouds != null) {
            if (clouds > 20 && !cloudView.isAnimating) { // TODO: Check if difference is enough to update animation
                val loadedColor: Int;
                if(weatherDescriptionString.contains("overcast"))
                    loadedColor = context.resources.getColor(R.color.cloudGray)
                else
                    loadedColor = context.resources.getColor(R.color.cloudBasic)
                currentWeather?.getDouble("uvi")?.let {
                    cloudView.setColor(loadedColor, it);
                }
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
    private fun updateWeather(currentWeather: JSONObject?, region: String?) {
        val weather = currentWeather?.getJSONArray("weather")
        val temperature = region + " " + (currentWeather?.getDouble("temp")?.minus(273.0))?.roundToInt().toString() + "°"
        regionTemperatureView.text = temperature
        weather?.let {
            for (it in weather) {
                val weatherMain = it.getString("main")
                weatherDescriptionString = it.getString("description")
                weatherDescriptionView.text = weatherDescriptionString
                when(weatherMain) {
                    "Snow" ->
                        if (!isWeatherAnimating || weatherView.precipType != PrecipType.SNOW) {
                            weatherView.setWeatherData(PrecipType.SNOW) // TDOO: Check is it correct
                            val percipation = currentWeather.getJSONObject("snow").getDouble("1h")
                            weatherView.emissionRate = (percipation * 150.0).toFloat()
                            isWeatherAnimating = true
                        }
                    "Rain" ->
                        if (!isWeatherAnimating || weatherView.precipType != PrecipType.SNOW) {
                            weatherView.setWeatherData(PrecipType.RAIN)
                            val rain = currentWeather.getJSONObject("rain").getDouble("1h")
                            weatherView.emissionRate = (rain * 150.0).toFloat()
                            isWeatherAnimating = true
                        }
                    else
                        weatherView.resetWeather()
                }
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun update(loc: Location) {
        if(!mutex.tryLock())
            return
        scope.launch {
            var response = withContext(Dispatchers.IO) { HttpClient.get(
                defaultUrl + "/load_weather/" + loc.latitude + "/" + loc.longitude, weatherToken
            ) }
            if(response == null)
                response = withContext(Dispatchers.IO) { HttpClient.get(directUrl + "&lat=" + loc.latitude + "&lon=" + loc.longitude) }
            val currentWeather = response?.getJSONObject("current")
            val region = response?.getString("timezone")?.split("/")?.get(1) ?: ""
            // Order is important
            updateWeather(currentWeather, region);
            updateClouds(currentWeather);
            mutex.unlock();
        }
    }

}