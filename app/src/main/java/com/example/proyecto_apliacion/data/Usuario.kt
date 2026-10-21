package com.example.proyecto_apliacion.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // SQLite autoincrementará este ID (1, 2, 3...)
    val nombre: String,
    val correo: String,
    val clave: String,
    val rol: String = "CLIENTE"
)