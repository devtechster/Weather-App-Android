package com.adhiraj.weather.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("places")
    val places: List<Place>
)