package com.example.proyecto_apliacion.funcionalidades.autenticacion

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginCorrecto: (String) -> Unit,
    onLoginCliente: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    var mostrandoRegistro by rememberSaveable { mutableStateOf(false) }
    var procesandoLogin by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.mensajeError) {
        if (viewModel.mensajeError != null) {
            procesandoLogin = false
        }
    }

    LaunchedEffect(viewModel.loginExitoso) {
        if (viewModel.loginExitoso) {
            procesandoLogin = false
            val usuarioValidado = viewModel.usuario
            val esCliente = viewModel.esIngresoCliente
            viewModel.consumirLoginExitoso()
            if (esCliente) onLoginCliente(usuarioValidado.trim().lowercase()) else onLoginCorrecto(usuarioValidado.trim().lowercase())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Crossfade(targetState = mostrandoRegistro, label = "TituloTopBar") { esReg ->
                        Text(if (esReg) "Kaiju - Registro de Cliente" else "Kaiju Inventory - Acceso")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = mostrandoRegistro,
                transitionSpec = {
                    val direccion = if (targetState) 1 else -1
                    (fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 3 * direccion })
                        .togetherWith(fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { -it / 3 * direccion })
                },
                label = "TransicionLoginRegistro"
            ) { esRegistro ->
                if (!esRegistro) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Iniciar Sesión",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 28.dp)
                        )
                        OutlinedTextField(
                            value = viewModel.usuario,
                            onValueChange = { viewModel.onUsuarioCambiado(it); procesandoLogin = false },
                            label = { Text("Usuario o correo") },
                            isError = viewModel.mensajeError != null && viewModel.usuario.isBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = viewModel.contrasena,
                            onValueChange = { viewModel.onContrasenaCambiada(it); procesandoLogin = false },
                            label = { Text("Contraseña") },
                            isError = viewModel.mensajeError != null && viewModel.contrasena.isBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        AnimatedVisibility(
                            visible = viewModel.mensajeError != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            viewModel.mensajeError?.let { error ->
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                procesandoLogin = true
                                viewModel.iniciarSesion(context)
                            },
                            enabled = !procesandoLogin,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (procesandoLogin) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Verificando…")
                            } else {
                                Text("Ingresar")
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { onLoginCliente("Invitado") }, modifier = Modifier.fillMaxWidth()) {
                            Text("Ver catálogo como invitado")
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { mostrandoRegistro = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("¿No tienes cuenta? Regístrate aquí")
                        }
                        Spacer(Modifier.height(16.dp))

                    }
                } else {
                    FormularioRegistroVista(
                        viewModel = viewModel,
                        onRegistroExitoso = { correo ->
                            viewModel.onUsuarioCambiado(correo)
                            mostrandoRegistro = false
                            Toast.makeText(context, "¡Cuenta creada con éxito! Ya puedes iniciar sesión.", Toast.LENGTH_LONG).show()
                        },
                        onVolverAlLogin = { mostrandoRegistro = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun FormularioRegistroVista(
    viewModel: LoginViewModel,
    onRegistroExitoso: (correo: String) -> Unit,
    onVolverAlLogin: () -> Unit
) {
    val context = LocalContext.current
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var confirmarClave by rememberSaveable { mutableStateOf("") }
    var mensajeError by rememberSaveable { mutableStateOf<String?>(null) }
    var registrando by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Crear cuenta de cliente",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        OutlinedTextField(
            nombre, { nombre = it; mensajeError = null }, label = { Text("Nombre completo") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            correo, { correo = it; mensajeError = null }, label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            clave, { clave = it; mensajeError = null }, label = { Text("Contraseña (mínimo 8 caracteres)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            confirmarClave, { confirmarClave = it; mensajeError = null }, label = { Text("Confirmar contraseña") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        AnimatedVisibility(
            visible = mensajeError != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            mensajeError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            enabled = !registrando,
            onClick = {
                when {
                    nombre.isBlank() || correo.isBlank() || clave.isBlank() || confirmarClave.isBlank() -> mensajeError = "Por favor completa todos los campos."
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches() -> mensajeError = "Ingresa un correo electrónico válido."
                    clave.length < 8 -> mensajeError = "La contraseña debe tener al menos 8 caracteres."
                    clave != confirmarClave -> mensajeError = "Las contraseñas no coinciden."
                    else -> {
                        registrando = true
                        viewModel.registrarUsuario(
                            context, nombre, correo, clave,
                            onExito = {
                                registrando = false
                                onRegistroExitoso(correo.trim().lowercase())
                            },
                            onError = { error -> registrando = false; mensajeError = error }
                        )
                    }
                }
            }, modifier = Modifier.fillMaxWidth()
        ) {
            if (registrando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text("Registrando…")
            } else {
                Text("Registrarme")
            }
        }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onVolverAlLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Ya tengo cuenta · Volver al inicio de sesión")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    LoginScreen(onLoginCorrecto = {}, onLoginCliente = {})
}