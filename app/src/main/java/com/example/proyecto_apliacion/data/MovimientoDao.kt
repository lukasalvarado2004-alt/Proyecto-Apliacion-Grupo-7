package com.example.proyecto_apliacion.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MovimientoDao {
    @Query("SELECT * FROM movimientos ORDER BY fechaMillis DESC")
    fun observarTodos(): Flow<List<Movimiento>>

    @Insert
    suspend fun insertar(movimiento: Movimiento)
}
