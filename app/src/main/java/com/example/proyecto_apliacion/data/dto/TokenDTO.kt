package com.example.proyecto_apliacion.data.dto

import com.google.gson.annotations.SerializedName

data class TokenDTO(
    val accessToken: String,
    val token: String? = null,
    val tipo: String? = null,
    val expiraEnSegundos: Long? = null,
    val usuarioId: Long? = null,
    val rol: String? = null
)
