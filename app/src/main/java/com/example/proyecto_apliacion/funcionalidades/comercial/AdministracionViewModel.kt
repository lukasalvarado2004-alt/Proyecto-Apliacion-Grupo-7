package com.example.proyecto_apliacion.funcionalidades.comercial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.proyecto_apliacion.data.Categoria
import com.example.proyecto_apliacion.data.ComercialRepository
import com.example.proyecto_apliacion.data.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdministracionViewModel(private val repository: ComercialRepository) : ViewModel() {

    val usuarios = repository.observarUsuarios()
    val categorias = repository.observarCategorias()

    private val _aviso = MutableStateFlow("")
    val aviso: StateFlow<String> = _aviso.asStateFlow()

    init {
        viewModelScope.launch {
            if (repository.contarUsuarios() == 0) {
                listOf(
                    Usuario("admin", "Administrador", "ADMIN", correo = "admin@kaiju.cl"),
                    Usuario("ventas", "Personal de ventas", "VENTAS", correo = "ventas@kaiju.cl"),
                    Usuario("inventario", "Encargado de inventario", "INVENTARIO", correo = "inventario@kaiju.cl")
                ).forEach { repository.guardarUsuario(it) }
            }
            if (repository.contarCategorias() == 0) {
                listOf(
                    Categoria("Ropa", "Prendas y vestuario"),
                    Categoria("Mochilas", "Bolsos y mochilas"),
                    Categoria("Accesorios", "Accesorios Kaiju")
                ).forEach { repository.guardarCategoria(it) }
            }
        }
    }

    fun guardarUsuario(username: String, nombreUsuario: String, correo: String, rol: String, onExito: () -> Unit) {
        if (username.isBlank() || nombreUsuario.isBlank()) {
            _aviso.value = "Completa usuario y nombre"
            return
        }
        viewModelScope.launch {
            repository.guardarUsuario(Usuario(username.trim(), nombreUsuario.trim(), rol, true, correo.trim()))
            _aviso.value = "Usuario guardado"
            onExito()
        }
    }

    fun cambiarEstadoUsuario(username: String, activo: Boolean) {
        viewModelScope.launch {
            repository.cambiarEstadoUsuario(username, activo)
        }
    }

    fun eliminarUsuario(username: String) {
        if (username == "admin") return
        viewModelScope.launch {
            repository.eliminarUsuario(username)
        }
    }

    fun guardarCategoria(nombre: String, descripcion: String, onExito: () -> Unit) {
        if (nombre.trim().length < 2) {
            _aviso.value = "El nombre debe tener al menos 2 caracteres"
            return
        }
        viewModelScope.launch {
            repository.guardarCategoria(Categoria(nombre.trim(), descripcion.trim()))
            _aviso.value = "Categoría guardada"
            onExito()
        }
    }

    fun cambiarEstadoCategoria(nombre: String, activa: Boolean) {
        viewModelScope.launch {
            repository.cambiarEstadoCategoria(nombre, activa)
        }
    }

    fun eliminarCategoria(nombre: String) {
        viewModelScope.launch {
            repository.eliminarCategoria(nombre)
        }
    }

    fun limpiarAviso() {
        _aviso.value = ""
    }
}

class AdministracionViewModelFactory(private val repository: ComercialRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdministracionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdministracionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
