package com.example.proyecto_apliacion.data.remote


import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class AuthAuthenticator(private val sessionManager: SessionManager) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // Entra aquí si el servidor devuelve un 401 Unauthorized

        // 1. Borramos los datos locales porque la sesión ya no es válida
        sessionManager.cerrarSesion()

        // 2. Aquí más adelante implementaremos un evento global para mandar al usuario al LoginScreen.
        // Por ahora, simplemente retornamos null para que la petición falle limpiamente.
        return null
    }
}