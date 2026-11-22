package com.example.proyecto_apliacion.data.repository

import android.util.Log
import com.example.proyecto_apliacion.data.dto.CambioRolDTO
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
            val dtoEntrada = usuario.toEntradaDTO()
            val response = apiService.crearUsuario(dtoEntrada)
            if (response.isSuccessful && response.body() != null) {
                var dtoFinal = response.body()!!
                // El backend siempre crea como CLIENTE; si se pidió otro rol, aplicamos PATCH /{id}/rol
                val rolDeseado = dtoEntrada.rol
                if (rolDeseado != "CLIENTE" && dtoFinal.id != null) {
                    try {
                        dtoFinal = apiService.cambiarRolUsuario(
                            id = dtoFinal.id,
                            cambioRol = CambioRolDTO(rol = rolDeseado)
                        )
                    } catch (e: Exception) {
                        Log.e("AWS_ROL", "Usuario creado como CLIENTE, pero falló PATCH de rol: ${e.message}")
                    }
                }
                val usuarioCreado = dtoFinal.toEntity(claveLocal = usuario.clave)
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
            val localExistente = usuarioDao.buscarPorNombre(usuario.username)
                ?: usuarioDao.buscarPorCorreo(usuario.correo)
            val idAws = usuario.idRemoto ?: localExistente?.idRemoto

            if (idAws != null) {
                val dtoEntrada = usuario.toEntradaDTO().copy(
                    contrasena = if (usuario.clave.length >= 8) usuario.clave else "12345678"
                )

                // 1. PUT actualiza nombre, teléfono y dirección
                var dtoRespuesta = apiService.actualizarUsuario(
                    id = idAws.toLong(),
                    entrada = dtoEntrada
                )

                // 2. PATCH actualiza el rol si cambió respecto al rol actual del servidor
                val rolActualServidor = dtoRespuesta.rol?.trim()?.uppercase()
                val rolNuevo = dtoEntrada.rol.trim().uppercase()
                if (rolNuevo != rolActualServidor) {
                    dtoRespuesta = apiService.cambiarRolUsuario(
                        id = idAws.toLong(),
                        cambioRol = CambioRolDTO(rol = rolNuevo)
                    )
                }

                val usuarioActualizado = dtoRespuesta.toEntity(claveLocal = usuario.clave).copy(
                    username = usuario.username,
                    idRemoto = idAws
                )

                usuarioDao.guardar(usuarioActualizado)
                Result.success(usuarioActualizado)
            } else {
                val usuarioLocal = usuario.copy(rol = usuario.toEntradaDTO().rol)
                usuarioDao.guardar(usuarioLocal)
                Result.success(usuarioLocal)
            }
        } catch (e: retrofit2.HttpException) {
            val detalleError = when (e.code()) {
                409 -> "No puedes cambiar el rol de tu propia cuenta."
                403 -> "Solo un administrador puede cambiar roles."
                else -> e.response()?.errorBody()?.string() ?: e.message()
            }
            Log.e("AWS_SYNC", "Error HTTP ${e.code()} al actualizar: $detalleError")
            Result.failure(Exception("Error (${e.code()}): $detalleError"))
        } catch (e: Exception) {
            Log.e("AWS_SYNC", "Error al actualizar usuario: ${e.message}")
            Result.failure(Exception("No se pudo actualizar el usuario: ${e.message}"))
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