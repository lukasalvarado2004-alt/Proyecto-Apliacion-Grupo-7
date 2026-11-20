package com.example.proyecto_apliacion.data.dto

import com.example.proyecto_apliacion.data.dto.UsuarioEntradaDTO
import com.example.proyecto_apliacion.data.dto.UsuarioSalidaDTO
import com.example.proyecto_apliacion.data.local.Usuario

// 1. De Local (Usuario) -> a AWS (UsuarioEntradaDTO)
fun Usuario.toEntradaDTO(): UsuarioEntradaDTO {
    return UsuarioEntradaDTO(
        nombre = this.nombre,
        correo = this.correo,
        contrasena = this.clave, // Cambiamos 'clave' por 'contrasena'
        telefono = "+56900000000",           // Agregamos el campo faltante
        direccion = "SIN DIRECCION",          // Agregamos el campo faltante
        rol = this.rol
    )
}
// Mapear de la API hacia local (Salida)
// Mapear de la API (UsuarioSalidaDTO) hacia local (Usuario)
fun UsuarioSalidaDTO.toEntity(claveLocal: String): Usuario {
    return Usuario(
        username = this.email ?: this.nombre ?: "",
        nombre = this.nombre ?: "",
        rol = this.rol ?: "CLIENTE",
        activo = true,
        correo = this.email ?: "",
        clave = claveLocal,
        telefono = this.telefono ?: "",
        direccion = this.direccion ?: "",
        idRemoto = this.id
    )
}