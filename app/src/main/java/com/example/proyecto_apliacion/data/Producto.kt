package com.example.proyecto_apliacion.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productos")
data class Producto(
    @PrimaryKey val id: String,
    val nombre: String,
    val categoria: String,
    val stock: Int,
    val precio: Int,
    val imagenUri: String? = null
)
