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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.proyecto_apliacion.R
import com.example.proyecto_apliacion.data.KaijuDatabase
import com.example.proyecto_apliacion.data.Producto
import com.example.proyecto_apliacion.data.ProductoRepository

@Composable
fun CatalogoClienteScreen(
    onVolverClick: () -> Unit
) {
    var productoSeleccionado by remember { mutableStateOf<Producto?>(null) }

    if (productoSeleccionado != null) {
        PantallaDetalleProductoCliente(
            producto = productoSeleccionado!!,
            onVolverClick = { productoSeleccionado = null }
        )
    } else {
        PantallaListaCatalogoCliente(
            onVolverClick = onVolverClick,
            onProductoClick = { producto -> productoSeleccionado = producto }
        )
    }
}

// VISTA 1: Lista del Catálogo
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PantallaListaCatalogoCliente(
    onVolverClick: () -> Unit,
    onProductoClick: (Producto) -> Unit
) {
    val context = LocalContext.current

    val db = remember { KaijuDatabase.obtener(context) }
    val repository = remember { ProductoRepository(db.productoDao(), db.movimientoDao()) }
    val productos by repository.observarProductos().collectAsState(initial = emptyList())

    LaunchedEffect(repository) {
        repository.inicializarSiVacio()
    }

    var busqueda by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("Todos") }

    val productosFiltrados = productos.filter { producto ->
        val coincideNombre = producto.nombre.contains(busqueda, ignoreCase = true) ||
                producto.id.contains(busqueda, ignoreCase = true)
        val coincideCategoria = categoriaSeleccionada == "Todos" || producto.categoria == categoriaSeleccionada
        coincideNombre && coincideCategoria
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catálogo Kaiju") },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                label = { Text("Buscar producto o código") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(listOf("Todos", "Ropa", "Mochilas", "Accesorios")) { cat ->
                    FilterChip(
                        selected = categoriaSeleccionada == cat,
                        onClick = { categoriaSeleccionada = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Text(
                text = "Productos disponibles: ${productosFiltrados.size}",
                style = MaterialTheme.typography.bodySmall
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(productosFiltrados, key = { it.id }) { producto ->
                    TarjetaProductoCliente(
                        producto = producto,
                        onClick = { onProductoClick(producto) }
                    )
                }
            }
        }
    }
}

// Tarjeta individual del catálogo
@Composable
private fun TarjetaProductoCliente(
    producto: Producto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImagenProducto(producto = producto, size = 70.dp)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = producto.nombre, style = MaterialTheme.typography.titleMedium)
                Text(text = "Código: ${producto.id} · ${producto.categoria}", style = MaterialTheme.typography.bodySmall)
                Text(text = "$${producto.precio} CLP", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

                if (producto.stock == 0) {
                    Text(text = "Agotado", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                } else if (producto.stock <= 15) {
                    Text(text = "Pocas unidades (${producto.stock})", color = Color(0xFFD97706), style = MaterialTheme.typography.labelMedium)
                } else {
                    Text(text = "Disponible en tienda", color = Color(0xFF16A34A), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// VISTA 2: Pantalla de Detalle del Producto
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PantallaDetalleProductoCliente(
    producto: Producto,
    onVolverClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Producto") },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver al Catálogo")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ImagenProducto(producto = producto, size = 180.dp)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = producto.nombre,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = "$${producto.precio} CLP",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    HorizontalDivider()

                    Text(text = "Código de Producto: ${producto.id}", style = MaterialTheme.typography.bodyLarge)
                    Text(text = "Categoría: ${producto.categoria}", style = MaterialTheme.typography.bodyLarge)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Estado en Tienda: ", style = MaterialTheme.typography.bodyLarge)
                        if (producto.stock == 0) {
                            Text(text = "Agotado", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                        } else if (producto.stock <= 15) {
                            Text(text = "Pocas Unidades (${producto.stock} en stock)", color = Color(0xFFD97706), style = MaterialTheme.typography.titleMedium)
                        } else {
                            Text(text = "Disponible", color = Color(0xFF16A34A), style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "Información del Artículo",
                        style = MaterialTheme.typography.titleMedium
                    )


                    Text(
                        text = obtenerDescripcionProducto(producto),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }

            Button(
                onClick = onVolverClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver al Catálogo")
            }
        }
    }
}

private fun obtenerDescripcionProducto(producto: Producto): String {
    return when (producto.id.uppercase()) {
        "K001" -> "Polera Oversize Kaiju confeccionada en 100% algodón hilado en anillo de 220g. Cuenta con estampado de alta resolución en la espalda resistente a múltiples lavados y un corte relajado ideal para uso urbano."
        "K002" -> "Mochila táctica/urbana Kaiju con compartimento acolchado para notebook de hasta 15.6 pulgadas. Fabricada en poliéster impermeable de alta resistencia con cierres reforzados y soporte lumbar ergonómico."
        "K003" -> "Polerón Kaiju Hoodie con capucha de doble capa y bolsillo canguro. Interior con tela afelpada para máxima retención térmica durante días fríos."
        "K004" -> "Gorra/Jockey Snapback Kaiju con visera curva y bordado frontal en alto relieve 3D. Sistema de ajuste trasero compatible con múltiples tallas."
        "K005" -> "Billetera urbana Kaiju con tecnología de protección anti-clonación RFID. Incluye 8 compartimentos para tarjetas, portabilletes y monedero con cierre."
        "K006" -> "Botella térmica Kaiju de acero inoxidable 18/8 con capacidad de 750 ml. Libre de BPA, mantiene bebidas frías hasta por 24 horas o calientes por 12 horas."
        else -> when (producto.categoria) {
            "Ropa" -> "Prenda de vestir de la línea oficial Kaiju. Diseñada con textiles seleccionados de excelente calidad para ofrecer durabilidad, suavidad y comodidad en todo momento."
            "Mochilas" -> "Mochila/Bolso Kaiju diseñado para el transporte seguro de tus pertenencias con costuras reforzadas y materiales resistentes al desgaste diario."
            "Accesorios" -> "Accesorio urbano Kaiju elaborado con materiales resistentes de primera línea. El complemento ideal para tu vestimenta diaria."
            else -> "Producto exclusivo de la marca Kaiju. Diseñado para ofrecer durabilidad y estilo. Consulta disponibilidad de compra presencial con el código ${producto.id}."
        }
    }
}

@Composable
private fun ImagenProducto(producto: Producto, size: Dp) {
    if (!producto.imagenUri.isNullOrBlank()) {
        AsyncImage(
            model = producto.imagenUri,
            contentDescription = producto.nombre,
            modifier = Modifier.size(size),
            contentScale = ContentScale.Crop
        )
    } else {
        val imagenLocal = when (producto.id.uppercase()) {
            "K001" -> R.drawable.producto_k001
            "K002" -> R.drawable.producto_k002
            "K003" -> R.drawable.producto_k003
            "K004" -> R.drawable.producto_k004
            "K005" -> R.drawable.producto_k005
            "K006" -> R.drawable.producto_k006
            else -> null
        }

        if (imagenLocal != null) {
            Image(
                painter = painterResource(imagenLocal),
                contentDescription = producto.nombre,
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(size),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "Sin imagen"
                    )
                }
            }
        }
    }
}