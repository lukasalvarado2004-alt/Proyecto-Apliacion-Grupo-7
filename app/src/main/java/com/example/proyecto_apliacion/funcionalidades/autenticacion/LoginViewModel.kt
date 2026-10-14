package com.example.proyecto_apliacion.funcionalidades.autenticacion


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {

    // Estados para controlar lo que el usuario escribe en las cajas de texto
    var usuario by mutableStateOf("")
        private set

    var contrasena by mutableStateOf("")
        private set

    // Estado para controlar los mensajes de error en la interfaz
    var mensajeError by mutableStateOf<String?>(null)
        private set

    // Estado para avisar a la vista si el ingreso fue exitoso o no
    var loginExitoso by mutableStateOf(false)
        private set

    // Consume el resultado para que el ViewModel no vuelva a autenticar automáticamente
    // cuando el usuario cierre sesión y regrese a la pantalla de acceso.
    fun consumirLoginExitoso() {
        loginExitoso = false
        usuario = ""
        contrasena = ""
        mensajeError = null
    }

    // Funciones para actualizar el estado desde la vista
    fun onUsuarioCambiado(nuevoUsuario: String) {
        usuario = nuevoUsuario
        mensajeError = null // Limpia el error al escribir
    }

    fun onContrasenaCambiada(nuevaContrasena: String) {
        contrasena = nuevaContrasena
        mensajeError = null
    }

    // Lógica para validar las credenciales de los perfiles solicitados
    fun iniciarSesion() {
        if (usuario.isBlank() || contrasena.isBlank()) {
            mensajeError = "Por favor, completa todos los campos."
            return
        }

        // Credenciales ficticias simulando los roles exigidos en la entrega (Admin, Ventas, Inventario)
        if ((usuario == "admin" && contrasena == "1234") ||
            (usuario == "ventas" && contrasena == "5678") ||
            (usuario == "inventario" && contrasena == "0000")) {

            loginExitoso = true
            mensajeError = null
        } else {
            mensajeError = "Usuario o contraseña incorrectos."
            loginExitoso = false
        }
    }
}