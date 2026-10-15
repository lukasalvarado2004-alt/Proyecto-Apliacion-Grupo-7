package com.example.proyecto_apliacion.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos ORDER BY nombre")
    fun observarTodos(): Flow<List<Producto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(producto: Producto)

    @Query("DELETE FROM productos WHERE id = :id")
    suspend fun eliminar(id: String)

    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: String): Producto?

    @Query("SELECT COUNT(*) FROM productos")
    suspend fun contar(): Int

    @Update
    suspend fun updateProducto(producto: Producto)
}
