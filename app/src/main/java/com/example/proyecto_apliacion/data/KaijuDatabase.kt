package com.example.proyecto_apliacion.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Producto::class, Movimiento::class, Usuario::class],
    version = 3, // Subimos a versión 3 para actualizar el esquema de la tabla usuarios
    exportSchema = false
)
abstract class KaijuDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao
    abstract fun movimientoDao(): MovimientoDao
    abstract fun usuarioDao(): UsuarioDao

    companion object {
        @Volatile
        private var INSTANCE: KaijuDatabase? = null

        fun obtener(context: Context): KaijuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KaijuDatabase::class.java,
                    "kaiju_inventory.db"
                )
                    .fallbackToDestructiveMigration() // Elimina la tabla vieja desactualizada y crea la nueva
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}