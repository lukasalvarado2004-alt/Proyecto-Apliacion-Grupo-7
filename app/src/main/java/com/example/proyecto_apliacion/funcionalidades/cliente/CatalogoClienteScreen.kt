package com.example.proyecto_apliacion.funcionalidades.cliente

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.proyecto_apliacion.R
import com.example.proyecto_apliacion.data.KaijuDatabase
import com.example.proyecto_apliacion.data.Producto

@Composable
fun CatalogoClienteScreen(onVolverClick: () -> Unit) {
    var seleccionado by remember { mutableStateOf<Producto?>(null) }
    if (seleccionado == null) CatalogoLista(onVolverClick, { seleccionado = it })
    else CatalogoDetalle(seleccionado!!, { seleccionado = null })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogoLista(onVolverClick: () -> Unit, onProductoClick: (Producto) -> Unit) {
    val context = LocalContext.current
    val db = remember { KaijuDatabase.obtener(context) }
    val productos by db.productoDao().observarTodos().collectAsState(initial = emptyList())
    var busqueda by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("Todos") }
    val categorias = listOf("Todos", "Ropa", "Mochilas", "Accesorios")
    val filtrados = productos.filter { p ->
        (p.nombre.contains(busqueda, true) || p.id.contains(busqueda, true)) &&
            (categoria == "Todos" || p.categoria.equals(categoria, true))
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Catálogo Kaiju") }, navigationIcon = {
        IconButton(onClick = onVolverClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(busqueda, { busqueda = it }, label = { Text("Buscar producto o código") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categorias) { cat -> FilterChip(selected = categoria == cat, onClick = { categoria = cat }, label = { Text(cat) }) }
            }
            Text("Productos disponibles: ${filtrados.size}", style = MaterialTheme.typography.bodySmall)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtrados, key = { it.id }) { p ->
                    Card(Modifier.fillMaxWidth().clickable { onProductoClick(p) }) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ImagenCatalogo(p, 76)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(p.nombre, style = MaterialTheme.typography.titleMedium)
                                Text("Código: ${p.id} · ${p.categoria}", style = MaterialTheme.typography.bodySmall)
                                Text("$${p.precio} CLP", color = MaterialTheme.colorScheme.primary)
                                Text(when { p.stock == 0 -> "Agotado"; p.stock <= 15 -> "Pocas unidades (${p.stock})"; else -> "Disponible en tienda" },
                                    color = when { p.stock == 0 -> MaterialTheme.colorScheme.error; p.stock <= 15 -> Color(0xFFD97706); else -> Color(0xFF16A34A) }, style = MaterialTheme.typography.labelMedium)
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
private fun CatalogoDetalle(producto: Producto, onVolverClick: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Detalle del producto") }, navigationIcon = {
        IconButton(onClick = onVolverClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver al catálogo") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ImagenCatalogo(producto, 220)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(producto.nombre, style = MaterialTheme.typography.headlineSmall)
                    Text("$${producto.precio} CLP", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider()
                    Text("Código: ${producto.id}")
                    Text("Categoría: ${producto.categoria}")
                    Text("Stock: ${producto.stock}")
                    Text(if (producto.stock == 0) "Agotado" else if (producto.stock <= 15) "Pocas unidades disponibles" else "Disponible en tienda",
                        color = if (producto.stock == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    HorizontalDivider()
                    Text("Información del artículo", style = MaterialTheme.typography.titleMedium)
                    Text(descripcion(producto))
                }
            }
            Button(onClick = onVolverClick, modifier = Modifier.fillMaxWidth()) { Text("Volver al catálogo") }
        }
    }
}

@Composable
private fun ImagenCatalogo(producto: Producto, size: Int) {
    val imagen = when (producto.id.uppercase()) {
        "K001" -> R.drawable.producto_k001
        "K002" -> R.drawable.producto_k002
        "K003" -> R.drawable.producto_k003
        "K004" -> R.drawable.producto_k004
        "K005" -> R.drawable.producto_k005
        "K006" -> R.drawable.producto_k006
        else -> null
    }
    if (!producto.imagenUri.isNullOrBlank()) {
        AsyncImage(model = producto.imagenUri, contentDescription = producto.nombre, modifier = Modifier.size(size.dp), contentScale = ContentScale.Crop)
    } else if (imagen != null) {
        Image(painterResource(imagen), contentDescription = producto.nombre, modifier = Modifier.size(size.dp), contentScale = ContentScale.Crop)
    } else {
        Surface(Modifier.size(size.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ImageIcon, contentDescription = "Sin imagen") }
        }
    }
}

private fun descripcion(p: Producto): String = when (p.id.uppercase()) {
    "K001" -> "Polera Oversize Kaiju de algodón con estampado de alta resolución y corte relajado para uso urbano."
    "K002" -> "Mochila urbana Kaiju con compartimento acolchado para notebook, materiales resistentes y cierres reforzados."
    "K003" -> "Polerón Kaiju con capucha de doble capa, bolsillo canguro e interior afelpado."
    "K004" -> "Gorra Kaiju con bordado frontal y sistema de ajuste trasero."
    "K005" -> "Billetera urbana Kaiju con compartimentos para tarjetas y protección RFID."
    "K006" -> "Botella térmica Kaiju de acero inoxidable, diseñada para conservar la temperatura de las bebidas."
    else -> "Producto de la categoría ${p.categoria}. Consulta su disponibilidad en tienda usando el código ${p.id}."
}
