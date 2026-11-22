package com.example.proyecto_apliacion.data.repository

import android.util.Log
import com.example.proyecto_apliacion.data.dto.toEntity
import com.example.proyecto_apliacion.data.dto.toEntradaDTO
import com.example.proyecto_apliacion.data.local.Usuario
import com.example.proyecto_apliacion.data.local.UsuarioDao
import com.example.proyecto_apliacion.data.remote.ApiService
import com.example.proyecto_apliacion.data.remote.UsuarioApiService
import kotlinx.coroutines.flow.Flow

class DefaultUsuarioRepository(
    private val usuarioDao: UsuarioDao,
    private val apiService: UsuarioApiService
) : UsuarioRepository {

    override fun observarUsuariosLocales(): Flow<List<Usuario>> = usuarioDao.observar()

    override suspend fun login(usuario: String, clave: String): Usuario? {
        return usuarioDao.buscarPorCredenciales(usuario, clave)
    }

    override suspend fun registrarUsuario(usuario: Usuario): Result<Usuario> {
        return try {
            val response = apiService.crearUsuario(usuario.toEntradaDTO())
            if (response.isSuccessful && response.body() != null) {
                val usuarioCreado = response.body()!!.toEntity(claveLocal = usuario.clave)
                usuarioDao.guardar(usuarioCreado)
                Result.success(usuarioCreado)
            } else {
                val errorDetalle = response.errorBody()?.string() ?: "Sin detalle"
                Log.e("AWS_400_DETALLE", "Código: ${response.code()} | Cuerpo: $errorDetalle")
                Result.failure(Exception("Error ${response.code()}: $errorDetalle"))
            }
        } catch (e: Exception) {
            Log.e("AWS_NETWORK_ERROR", "Fallo al conectar con AWS: ${e.message}", e)
            try {
                val existente = usuarioDao.buscarPorCorreo(usuario.correo)
                if (existente != null) {
                    Result.failure(Exception("Sin conexión. El correo ya está registrado localmente."))
                } else {
                    usuarioDao.registrar(usuario)
                    Result.success(usuario)
                }
            } catch (dbEx: Exception) {
                Result.failure(Exception("Error crítico local: ${dbEx.message}"))
            }
        }
    }

    override suspend fun actualizarUsuario(usuario: Usuario): Result<Usuario> {
        return try {
            val idAws = usuario.idRemoto ?: usuarioDao.buscarPorNombre(usuario.username)?.idRemoto

            if (idAws != null) {
                val response = apiService.actualizarUsuario(id = idAws.toLong(), entrada = usuario.toEntradaDTO())
                if (idAws != null) {
                    val dtoActualizado = apiService.actualizarUsuario(id = idAws.toLong(), entrada = usuario.toEntradaDTO())
                    val usuarioActualizado = dtoActualizado.toEntity(claveLocal = usuario.clave)
                    usuarioDao.actualizar(usuarioActualizado)
                    Result.success(usuarioActualizado)
                } else {
                    usuarioDao.actualizar(usuario)
                    Result.success(usuario)
                }
            } else {
                // Si es un usuario solo local (como los creados por defecto sin idRemoto)
                usuarioDao.actualizar(usuario)
                Result.success(usuario)
            }
        } catch (e: Exception) {
            Log.e("AWS_SYNC", "Fallo AWS al actualizar, guardando en local: ${e.message}")
            try {
                usuarioDao.actualizar(usuario)
                Result.success(usuario)
            } catch (dbEx: Exception) {
                Result.failure(Exception("Error local al actualizar: ${dbEx.message}"))
            }
        }
    }



    override suspend fun eliminarUsuario(username: String): Result<Unit> {
        return try {
            val usuarioLocal = usuarioDao.buscarPorNombre(username) // o busca en tu lista
            val idAws = usuarioLocal?.idRemoto

            if (idAws != null) {
                val response = apiService.eliminarUsuario(idAws)
                if (response.isSuccessful) {
                    usuarioDao.eliminar(username)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Error AWS al eliminar: ${response.code()}"))
                }
            } else {
                // Si era un usuario solo local (como los creados por defecto sin idRemoto)
                usuarioDao.eliminar(username)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("AWS_SYNC", "Fallo AWS al eliminar, borrando en local: ${e.message}")
            usuarioDao.eliminar(username)
            Result.success(Unit)
        }
    }
    override suspend fun sincronizarUsuariosDesdeAws() {
        try {
            val usuariosDesdeAws = apiService.listarUsuarios()
            usuariosDesdeAws.forEach { dto ->
                val usuarioRemoto = dto.toEntity(claveLocal = "")
                usuarioDao.guardar(usuarioRemoto)
            }
        } catch (e: Exception) {
            Log.e("AWS_SYNC", "No se pudo sincronizar usuarios desde AWS: ${e.message}")
        }
    }
}