package com.adhiraj.weather.model

import com.google.gson.annotations.SerializedName

data class LocationInfo(
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lng")
    val lng: Double
)