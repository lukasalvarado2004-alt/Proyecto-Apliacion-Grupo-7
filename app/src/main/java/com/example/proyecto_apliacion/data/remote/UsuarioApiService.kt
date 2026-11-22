package com.example.proyecto_apliacion.data.remote

import com.example.proyecto_apliacion.data.dto.LoginDTO
import com.example.proyecto_apliacion.data.dto.TokenDTO
import com.example.proyecto_apliacion.data.dto.UsuarioEntradaDTO
import com.example.proyecto_apliacion.data.dto.UsuarioSalidaDTO
import retrofit2.Response
import retrofit2.http.*

interface UsuarioApiService {

    // 1. Listar todos los usuarios
    @GET("api/v1/usuarios")
    suspend fun listarUsuarios(): List<UsuarioSalidaDTO>

    // 2. Buscar usuario por ID
    @GET("api/v1/usuarios/{id}")
    suspend fun buscarUsuario(@Path("id") id: Long): UsuarioSalidaDTO

    // 3. Buscar usuario por correo
    @GET("api/v1/usuarios/email/{email}")
    suspend fun buscarUsuarioPorEmail(@Path("email") email: String): UsuarioSalidaDTO

    // 4. Registrar un nuevo usuario
    @POST("api/v1/usuarios")
    suspend fun crearUsuario(@Body entrada: UsuarioEntradaDTO): Response<UsuarioSalidaDTO>

    @POST("api/v1/auth/login")
    suspend fun login(@Body credenciales: LoginDTO): TokenDTO

    // 5. Actualizar usuario
    @PUT("api/v1/usuarios/{id}")
    suspend fun actualizarUsuario(
        @Path("id") id: Long,
        @Body entrada: UsuarioEntradaDTO
    ): UsuarioSalidaDTO

    // 6. Eliminar usuario
    @DELETE("api/v1/usuarios/{id}")
    suspend fun eliminarUsuario(@Path("id") id: Long): Response<Unit>
}