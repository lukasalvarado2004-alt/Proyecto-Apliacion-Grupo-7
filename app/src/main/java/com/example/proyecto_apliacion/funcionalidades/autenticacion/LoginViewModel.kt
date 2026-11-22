package com.example.proyecto_apliacion.funcionalidades.autenticacion

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto_apliacion.data.DefaultAppContainer
import com.example.proyecto_apliacion.data.dto.LoginDTO
import com.example.proyecto_apliacion.data.local.Usuario
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LoginViewModel : ViewModel() {
    var usuario by mutableStateOf("")
        private set
    var contrasena by mutableStateOf("")
        private set
    var mensajeError by mutableStateOf<String?>(null)
        private set
    var loginExitoso by mutableStateOf(false)
        private set
    var esIngresoCliente by mutableStateOf(false)
        private set

    fun consumirLoginExitoso() {
        loginExitoso = false
        esIngresoCliente = false
        usuario = ""
        contrasena = ""
        mensajeError = null
    }

    fun onUsuarioCambiado(nuevoUsuario: String) {
        usuario = nuevoUsuario
        mensajeError = null
    }

    fun onContrasenaCambiada(nuevaContrasena: String) {
        contrasena = nuevaContrasena
        mensajeError = null
    }

    fun iniciarSesion(context: Context) {
        val usuarioLimpio = usuario.trim().lowercase()
        val claveLimpia = contrasena.trim()
        if (usuarioLimpio.isBlank() || claveLimpia.isBlank()) {
            mensajeError = "Por favor, completa todos los campos."
            loginExitoso = false
            return
        }

        // Accesos de operador requeridos para la evaluación (Modo Local/Demo)
        val claveOperador = when (usuarioLimpio) {
            "admin" -> "1234"
            "ventas" -> "5678"
            "inventario" -> "0000"
            else -> null
        }
        if (claveOperador != null) {
            if (claveLimpia == claveOperador) {
                esIngresoCliente = false
                loginExitoso = true
                mensajeError = null
            } else {
                loginExitoso = false
                mensajeError = "Usuario o contraseña incorrectos."
            }
            return
        }

        // Conectamos a través del AppContainer para obtener los servicios de red seguros
        val appContainer = DefaultAppContainer(context)
        val apiService = appContainer.apiService
        val sessionManager = appContainer.sessionManager

        viewModelScope.launch {
            try {
                // 1. Petición directa al backend AWS
                val peticion = LoginDTO(
                    correo = usuarioLimpio,
                    contrasena = claveLimpia
                )
                val respuesta = apiService.login(peticion)

                // 2. Guardar el JWT y el rol de forma segura en el dispositivo
                sessionManager.guardarToken(respuesta.accessToken)
                sessionManager.guardarRol(respuesta.rol ?: "CLIENTE")

                // 3. Notificar a la UI del éxito
                usuario = usuarioLimpio
                esIngresoCliente = respuesta.rol == "CLIENTE"
                loginExitoso = true
                mensajeError = null

            } catch (e: HttpException) {
                loginExitoso = false
                esIngresoCliente = false
                mensajeError = if (e.code() == 401) {
                    "Usuario o contraseña incorrectos."
                } else {
                    "Error del servidor (${e.code()})"
                }
            } catch (e: Exception) {
                loginExitoso = false
                esIngresoCliente = false
                mensajeError = "No se pudo conectar con el servidor. Verifica tu internet."
            }
        }
    }

    fun registrarUsuario(
        context: Context,
        nombre: String,
        correo: String,
        clave: String,
        onExito: () -> Unit,
        onError: (String) -> Unit
    ) {
        val nombreLimpio = nombre.trim()
        val correoLimpio = correo.trim().lowercase()
        val claveLimpia = clave.trim()
        if (nombreLimpio.isBlank() || correoLimpio.isBlank() || claveLimpia.isBlank()) {
            onError("Completa todos los campos.")
            return
        }

        val repository = DefaultAppContainer(context).usuarioRepository

        viewModelScope.launch {
            val nuevoUsuario = Usuario(
                username = correoLimpio,
                nombre = nombreLimpio,
                rol = "CLIENTE",
                activo = true,
                correo = correoLimpio,
                clave = claveLimpia
            )

            // Envía el registro a AWS (POST /api/v1/usuarios) y respalda en Room
            val resultado = repository.registrarUsuario(nuevoUsuario)

            resultado.fold(
                onSuccess = {
                    onExito()
                },
                onFailure = { error ->
                    // Si AWS devuelve 409 (Conflict), captura el mensaje
                    val mensaje = if (error.message?.contains("409") == true) {
                        "El correo ya está registrado en el servidor."
                    } else {
                        error.message ?: "No se pudo registrar la cuenta."
                    }
                    onError(mensaje)
                }
            )
        }
    }
}