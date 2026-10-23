package com.example.proyecto_apliacion.funcionalidades.autenticacion

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto_apliacion.data.KaijuDatabase
import com.example.proyecto_apliacion.data.Usuario
import kotlinx.coroutines.launch

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

        // Accesos de operador requeridos para la evaluación.
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

        val db = KaijuDatabase.obtener(context)
        viewModelScope.launch {
            try {
                val cliente = db.usuarioDao().buscarPorCredenciales(usuarioLimpio, claveLimpia)
                val esClienteDemo = usuarioLimpio == "cliente" && claveLimpia == "1234"
                if (cliente != null || esClienteDemo) {
                    // Conserva el correo/usuario ingresado para mostrarlo en la sesión.
                    usuario = cliente?.username ?: usuarioLimpio
                    esIngresoCliente = true
                    loginExitoso = true
                    mensajeError = null
                } else {
                    loginExitoso = false
                    esIngresoCliente = false
                    mensajeError = "Usuario o contraseña incorrectos."
                }
            } catch (_: Exception) {
                loginExitoso = false
                esIngresoCliente = false
                mensajeError = "No se pudo validar el acceso. Inténtalo nuevamente."
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
        val db = KaijuDatabase.obtener(context)
        viewModelScope.launch {
            try {
                val existente = db.usuarioDao().buscarPorCorreo(correoLimpio)
                if (existente != null) {
                    onError("El correo ya está registrado.")
                    return@launch
                }
                val username = correoLimpio
                val nuevoUsuario = Usuario(
                    username = username,
                    nombre = nombreLimpio,
                    rol = "CLIENTE",
                    activo = true,
                    correo = correoLimpio,
                    clave = claveLimpia
                )
                db.usuarioDao().registrar(nuevoUsuario)
                onExito()
            } catch (_: Exception) {
                onError("No se pudo registrar la cuenta. Comprueba si el correo ya existe.")
            }
        }
    }
}
