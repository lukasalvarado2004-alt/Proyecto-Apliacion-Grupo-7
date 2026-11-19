package com.example.proyecto_apliacion.data


import android.content.Context
import com.example.proyecto_apliacion.data.local.KaijuDatabase
import com.example.proyecto_apliacion.data.remote.RetrofitInstance
import com.example.proyecto_apliacion.data.repository.*

interface AppContainer {
    val productoRepository: ProductoRepository
    val comercialRepository: ComercialRepository
    val usuarioRepository: UsuarioRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

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
            apiService = RetrofitInstance.api,
            usuarioDao = database.usuarioDao()
        )
    }
}