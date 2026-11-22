package com.example.proyecto_apliacion.data.remote

import com.example.proyecto_apliacion.data.dto.ExchangeRateResponse
import retrofit2.http.GET

interface IndicadoresApiService {
    @GET("v6/latest/USD")
    suspend fun obtenerTasasCambio(): ExchangeRateResponse
}