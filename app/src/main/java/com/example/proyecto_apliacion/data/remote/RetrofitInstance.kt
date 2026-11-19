package com.example.proyecto_apliacion.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val AWS_BASE_URL = "http://52.201.137.48:8081/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(AWS_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    val usuarioApi: UsuarioApiService by lazy {
        Retrofit.Builder()
            .baseUrl(AWS_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UsuarioApiService::class.java)
    }
}