package com.example.proyecto_apliacion.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_ROL = "user_rol"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "sesion_segura_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun guardarToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
    }

    fun obtenerToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    fun guardarRol(rol: String) {
        sharedPreferences.edit().putString(KEY_ROL, rol).apply()
    }

    fun obtenerRol(): String? {
        return sharedPreferences.getString(KEY_ROL, null)
    }

    fun cerrarSesion() {
        sharedPreferences.edit().clear().apply()
    }
}