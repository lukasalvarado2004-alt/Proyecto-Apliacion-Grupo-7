package com.example.proyecto_apliacion.funcionalidades.movimientos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.proyecto_apliacion.data.KaijuDatabase
import com.example.proyecto_apliacion.data.Movimiento
import com.example.proyecto_apliacion.data.ProductoRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

class MovimientosViewModel(repository: ProductoRepository) : ViewModel() {
    val movimientos = repository.observarMovimientos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientosScreen() {
    val context = LocalContext.current
    val vm: MovimientosViewModel = viewModel(factory = viewModelFactory {
        initializer { MovimientosViewModel(ProductoRepository(KaijuDatabase.obtener(context).productoDao(), KaijuDatabase.obtener(context).movimientoDao())) }
    })
    val movimientos by vm.movimientos.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Movimientos de inventario") }) }) { padding ->
        if (movimientos.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.History, contentDescription = null)
                Text("Aún no hay movimientos", style = MaterialTheme.typography.titleLarge)
                Text("Las altas, cambios de stock y eliminaciones aparecerán aquí automáticamente.")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(movimientos, key = { it.id }) { movimiento -> MovimientoCard(movimiento) }
            }
        }
    }
}

@Composable
private fun MovimientoCard(m: Movimiento) {
    val fecha = remember(m.fechaMillis) { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(m.fechaMillis)) }
    val color = when (m.tipo) { "ALTA", "ENTRADA" -> MaterialTheme.colorScheme.primary; "SALIDA", "ELIMINACIÓN" -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.secondary }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(m.nombreProducto, style = MaterialTheme.typography.titleMedium)
                Text(m.tipo, color = color, style = MaterialTheme.typography.labelLarge)
            }
            Text("Código: ${m.productoId} · Cantidad: ${m.cantidad}")
            Text(m.detalle, style = MaterialTheme.typography.bodyMedium)
            Text(fecha, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
