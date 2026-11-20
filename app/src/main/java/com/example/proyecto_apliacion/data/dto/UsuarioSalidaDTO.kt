package com.example.proyecto_apliacion.data.dto

data class UsuarioSalidaDTO(
    val id: Long,
    val nombre: String,
    val email: String,
    val telefono: String,
    val direccion: String,
    val rol: String,
    val fechaRegistro: String
)