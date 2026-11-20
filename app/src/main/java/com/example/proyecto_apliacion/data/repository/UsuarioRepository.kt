package com.example.proyecto_apliacion.data.repository

import com.example.proyecto_apliacion.data.local.Usuario
import kotlinx.coroutines.flow.Flow

interface UsuarioRepository {
    fun observarUsuariosLocales(): Flow<List<Usuario>>
    suspend fun login(usuario: String, clave: String): Usuario?
    suspend fun sincronizarUsuariosDesdeAws()
    suspend fun registrarUsuario(usuario: Usuario): Result<Usuario>
    suspend fun actualizarUsuario(usuario: Usuario): Result<Usuario>

    suspend fun eliminarUsuario(username: String): Result<Unit>
}