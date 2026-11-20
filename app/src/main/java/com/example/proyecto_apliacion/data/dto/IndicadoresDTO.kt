package com.example.proyecto_apliacion.data.dto

data class MindicadorResponse(
    val dolar: IndicadorDetalle? = null,
    val uf: IndicadorDetalle? = null
)

data class IndicadorDetalle(
    val codigo: String = "",
    val nombre: String = "",
    val valor: Double = 0.0,
    val fecha: String = ""
)