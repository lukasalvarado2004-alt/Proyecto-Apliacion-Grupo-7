package com.example.proyecto_apliacion.funcionalidades.comercial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.proyecto_apliacion.data.local.Categoria
import com.example.proyecto_apliacion.data.local.Usuario
import com.example.proyecto_apliacion.data.repository.ComercialRepository
import com.example.proyecto_apliacion.data.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdministracionViewModel(
    private val usuarioRepository: UsuarioRepository,
    private val comercialRepository: ComercialRepository
) : ViewModel() {

    val usuarios = usuarioRepository.observarUsuariosLocales()
    val categorias = comercialRepository.observarCategorias()

    private val _aviso = MutableStateFlow("")
    val aviso: StateFlow<String> = _aviso.asStateFlow()

    init {
        viewModelScope.launch {
            // Sincroniza desde AWS al iniciar
            usuarioRepository.sincronizarUsuariosDesdeAws()

            if (comercialRepository.contarCategorias() == 0) {
                listOf(
                    Categoria("Ropa", "Prendas y vestuario"),
                    Categoria("Mochilas", "Bolsos y mochilas"),
                    Categoria("Accesorios", "Accesorios Kaiju")
                ).forEach { comercialRepository.guardarCategoria(it) }
            }
        }
    }

    fun guardarUsuario(username: String, nombreUsuario: String, correo: String, rol: String, onExito: () -> Unit) {
        if (username.isBlank() || nombreUsuario.isBlank()) {
            _aviso.value = "Completa usuario y nombre"
            return
        }
        viewModelScope.launch {
            val resultado = usuarioRepository.registrarUsuario(
                Usuario(
                    username = username.trim(),
                    nombre = nombreUsuario.trim(),
                    rol = rol,
                    activo = true,
                    correo = correo.trim(),
                    clave = "12345678" // Mínimo 8 caracteres exigido por AWS
                )
            )

            resultado.fold(
                onSuccess = {
                    _aviso.value = "Usuario guardado en sistema y servidor"
                    onExito()
                },
                onFailure = {
                    _aviso.value = "Error al guardar: ${it.message}"
                }
            )
        }
    }

    fun cambiarEstadoUsuario(username: String, activo: Boolean) {
        viewModelScope.launch {
            // Nota: Aquí necesitarías un método en AWS para actualizar (PUT).
            // Si no lo tienes, al menos actualiza en Room a través de un dao o repository
            // Por ahora, como es solo frontend, dejamos un aviso
            _aviso.value = "Opción requiere actualización (PUT) en API"
        }
    }

    private val _usuarioEliminando = MutableStateFlow<String?>(null)
    val usuarioEliminando: StateFlow<String?> = _usuarioEliminando.asStateFlow()

    fun eliminarUsuario(username: String) {
        if (username == "admin" || _usuarioEliminando.value != null) return
        viewModelScope.launch {
            _usuarioEliminando.value = username
            try {
                usuarioRepository.eliminarUsuario(username)
                    .onSuccess { _aviso.value = "Usuario eliminado correctamente" }
                    .onFailure { _aviso.value = it.message ?: "No se pudo eliminar el usuario" }
            } finally {
                _usuarioEliminando.value = null
            }
        }
    }

    fun guardarCategoria(nombre: String, descripcion: String, onExito: () -> Unit) {
        if (nombre.trim().length < 2) {
            _aviso.value = "El nombre debe tener al menos 2 caracteres"
            return
        }
        viewModelScope.launch {
            comercialRepository.guardarCategoria(Categoria(nombre.trim(), descripcion.trim()))
            _aviso.value = "Categoría guardada"
            onExito()
        }
    }

    fun cambiarEstadoCategoria(nombre: String, activa: Boolean) {
        viewModelScope.launch {
            comercialRepository.cambiarEstadoCategoria(nombre, activa)
        }
    }

    fun eliminarCategoria(nombre: String) {
        viewModelScope.launch {
            comercialRepository.eliminarCategoria(nombre)
        }
    }

    fun limpiarAviso() {
        _aviso.value = ""
    }

    private val _guardandoEdicion = MutableStateFlow(false)
    val guardandoEdicion: StateFlow<Boolean> = _guardandoEdicion.asStateFlow()

    fun editarUsuario(
        usuarioOriginal: Usuario,
        nuevoNombre: String,
        nuevoCorreo: String,
        nuevoTelefono: String,
        nuevaDireccion: String,
        nuevoRol: String,
        onExito: () -> Unit
    ) {
        if (nuevoNombre.trim().isBlank() || nuevoCorreo.trim().isBlank()) {
            _aviso.value = "El nombre y el correo son obligatorios"
            return
        }
        viewModelScope.launch {
            _guardandoEdicion.value = true
            try {
                // .copy() conserva idRemoto, username y clave automáticamente
                val usuarioEditado = usuarioOriginal.copy(
                    nombre = nuevoNombre.trim(),
                    correo = nuevoCorreo.trim().lowercase(),
                    telefono = nuevoTelefono.trim(),
                    direccion = nuevaDireccion.trim(),
                    rol = nuevoRol
                )
                usuarioRepository.actualizarUsuario(usuarioEditado)
                    .onSuccess {
                        _aviso.value = "Usuario actualizado correctamente"
                        onExito()
                    }
                    .onFailure {
                        _aviso.value = it.message ?: "No se pudo actualizar el usuario"
                    }
            } finally {
                _guardandoEdicion.value = false
            }
        }
    }
}

class AdministracionViewModelFactory(
    private val usuarioRepository: UsuarioRepository,
    private val comercialRepository: ComercialRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdministracionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdministracionViewModel(usuarioRepository, comercialRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}