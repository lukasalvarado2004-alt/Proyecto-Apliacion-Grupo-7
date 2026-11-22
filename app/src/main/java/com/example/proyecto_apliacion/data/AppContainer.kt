package com.example.proyecto_apliacion.data

import android.content.Context
import com.example.proyecto_apliacion.data.local.KaijuDatabase
import com.example.proyecto_apliacion.data.remote.ApiService
import com.example.proyecto_apliacion.data.remote.AuthAuthenticator
import com.example.proyecto_apliacion.data.remote.AuthInterceptor
import com.example.proyecto_apliacion.data.remote.IndicadoresApiService
import com.example.proyecto_apliacion.data.remote.RetrofitInstance
import com.example.proyecto_apliacion.data.remote.SessionManager
import com.example.proyecto_apliacion.data.remote.UsuarioApiService
import com.example.proyecto_apliacion.data.repository.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

interface AppContainer {
    val productoRepository: ProductoRepository
    val comercialRepository: ComercialRepository
    val usuarioRepository: UsuarioRepository
    val sessionManager: SessionManager
    val usuarioApiService: UsuarioApiService
    val apiService: ApiService
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    override val sessionManager: SessionManager by lazy {
        SessionManager(context)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .authenticator(AuthAuthenticator(sessionManager))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            // CAMBIA ESTO por la IP de tu PC en la red local, o tu dominio AWS
            .baseUrl("http://52.201.137.48:8081/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    override val usuarioApiService: UsuarioApiService by lazy {
        retrofit.create(UsuarioApiService::class.java)
    }

    private val okHttpExterno: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val retrofitIndicadores: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://open.er-api.com/")
            .client(okHttpExterno)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val indicadoresApiService: IndicadoresApiService by lazy {
        retrofitIndicadores.create(IndicadoresApiService::class.java)
    }

    private val database: KaijuDatabase by lazy {
        KaijuDatabase.obtener(context)
    }

    override val productoRepository: ProductoRepository by lazy {
        ProductoRepository(database.productoDao(), database.movimientoDao())
    }

    override val comercialRepository: ComercialRepository by lazy {
        ComercialRepository(
            database.ventaDao(),
            database.usuarioDao(),
            database.categoriaDao()
        )
    }

    override val usuarioRepository: UsuarioRepository by lazy {
        DefaultUsuarioRepository(
            apiService = usuarioApiService,
            usuarioDao = database.usuarioDao()
        )
    }
}