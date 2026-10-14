package com.example.proyecto_apliacion.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movimientos")
data class Movimiento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productoId: String,
    val nombreProducto: String,
    val tipo: String,
    val cantidad: Int,
    val detalle: String,
    val fechaMillis: Long = System.currentTimeMillis()
)
