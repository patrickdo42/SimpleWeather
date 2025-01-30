package com.example.weather.ui

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.android.volley.ClientError
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.ServerError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.weather.databinding.ActivityMainBinding
import com.example.weather.R
import com.example.weather.utils.TOKEN
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.math.ceil

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Remove layout limits
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        // Set up views and handle the search button click
        binding.apply {
            btnSearch.setOnClickListener {
                val userLocationInput = etLocation.text.toString().trim()

                if (userLocationInput.isNotEmpty()) {
                    // Hide error message if the input is valid
                    showError(false)

                    // Fetch weather data for the provided location
                    requestToServer(userLocationInput)
                } else {
                    // Show error message if the input is empty
                    showError(true, "Please enter a location to search")
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun requestToServer(location: String) {
        // Volley request
        val queue = Volley.newRequestQueue(this)
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$location&units=metric&appid=$TOKEN"

        val stringRequest = StringRequest(Request.Method.GET, url,
            Response.Listener { response ->
                // Handle API response
            try {
                // Parse the response into a JSONObject
                val jsonResponse = JSONObject(response)

                // Extract temperature, humidity, and wind speed
                val mainObject: JSONObject = jsonResponse.getJSONObject("main")
                val temperature = mainObject.getDouble("temp")
                val humidity = mainObject.getInt("humidity")
                val windObject: JSONObject = jsonResponse.getJSONObject("wind")
                val windSpeed = windObject.getDouble("speed")

                // Extract weather details (description and main status)
                val weatherArray: JSONArray = jsonResponse.getJSONArray("weather")
                var description: String? = null
                var main: String? = null

                if (weatherArray.length() > 0) {
                    val weatherObject = weatherArray.getJSONObject(0)
                    description = weatherObject.getString("description")
                    main = weatherObject.getString("main")
                }

                // Update UI with weather data
                binding.layoutWeather.apply {
                    root.visibility = View.VISIBLE
                    locationTitle.text = location.capitalize(Locale.ROOT)
                    val roundedTemperature = ceil(temperature).toInt().toString()
                    tvTemperature.text = "$roundedTemperature°C"
                    tvWindSpeed.text = "$windSpeed m/s"
                    tvHumidityValue.text = "$humidity%"
                    tvDescription.text = description ?: "No description available"

                    // Set weather status icon based on the "main" weather status
                    when (main) {
                        "Clear" -> ivStatus.setImageResource(R.drawable.clear)
                        "Rain" -> ivStatus.setImageResource(R.drawable.rain)
                        "Snow" -> ivStatus.setImageResource(R.drawable.snow)
                        "Clouds" -> ivStatus.setImageResource(R.drawable.cloudy)
                        else -> ivStatus.setImageResource(R.drawable.clear) // Fallback icon for unknown conditions
                    }

                    // Click listener to hide weather data layout
                    ivBack.setOnClickListener {
                        root.visibility = View.GONE
                    }
                }
            } catch (e: JSONException) {
                // Handle JSON parsing errors
                Toast.makeText(this@MainActivity, "Error parsing data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }) { error ->
            // Handle network errors and server errors
            when (error) {
                is ClientError, is ServerError -> {
                    val response: NetworkResponse? = error.networkResponse
                    val statusCode = response?.statusCode
                    if (statusCode == 404) {
                        showError(true, "Location not found")
                    } else {
                        Toast.makeText(this, "Error connecting to the API: $statusCode", Toast.LENGTH_SHORT).show()
                    }
                }

                else -> {
                    Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Add the request to the Volley queue
        queue.add(stringRequest)
    }

    private fun showError(haveError: Boolean, message: String? = null) {
        binding.tvError.apply {
            if (haveError) {
                text = message
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
    }
}
