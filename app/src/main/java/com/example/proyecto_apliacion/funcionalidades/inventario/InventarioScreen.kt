package com.example.proyecto_apliacion.funcionalidades.inventario

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.example.proyecto_apliacion.R
import com.example.proyecto_apliacion.data.KaijuDatabase
import com.example.proyecto_apliacion.data.Producto
import com.example.proyecto_apliacion.data.ProductoRepository
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(onVolverClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = remember { KaijuDatabase.obtener(context) }
    val vm: InventarioViewModel = viewModel(factory = viewModelFactory {
        initializer { InventarioViewModel(ProductoRepository(db.productoDao(), db.movimientoDao())) }
    })
    val state by vm.uiState.collectAsState()
    var id by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("Ropa") }
    var stock by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var mostrarFormulario by remember { mutableStateOf(false) }
    var imagenSeleccionada by remember { mutableStateOf<Uri?>(null) }
    var bitmapCamara by remember { mutableStateOf<Bitmap?>(null) }
    var errorImagen by remember { mutableStateOf<String?>(null) }

    // Estado para saber cuál producto se está editando
    var productoAEditar by remember { mutableStateOf<Producto?>(null) }

    // Muestra una notificación emergente (Toast) cuando se realiza una acción exitosa
    LaunchedEffect(state.mensaje) {
        state.mensaje?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            if (msg.contains("Creación", ignoreCase = true)) {
                id = ""; nombre = ""; stock = ""; precio = ""
                imagenSeleccionada = null; bitmapCamara = null
                mostrarFormulario = false
            }
            vm.limpiarMensaje()
        }
    }

    fun guardarBitmap(bitmap: Bitmap): Uri? = runCatching {
        val dir = File(context.filesDir, "fotos_productos").apply { mkdirs() }
        val archivo = File(dir, "producto_${System.currentTimeMillis()}.jpg")
        FileOutputStream(archivo).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        Uri.fromFile(archivo)
    }.getOrNull()

    fun copiarImagenGaleria(uri: Uri): Uri? = runCatching {
        val dir = File(context.filesDir, "fotos_productos").apply { mkdirs() }
        val archivo = File(dir, "producto_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "No se pudo abrir la imagen" }
            FileOutputStream(archivo).use { output -> input.copyTo(output) }
        }
        Uri.fromFile(archivo)
    }.getOrNull()

    val launcherCamara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val uri = guardarBitmap(bitmap)
            if (uri != null) { imagenSeleccionada = uri; bitmapCamara = bitmap; errorImagen = null }
            else errorImagen = "No se pudo guardar la foto. Intenta nuevamente."
        }
    }
    val launcherGaleria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val copia = copiarImagenGaleria(uri)
            if (copia != null) { imagenSeleccionada = copia; bitmapCamara = null; errorImagen = null }
            else errorImagen = "No se pudo importar la imagen seleccionada."
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Inventario Kaiju") },
                    navigationIcon = {
                        IconButton(onClick = onVolverClick) {
                            Icon(Icons.Default.ArrowBack, "Volver")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    mostrarFormulario = !mostrarFormulario
                    if (mostrarFormulario.not()) {
                        id = ""; nombre = ""; stock = ""; precio = ""
                        imagenSeleccionada = null; bitmapCamara = null
                        vm.limpiarMensaje()
                    }
                }) {
                    Text(if (mostrarFormulario) "×" else "+")
                }
            }
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = state.busqueda,
                    onValueChange = vm::cambiarBusqueda,
                    label = { Text("Buscar producto o código") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Todos", "Ropa", "Mochilas", "Accesorios").forEach { cat ->
                        FilterChip(
                            selected = state.categoria == cat,
                            onClick = { vm.cambiarCategoria(cat) },
                            label = { Text(cat) }
                        )
                    }
                }
                AnimatedVisibility(visible = mostrarFormulario) {
                    Card(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Nuevo producto", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = id,
                                onValueChange = { id = it; vm.limpiarMensaje() },
                                label = { Text("Código (ej. K007)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = nombre,
                                onValueChange = { nombre = it; vm.limpiarMensaje() },
                                label = { Text("Nombre") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Text("Categoría")
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Ropa", "Mochilas", "Accesorios").forEach { cat ->
                                    FilterChip(
                                        selected = categoria == cat,
                                        onClick = { categoria = cat },
                                        label = { Text(cat) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = stock,
                                    onValueChange = { stock = it.filter(Char::isDigit); vm.limpiarMensaje() },
                                    label = { Text("Stock") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = precio,
                                    onValueChange = { precio = it.filter(Char::isDigit); vm.limpiarMensaje() },
                                    label = { Text("Precio CLP") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            Text("Fotografía del producto (opcional)", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { launcherCamara.launch(null) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CameraAlt, null)
                                    Spacer(Modifier.width(5.dp))
                                    Text("Tomar foto")
                                }
                                OutlinedButton(
                                    onClick = { launcherGaleria.launch("image/*") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Collections, null)
                                    Spacer(Modifier.width(5.dp))
                                    Text("Galería")
                                }
                            }
                            AnimatedVisibility(imagenSeleccionada != null) {
                                AsyncImage(
                                    model = imagenSeleccionada,
                                    contentDescription = "Foto del producto",
                                    modifier = Modifier.fillMaxWidth().height(150.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            if (imagenSeleccionada != null) {
                                TextButton(onClick = { imagenSeleccionada = null; bitmapCamara = null }) {
                                    Text("Quitar fotografía")
                                }
                            }
                            errorImagen?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            state.errorFormulario?.let { Text("⚠ $it", color = MaterialTheme.colorScheme.error) }
                            Button(
                                onClick = {
                                    vm.guardar(id, nombre, categoria, stock, precio, imagenSeleccionada?.toString())
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Guardar producto")
                            }
                        }
                    }
                }
                Text("Productos: ${state.filtrados.size}", style = MaterialTheme.typography.titleMedium)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.filtrados, key = { it.id }) { producto ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (!producto.imagenUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = producto.imagenUri,
                                        contentDescription = "Foto de ${producto.nombre}",
                                        modifier = Modifier.size(82.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    val imagenPredeterminada = when (producto.id.uppercase()) {
                                        "K001" -> R.drawable.producto_k001
                                        "K002" -> R.drawable.producto_k002
                                        "K003" -> R.drawable.producto_k003
                                        "K004" -> R.drawable.producto_k004
                                        "K005" -> R.drawable.producto_k005
                                        "K006" -> R.drawable.producto_k006
                                        else -> null
                                    }
                                    if (imagenPredeterminada != null) {
                                        Image(
                                            painter = painterResource(imagenPredeterminada),
                                            contentDescription = "Imagen de ${producto.nombre}",
                                            modifier = Modifier.size(82.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Surface(
                                            Modifier.size(82.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Icon(
                                                Icons.Default.Image,
                                                contentDescription = "Sin foto",
                                                modifier = Modifier.padding(24.dp)
                                            )
                                        }
                                    }
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(producto.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text("${producto.id} · ${producto.categoria}", style = MaterialTheme.typography.bodySmall)
                                    Text("Stock: ${producto.stock} · $${producto.precio}")

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(
                                            onClick = { productoAEditar = producto },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Editar")
                                        }
                                        TextButton(
                                            onClick = { vm.eliminar(producto.id) },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Diálogo modal de edición
            productoAEditar?.let { prod ->
                DialogEditarProducto(
                    producto = prod,
                    onDismiss = { productoAEditar = null },
                    onConfirm = { id, nom, cat, stk, prc, img ->
                        vm.actualizar(id, nom, cat, stk, prc, img)
                        productoAEditar = null
                    }
                )
            }
        }

        // Overlay de carga (rueda tipo spinner) que bloquea la pantalla mientras procesa
        if (state.cargando) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                        Text("Procesando...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun DialogEditarProducto(
    producto: Producto,
    onDismiss: () -> Unit,
    onConfirm: (id: String, nombre: String, categoria: String, stock: String, precio: String, imagenUri: String?) -> Unit
) {
    var nombre by remember { mutableStateOf(producto.nombre) }
    var categoria by remember { mutableStateOf(producto.categoria) }
    var stock by remember { mutableStateOf(producto.stock.toString()) }
    var precio by remember { mutableStateOf(producto.precio.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar producto (${producto.id})") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Categoría", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Ropa", "Mochilas", "Accesorios").forEach { cat ->
                        FilterChip(
                            selected = categoria == cat,
                            onClick = { categoria = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it.filter(Char::isDigit) },
                    label = { Text("Stock") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it.filter(Char::isDigit) },
                    label = { Text("Precio CLP") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        producto.id,
                        nombre,
                        categoria,
                        stock,
                        precio,
                        producto.imagenUri
                    )
                }
            ) {
                Text("Guardar cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}