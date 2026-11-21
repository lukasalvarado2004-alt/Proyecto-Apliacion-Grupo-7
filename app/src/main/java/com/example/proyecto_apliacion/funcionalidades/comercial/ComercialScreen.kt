package com.example.proyecto_apliacion.funcionalidades.comercial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto_apliacion.data.DefaultAppContainer
import com.example.proyecto_apliacion.data.local.Usuario
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun clp(n: Long) = NumberFormat.getCurrencyInstance(Locale("es", "CL")).format(n)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentasScreen(usuarioActual: String) {
    val ctx = LocalContext.current
    val container = remember { DefaultAppContainer(ctx) }

    val viewModel: VentasViewModel = viewModel(
        factory = VentasViewModelFactory(
            container.comercialRepository,
            container.productoRepository,
            ctx
        )
    )

    val productos by viewModel.productos.collectAsState(initial = emptyList())
    val ventas by viewModel.ventas.collectAsState(initial = emptyList())
    var seleccionado by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("1") }

    val ventasHoy by viewModel.ventasHoy.collectAsState(initial = 0L)
    val aviso by viewModel.aviso.collectAsState()

    val hoy = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Ventas y resumen comercial") }) }) { pad ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Ventas de hoy", style = MaterialTheme.typography.titleMedium)
                        Text(
                            viewModel.formatClp(ventasHoy),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Transacciones registradas: ${ventas.count { it.fechaMillis >= hoy }}")
                    }
                }
            }
            item { Text("Registrar venta", style = MaterialTheme.typography.titleLarge) }
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = productos.firstOrNull { it.id == seleccionado }
                            ?.let { "${it.nombre} · stock ${it.stock} · ${viewModel.formatClp(it.precio.toLong())}" }
                            ?: "Selecciona un producto",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Producto disponible") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        productos.filter { it.stock > 0 }.forEach { prod ->
                            DropdownMenuItem(
                                text = { Text("${prod.nombre} · stock ${prod.stock}") },
                                onClick = { seleccionado = prod.id; expanded = false }
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { cantidad = it.filter(Char::isDigit) },
                    label = { Text("Cantidad") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                val prod = productos.firstOrNull { it.id == seleccionado }
                Text("Total: ${prod?.let { viewModel.formatClp(it.precio.toLong() * (cantidad.toIntOrNull() ?: 0)) } ?: viewModel.formatClp(0)}")
            }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val prod = productos.firstOrNull { it.id == seleccionado }
                        viewModel.registrarVenta(usuarioActual, prod, cantidad) {
                            seleccionado = ""
                            cantidad = "1"
                        }
                    }
                ) { Text("Confirmar venta y descontar stock") }
            }
            if (aviso.isNotBlank()) item { Text(aviso, color = MaterialTheme.colorScheme.primary) }
            item {
                HorizontalDivider()
                Text("Historial de ventas", style = MaterialTheme.typography.titleLarge)
            }
            items(ventas.take(100), key = { it.id }) { v ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Venta #${v.id} · ${viewModel.formatClp(v.total)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(v.fechaMillis))} · ${v.usuario}")
                        Text(v.estado)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdministracionScreen() {
    val ctx = LocalContext.current
    val container = remember { DefaultAppContainer(ctx) }

    val viewModel: AdministracionViewModel = viewModel(
        factory = AdministracionViewModelFactory(
            container.usuarioRepository,
            container.comercialRepository
        )
    )

    val usuarios by viewModel.usuarios.collectAsState(initial = emptyList())
    val categorias by viewModel.categorias.collectAsState(initial = emptyList())
    val aviso by viewModel.aviso.collectAsState()
    val usuarioEliminando by viewModel.usuarioEliminando.collectAsState()
    val guardandoEdicion by viewModel.guardandoEdicion.collectAsState()

    var tab by remember { mutableIntStateOf(0) }
    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var nombreUsuario by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var rol by remember { mutableStateOf("VENTAS") }

    val snackbarHostState = remember { SnackbarHostState() }
    var usuarioAConfirmarEliminacion by remember { mutableStateOf<Usuario?>(null) }
    var usuarioEnEdicion by remember { mutableStateOf<Usuario?>(null) }

    LaunchedEffect(aviso) {
        if (aviso.isNotBlank()) {
            snackbarHostState.showSnackbar(aviso)
            viewModel.limpiarAviso()
        }
    }

    usuarioAConfirmarEliminacion?.let { usuarioTarget ->
        AlertDialog(
            onDismissRequest = { usuarioAConfirmarEliminacion = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Eliminar usuario") },
            text = {
                val detalleCorreo = if (usuarioTarget.correo.isNotBlank()) " (${usuarioTarget.correo})" else ""
                Text("¿Seguro que deseas eliminar a ${usuarioTarget.nombre}$detalleCorreo? Esta acción también lo eliminará del servidor.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarUsuario(usuarioTarget.username)
                        usuarioAConfirmarEliminacion = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { usuarioAConfirmarEliminacion = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    usuarioEnEdicion?.let { target ->
        var editNombre by remember(target) { mutableStateOf(target.nombre) }
        var editCorreo by remember(target) { mutableStateOf(target.correo) }
        var editTelefono by remember(target) { mutableStateOf(target.telefono) }
        var editDireccion by remember(target) { mutableStateOf(target.direccion) }
        var editRol by remember(target) { mutableStateOf(target.rol) }
        var rolExpandido by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!guardandoEdicion) usuarioEnEdicion = null },
            title = { Text("Editar usuario") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editNombre,
                        onValueChange = { editNombre = it },
                        label = { Text("Nombre") },
                        enabled = !guardandoEdicion,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCorreo,
                        onValueChange = { editCorreo = it },
                        label = { Text("Correo") },
                        enabled = !guardandoEdicion,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTelefono,
                        onValueChange = { editTelefono = it },
                        label = { Text("Teléfono") },
                        enabled = !guardandoEdicion,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDireccion,
                        onValueChange = { editDireccion = it },
                        label = { Text("Dirección") },
                        enabled = !guardandoEdicion,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenuBox(
                        expanded = rolExpandido,
                        onExpandedChange = { if (!guardandoEdicion) rolExpandido = it }
                    ) {
                        OutlinedTextField(
                            value = editRol,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Rol") },
                            enabled = !guardandoEdicion,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = rolExpandido,
                            onDismissRequest = { rolExpandido = false }
                        ) {
                            listOf("ADMIN", "VENTAS", "INVENTARIO", "CLIENTE").forEach { opcion ->
                                DropdownMenuItem(
                                    text = { Text(opcion) },
                                    onClick = {
                                        editRol = opcion
                                        rolExpandido = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !guardandoEdicion,
                    onClick = {
                        viewModel.editarUsuario(
                            usuarioOriginal = target,
                            nuevoNombre = editNombre,
                            nuevoCorreo = editCorreo,
                            nuevoTelefono = editTelefono,
                            nuevaDireccion = editDireccion,
                            nuevoRol = editRol
                        ) {
                            usuarioEnEdicion = null
                        }
                    }
                ) {
                    if (guardandoEdicion) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Guardar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !guardandoEdicion,
                    onClick = { usuarioEnEdicion = null }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Administración") }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp)
        ) {
            TabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { viewModel.limpiarAviso(); tab = 0 }, text = { Text("Usuarios") })
                Tab(tab == 1, { viewModel.limpiarAviso(); tab = 1 }, text = { Text("Categorías") })
            }
            Spacer(Modifier.height(12.dp))
            if (tab == 0) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Usuario / login") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = nombreUsuario,
                                onValueChange = { nombreUsuario = it },
                                label = { Text("Nombre visible") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = correo,
                                onValueChange = { correo = it },
                                label = { Text("Correo") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            var ex by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(ex, { ex = it }) {
                                OutlinedTextField(
                                    value = rol,
                                    onValueChange = {},
                                    label = { Text("Rol") },
                                    readOnly = true,
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(ex, { ex = false }) {
                                    listOf("ADMIN", "VENTAS", "INVENTARIO").forEach {
                                        DropdownMenuItem(
                                            text = { Text(it) },
                                            onClick = { rol = it; ex = false }
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = {
                                    viewModel.guardarUsuario(username, nombreUsuario, correo, rol) {
                                        username = ""
                                        nombreUsuario = ""
                                        correo = ""
                                    }
                                },
                                enabled = usuarioEliminando == null,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Guardar usuario") }
                            Text(
                                "Los usuarios demo usan las claves: admin/1234, ventas/5678, inventario/0000. Los nuevos registros se administran aquí; para iniciar sesión con credenciales nuevas se debe asignar contraseña en la próxima versión.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    items(usuarios, key = { it.username }) { u ->
                        val estaEliminando = usuarioEliminando == u.username
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(u.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text("${u.username} · ${u.rol}")
                                    if (u.telefono.isNotBlank()) {
                                        Text("Tel: ${u.telefono}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(if (u.activo) "Activo" else "Deshabilitado")
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    TextButton(
                                        onClick = { usuarioEnEdicion = u },
                                        enabled = usuarioEliminando == null
                                    ) {
                                        Text("Editar")
                                    }
                                    TextButton(
                                        onClick = { viewModel.cambiarEstadoUsuario(u.username, !u.activo) },
                                        enabled = usuarioEliminando == null
                                    ) {
                                        Text(if (u.activo) "Desactivar" else "Activar")
                                    }
                                    if (u.username != "admin") {
                                        if (estaEliminando) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                                Text(
                                                    "Eliminando...",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        } else {
                                            TextButton(
                                                onClick = { usuarioAConfirmarEliminacion = u },
                                                enabled = usuarioEliminando == null,
                                                colors = ButtonDefaults.textButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.error
                                                )
                                            ) {
                                                Text("Eliminar")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = nombre,
                                onValueChange = { nombre = it },
                                label = { Text("Nombre de categoría") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = descripcion,
                                onValueChange = { descripcion = it },
                                label = { Text("Descripción") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    viewModel.guardarCategoria(nombre, descripcion) {
                                        nombre = ""
                                        descripcion = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Guardar categoría") }
                        }
                    }
                    items(categorias, key = { it.nombre }) { c ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(c.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text(c.descripcion)
                                    Text(if (c.activa) "Activa" else "Inactiva")
                                }
                                Column {
                                    TextButton(onClick = { viewModel.cambiarEstadoCategoria(c.nombre, !c.activa) }) {
                                        Text(if (c.activa) "Desactivar" else "Activar")
                                    }
                                    TextButton(onClick = { viewModel.eliminarCategoria(c.nombre) }) {
                                        Text("Eliminar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoPublicoScreen() {
    val ctx = LocalContext.current
    val container = remember { DefaultAppContainer(ctx) }
    val productos by container.productoRepository.observarProductos().collectAsState(initial = emptyList())
    var busqueda by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Catálogo y disponibilidad") }) }) { pad ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    label = { Text("Buscar producto") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(
                productos.filter {
                    it.nombre.contains(busqueda, true) || it.categoria.contains(busqueda, true)
                },
                key = { it.id }
            ) { p ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(p.nombre, style = MaterialTheme.typography.titleMedium)
                        Text("Categoría: ${p.categoria}")
                        Text("Código: ${p.id}")
                        Text("Precio: ${clp(p.precio.toLong())}")
                        Text(
                            if (p.stock > 0) "Disponible" else "Agotado",
                            color = if (p.stock > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}