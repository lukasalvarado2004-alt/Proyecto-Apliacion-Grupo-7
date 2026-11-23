package com.example.proyecto_apliacion

import com.example.proyecto_apliacion.data.dto.*
import com.example.proyecto_apliacion.data.local.Usuario
import com.example.proyecto_apliacion.data.remote.UsuarioApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
internal class MainRule : TestWatcher() {
    private val dispatcher = StandardTestDispatcher()
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

internal fun crearHttpException(codigo: Int, mensaje: String = "Error"): HttpException {
    val body = mensaje.toResponseBody("application/json".toMediaTypeOrNull())
    return HttpException(Response.error<Any>(codigo, body))
}

internal val cliente = Usuario(
    username = "ana@test.cl",
    nombre = "Ana",
    rol = "CLIENTE",
    activo = true,
    correo = "ana@test.cl",
    clave = "test-clave"
)

internal val remoto = UsuarioSalidaDTO(
    id = 7L,
    nombre = "Ana",
    email = cliente.correo,
    telefono = "1",
    direccion = "Calle",
    rol = "CLIENTE",
    fechaRegistro = ""
)

internal val tokenPrueba = TokenDTO(
    accessToken = "token-prueba",
    token = "token-prueba",
    tipo = "Bearer",
    expiraEnSegundos = 900L,
    usuarioId = 7L,
    rol = "CLIENTE"
)

internal class ApiUsuariosPrueba : UsuarioApiService {
    var crear: (UsuarioEntradaDTO) -> Response<UsuarioSalidaDTO> = { Response.success(remoto) }
    var borrar: (Long) -> Response<Unit> = { Response.success(Unit) }
    var rol: (CambioRolDTO) -> UsuarioSalidaDTO = { remoto.copy(rol = it.rol) }
    var editar: (UsuarioEntradaDTO) -> UsuarioSalidaDTO = { remoto.copy(nombre = it.nombre) }
    var loginRespuesta: (LoginDTO) -> TokenDTO = { tokenPrueba }

    override suspend fun listarUsuarios(): List<UsuarioSalidaDTO> = listOf(remoto)
    override suspend fun buscarUsuario(id: Long): UsuarioSalidaDTO = remoto
    override suspend fun buscarUsuarioPorEmail(email: String): UsuarioSalidaDTO = remoto
    override suspend fun crearUsuario(entrada: UsuarioEntradaDTO): Response<UsuarioSalidaDTO> = crear(entrada)
    override suspend fun login(credenciales: LoginDTO): TokenDTO = loginRespuesta(credenciales)
    override suspend fun actualizarUsuario(id: Long, entrada: UsuarioEntradaDTO): UsuarioSalidaDTO = editar(entrada)
    override suspend fun cambiarRolUsuario(id: Long, cambioRol: CambioRolDTO): UsuarioSalidaDTO = rol(cambioRol)
    override suspend fun eliminarUsuario(id: Long): Response<Unit> = borrar(id)
}