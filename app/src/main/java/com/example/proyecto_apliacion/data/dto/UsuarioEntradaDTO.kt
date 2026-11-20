package com.example.proyecto_apliacion.data.dto

data class UsuarioEntradaDTO(
    val nombre: String,
    val correo: String,
    val contrasena: String,
    val telefono: String,
    val direccion: String,
    val rol: String
)