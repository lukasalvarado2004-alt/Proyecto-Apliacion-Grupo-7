package com.example.proyecto_apliacion.data.remote

import com.example.proyecto_apliacion.data.dto.UsuarioEntradaDTO
import com.example.proyecto_apliacion.data.dto.UsuarioSalidaDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @GET("api/v1/usuarios")
    suspend fun obtenerUsuarios(): Response<List<UsuarioSalidaDTO>>

    @POST("api/v1/usuarios")
    suspend fun crearUsuario(
        @Body usuario: UsuarioEntradaDTO
    ): Response<UsuarioSalidaDTO>

    @PUT("api/v1/usuarios/{id}")
    suspend fun actualizarUsuario(
        @Path("id") id: Long,
        @Body usuario: UsuarioEntradaDTO
    ): Response<UsuarioSalidaDTO>

    @DELETE("api/v1/usuarios/{id}")
    suspend fun eliminarUsuario(
        @Path("id") id: Long
    ): Response<Unit>
}