# Kaiju Inventory — Equipo 7

Aplicación Android en Kotlin y Jetpack Compose para consultar y mantener un inventario local.

## Funcionalidades
- Inicio de sesión de demostración con validación de campos y mensajes visuales.
- Panel principal con total de unidades calculado desde los datos persistidos.
- Inventario con búsqueda por código/nombre, filtro por categoría y alta/eliminación de productos.
- Validaciones centralizadas en `InventarioViewModel` (código, nombre, categoría, stock y precio).
- Persistencia local con Room (`Producto`, `ProductoDao`, `KaijuDatabase`, `ProductoRepository`).
- Acceso a dos recursos nativos: cámara mediante la aplicación de cámara del dispositivo y selección de imagen mediante el selector de galería. No se solicita permiso de cámara porque la captura se delega a la app de cámara del sistema.
- Animación de aparición/desaparición del área de previsualización y del formulario.

## Ejecutar
1. Abrir la carpeta raíz en Android Studio.
2. Usar JDK 17 y sincronizar Gradle con conexión a Internet para descargar dependencias.
3. Ejecutar la configuración `app` en un emulador o dispositivo Android.

## Credenciales de demostración
- `admin` / `1234`
- `ventas` / `5678`
- `inventario` / `0000`

Estas credenciales son exclusivamente para una demostración académica; no representan autenticación segura para producción.


## Base de datos SQLite

La aplicación utiliza SQLite local a través de Android Room. Los productos y el historial de movimientos persisten entre ejecuciones. La base se llama `kaiju_inventory.db`, contiene las tablas `productos` y `movimientos`, y dispone de migración del esquema de versión 1 a 2. Consulta `docs/BASE_DE_DATOS_SQLITE.md` para el esquema y los pasos para inspeccionarla desde Android Studio.

## Arquitectura
- `data/`: entidad Room, DAO, base de datos y repositorio.
- `funcionalidades/autenticacion/`: pantalla y lógica de acceso.
- `funcionalidades/inventario/`: pantalla, estado y validaciones del inventario.
- `MainActivity.kt`: navegación principal y lanzadores de recursos nativos.

## Entrega y colaboración
La rúbrica también exige evidencia externa real de commits distribuidos y planificación colaborativa en GitHub/Trello. Esta copia ZIP no puede crear ni demostrar actividad histórica de un repositorio ni tareas compartidas; el equipo debe subir el proyecto a su repositorio y completar esos artefactos.

## Funcionalidades ampliadas (EP3)

- **Movimientos:** historial local en Room de altas de productos, cambios de stock y eliminaciones, ordenado del más reciente al más antiguo.
- **Perfil:** pantalla editable con nombre y correo; los datos se guardan localmente en preferencias del dispositivo.
- **Fotos de productos:** desde el formulario de alta se puede tomar una foto con la cámara o seleccionar una imagen de la galería. La imagen se copia al almacenamiento privado de la aplicación y su URI se guarda en Room junto al producto, por lo que aparece en el listado después de cerrar y volver a abrir la app.
- **Migración Room:** la base de datos migra desde la versión 1 a la versión 2, agregando la ruta de imagen y la tabla de movimientos sin borrar el inventario existente.

### Prueba rápida
1. Inicia sesión y abre **Inventario**.
2. Pulsa `+`, completa código, nombre, categoría, stock y precio.
3. Usa **Tomar foto** o **Galería**, verifica la vista previa y guarda el producto.
4. Vuelve al listado y comprueba que se muestre la fotografía.
5. Abre **Movimientos** para ver el alta. Cambios de stock y eliminaciones también quedan registrados.
6. En **Perfil**, edita el nombre y correo; los cambios quedan guardados localmente.

Las funciones de cámara dependen de que el emulador o dispositivo tenga una aplicación de cámara disponible. La selección de galería usa el selector del sistema Android y no requiere solicitar acceso amplio al almacenamiento.

## Módulos comerciales añadidos

- **Ventas:** registro de ventas por producto y cantidad, validación de stock disponible, detalle de venta, total en CLP, descuento de existencias y movimiento tipo `VENTA`. La escritura de venta, detalle, stock y movimiento se realiza dentro de una transacción Room.
- **Usuarios y roles:** pantalla de administración para registrar, activar/desactivar y eliminar usuarios, con roles `ADMIN`, `VENTAS` e `INVENTARIO`.
- **Categorías:** administración de categorías (crear/actualizar por nombre, activar/desactivar y eliminar).
- **Catálogo público:** consulta de productos, categoría, precio y disponibilidad.
- **Indicadores:** stock total, cantidad de productos con stock bajo (umbral de 10 unidades), ventas del día e historial de ventas.
- **Control de acceso por rol en navegación:** administrador accede a todo; ventas a catálogo y ventas; inventario a catálogo, inventario y movimientos.

### Accesos demo

- `admin / 1234` — administrador
- `ventas / 5678` — personal de ventas
- `inventario / 0000` — encargado de inventario

Nota: el alta de usuarios en esta versión administra el registro y el rol en SQLite; la autenticación de cuentas creadas desde esa pantalla y el almacenamiento seguro de contraseñas quedan como mejora pendiente. Las cuentas demo siguen usando credenciales locales de demostración.
