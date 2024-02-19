package com.adhiraj.weather.model

import com.adhiraj.weather.model.Observation
import com.google.gson.annotations.SerializedName

data class Place(
    @SerializedName("observations")
    val observations: List<Observation>
)