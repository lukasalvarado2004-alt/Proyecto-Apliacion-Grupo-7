package com.example.proyecto_apliacion.data.remote

import com.example.proyecto_apliacion.data.dto.LoginDTO
import com.example.proyecto_apliacion.data.dto.TokenDTO
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

    @POST("api/v1/auth/login")
    suspend fun login(@Body credenciales: LoginDTO): TokenDTO

    @GET("api/v1/auth/me")
    suspend fun obtenerMiPerfil(): UsuarioSalidaDTO

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