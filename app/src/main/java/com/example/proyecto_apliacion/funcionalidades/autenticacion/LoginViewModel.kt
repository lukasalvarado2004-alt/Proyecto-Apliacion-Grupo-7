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
        if (usuario.isBlank() || contrasena.isBlank()) {
            mensajeError = "Por favor, completa todos los campos."
            return
        }

        val cleanUser = usuario.trim().lowercase()
        val cleanPass = contrasena.trim()

        // 1. Validar operadores de administración
        if ((cleanUser == "admin" && cleanPass == "1234") ||
            (cleanUser == "ventas" && cleanPass == "5678") ||
            (cleanUser == "inventario" && cleanPass == "0000")) {

            esIngresoCliente = false
            loginExitoso = true
            mensajeError = null
            return
        }

        // 2. Validar cliente registrado en la tabla 'usuarios' de SQLite
        val db = KaijuDatabase.obtener(context)
        viewModelScope.launch {
            val clienteEncontrado = db.usuarioDao().buscarPorCredenciales(cleanUser, cleanPass)
            val esClienteDemo = cleanUser == "cliente" && cleanPass == "1234"

            if (clienteEncontrado != null || esClienteDemo) {
                esIngresoCliente = true
                loginExitoso = true
                mensajeError = null
            } else {
                mensajeError = "Usuario o contraseña incorrectos."
                loginExitoso = false
                esIngresoCliente = false
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
        val db = KaijuDatabase.obtener(context)
        viewModelScope.launch {
            val correoLimpio = correo.trim().lowercase()
            val existente = db.usuarioDao().buscarPorCorreo(correoLimpio)

            if (existente != null) {
                onError("El correo ya está registrado.")
            } else {
                val nuevoUsuario = Usuario(
                    nombre = nombre.trim(),
                    correo = correoLimpio,
                    clave = clave.trim(),
                    rol = "CLIENTE"
                )
                db.usuarioDao().registrar(nuevoUsuario)
                onExito()
            }
        }
    }
}