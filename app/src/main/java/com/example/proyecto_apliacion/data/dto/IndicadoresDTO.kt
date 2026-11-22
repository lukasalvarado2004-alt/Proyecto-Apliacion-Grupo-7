package com.example.proyecto_apliacion.data.dto

import com.google.gson.annotations.SerializedName

data class ExchangeRateResponse(
    val result: String = "",
    @SerializedName("time_last_update_utc")
    val ultimaActualizacion: String = "",
    val rates: Map<String, Double> = emptyMap()
)