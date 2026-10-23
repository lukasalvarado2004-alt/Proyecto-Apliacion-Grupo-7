package com.example.proyecto_apliacion.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class Usuario(@PrimaryKey val username: String, val nombre: String, val rol: String, val activo: Boolean = true, val correo: String = "", val clave: String = "")

@Entity(tableName = "categorias")
data class Categoria(@PrimaryKey val nombre: String, val descripcion: String = "", val activa: Boolean = true)

@Entity(tableName = "ventas")
data class Venta(@PrimaryKey(autoGenerate = true) val id: Long = 0, val fechaMillis: Long = System.currentTimeMillis(), val usuario: String, val total: Long, val estado: String = "COMPLETADA")

@Entity(tableName = "detalle_ventas")
data class DetalleVenta(@PrimaryKey(autoGenerate = true) val id: Long = 0, val ventaId: Long, val productoId: String, val nombreProducto: String, val cantidad: Int, val precioUnitario: Int, val subtotal: Long)
