package com.example.proyecto_apliacion.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registrar(usuario: Usuario)

    @Query("SELECT * FROM usuarios WHERE LOWER(correo) = LOWER(:correo) AND clave = :clave ")
    suspend fun buscarPorCredenciales(correo: String, clave: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE LOWER(correo) = LOWER(:correo) ")
    suspend fun buscarPorCorreo(correo: String): Usuario?
}