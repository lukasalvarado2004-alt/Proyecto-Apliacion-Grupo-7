package com.example.proyecto_apliacion.funcionalidades.comercial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.proyecto_apliacion.data.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun clp(n: Long) = NumberFormat.getCurrencyInstance(Locale("es", "CL")).format(n)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentasScreen(usuarioActual: String) {
    val ctx = LocalContext.current
    val db = remember { KaijuDatabase.obtener(ctx) }
    val repoVentas = remember { ComercialRepository(db.ventaDao(), db.usuarioDao(), db.categoriaDao()) }
    val repoProductos = remember { ProductoRepository(db.productoDao(), db.movimientoDao()) }
    
    val viewModel: VentasViewModel = viewModel(
        factory = VentasViewModelFactory(repoVentas, repoProductos, db)
    )

    val productos by viewModel.productos.collectAsState(initial = emptyList())
    val ventas by viewModel.ventas.collectAsState(initial = emptyList())
    var seleccionado by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("1") }
    
    val ventasHoy by viewModel.ventasHoy.collectAsState(initial = 0L)
    val aviso by viewModel.aviso.collectAsState()

    val hoy = remember { java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY,0);set(java.util.Calendar.MINUTE,0);set(java.util.Calendar.SECOND,0);set(java.util.Calendar.MILLISECOND,0) }.timeInMillis }

    Scaffold(topBar = { TopAppBar(title = { Text("Ventas y resumen comercial") }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Ventas de hoy", style = MaterialTheme.typography.titleMedium)
                        Text(viewModel.formatClp(ventasHoy), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Transacciones registradas: ${ventas.count { it.fechaMillis >= hoy }}")
                    }
                }
            }
            item { Text("Registrar venta", style = MaterialTheme.typography.titleLarge) }
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = productos.firstOrNull { it.id == seleccionado }?.let { "${it.nombre} · stock ${it.stock} · ${viewModel.formatClp(it.precio.toLong())}" } ?: "Selecciona un producto",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Producto disponible") },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        productos.filter { it.stock > 0 }.forEach { prod ->
                            DropdownMenuItem(text = { Text("${prod.nombre} · stock ${prod.stock}") }, onClick = { seleccionado = prod.id; expanded = false })
                        }
                    }
                }
            }
            item { OutlinedTextField(value = cantidad, onValueChange = { cantidad = it.filter(Char::isDigit) }, label = { Text("Cantidad") }, modifier = Modifier.fillMaxWidth()) }
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
            item { HorizontalDivider(); Text("Historial de ventas", style = MaterialTheme.typography.titleLarge) }
            items(ventas.take(100), key = { it.id }) { v ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Venta #${v.id} · ${viewModel.formatClp(v.total)}", style = MaterialTheme.typography.titleMedium)
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
    val db = remember { KaijuDatabase.obtener(ctx) }
    val repo = remember { ComercialRepository(db.ventaDao(), db.usuarioDao(), db.categoriaDao()) }
    
    val viewModel: AdministracionViewModel = viewModel(factory = AdministracionViewModelFactory(repo))
    
    val usuarios by viewModel.usuarios.collectAsState(initial = emptyList())
    val categorias by viewModel.categorias.collectAsState(initial = emptyList())
    var tab by remember { mutableIntStateOf(0) }
    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var nombreUsuario by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var rol by remember { mutableStateOf("VENTAS") }
    val aviso by viewModel.aviso.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Administración") }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            TabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { viewModel.limpiarAviso(); tab = 0 }, text = { Text("Usuarios") })
                Tab(tab == 1, { viewModel.limpiarAviso(); tab = 1 }, text = { Text("Categorías") })
            }
            if (tab == 0) {
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedTextField(username, { username = it }, label = { Text("Usuario / login") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(nombreUsuario, { nombreUsuario = it }, label = { Text("Nombre visible") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(correo, { correo = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth())
                        var ex by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(ex, { ex = it }) {
                            OutlinedTextField(value = rol, onValueChange = {}, label = { Text("Rol") }, readOnly = true, modifier = Modifier.menuAnchor().fillMaxWidth())
                            ExposedDropdownMenu(ex, { ex = false }) {
                                listOf("ADMIN", "VENTAS", "INVENTARIO").forEach {
                                    DropdownMenuItem(text = { Text(it) }, onClick = { rol = it; ex = false })
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
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Guardar usuario") }
                        Text(
                            "Los usuarios demo usan las claves: admin/1234, ventas/5678, inventario/0000. Los nuevos registros se administran aquí; para iniciar sesión con credenciales nuevas se debe asignar contraseña en la próxima versión.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    items(usuarios, key = { it.username }) { u ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(u.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text("${u.username} · ${u.rol}")
                                    Text(if (u.activo) "Activo" else "Deshabilitado")
                                }
                                Column {
                                    TextButton(onClick = { viewModel.cambiarEstadoUsuario(u.username, !u.activo) }) { Text(if (u.activo) "Desactivar" else "Activar") }
                                    if (u.username != "admin") {
                                        TextButton(onClick = { viewModel.eliminarUsuario(u.username) }) { Text("Eliminar") }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre de categoría") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(descripcion, { descripcion = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
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
                    items(categorias, key = { it.nombre }) { c ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(c.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text(c.descripcion)
                                    Text(if (c.activa) "Activa" else "Inactiva")
                                }
                                Column {
                                    TextButton(onClick = { viewModel.cambiarEstadoCategoria(c.nombre, !c.activa) }) { Text(if (c.activa) "Desactivar" else "Activar") }
                                    TextButton(onClick = { viewModel.eliminarCategoria(c.nombre) }) { Text("Eliminar") }
                                }
                            }
                        }
                    }
                }
            }
            if (aviso.isNotBlank()) Text(aviso, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoPublicoScreen() {
 val ctx=LocalContext.current; val db= remember { KaijuDatabase.obtener(ctx) }; val productos by db.productoDao().observarTodos().collectAsState(initial=emptyList()); var busqueda by remember { mutableStateOf("") }
 Scaffold(topBar={TopAppBar(title={Text("Catálogo y disponibilidad")})}) { pad -> LazyColumn(Modifier.fillMaxSize().padding(pad).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
 item { OutlinedTextField(busqueda,{busqueda=it},label={Text("Buscar producto")},modifier=Modifier.fillMaxWidth()) }
 items(productos.filter{it.nombre.contains(busqueda,true)||it.categoria.contains(busqueda,true)},key={it.id}) { p -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text(p.nombre,style=MaterialTheme.typography.titleMedium); Text("Categoría: ${p.categoria}"); Text("Código: ${p.id}"); Text("Precio: ${clp(p.precio.toLong())}"); Text(if(p.stock>0) "Disponible" else "Agotado",color=if(p.stock>0)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) } } }
 } }
}
