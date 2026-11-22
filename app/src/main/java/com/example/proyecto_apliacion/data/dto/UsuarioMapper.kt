package com.example.proyecto_apliacion.data.dto

import com.example.proyecto_apliacion.data.local.Usuario

private fun normalizarRolParaAws(rolLocal: String): String {
    return when (rolLocal.trim().uppercase()) {
        "ADMIN", "ADMINISTRADOR" -> "ADMINISTRADOR"
        "VENTAS", "VENDEDOR" -> "VENDEDOR"
        "INVENTARIO" -> "INVENTARIO"
        else -> "CLIENTE"
    }
}

// 1. De Local (Usuario) -> a AWS (UsuarioEntradaDTO)
fun Usuario.toEntradaDTO(): UsuarioEntradaDTO {
    return UsuarioEntradaDTO(
        nombre = this.nombre,
        correo = this.correo,
        contrasena = this.clave,
        telefono = this.telefono.ifBlank { "+56900000000" },
        direccion = this.direccion.ifBlank { "SIN DIRECCION" },
        rol = normalizarRolParaAws(this.rol)
    )
}

// 2. De la API (UsuarioSalidaDTO) -> hacia local (Usuario)
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