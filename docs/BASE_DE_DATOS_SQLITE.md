# Base de datos SQLite — Kaiju Inventory

La aplicación utiliza **SQLite local en el dispositivo**, gestionado mediante **Room** (la capa oficial de persistencia de Android). No requiere servidor ni conexión a Internet para consultar o modificar los datos una vez instalada.

## Archivo de base de datos

- Nombre: `kaiju_inventory.db`
- Ubicación: almacenamiento privado de la aplicación Android.
- Creación/acceso: `data/KaijuDatabase.kt`, mediante `Room.databaseBuilder(...)`.
- Versión actual del esquema: `2`.
- Migración: de versión 1 a 2, añade `imagenUri` a `productos` y crea `movimientos`.

## Tabla `productos`

| Columna | Tipo SQLite | Uso |
|---|---|---|
| `id` | TEXT PRIMARY KEY | Código único del producto |
| `nombre` | TEXT NOT NULL | Nombre |
| `categoria` | TEXT NOT NULL | Categoría |
| `stock` | INTEGER NOT NULL | Unidades disponibles |
| `precio` | INTEGER NOT NULL | Precio en pesos chilenos |
| `imagenUri` | TEXT | URI/ruta local de la fotografía; admite NULL |

## Tabla `movimientos`

| Columna | Tipo SQLite | Uso |
|---|---|---|
| `id` | INTEGER PRIMARY KEY AUTOINCREMENT | Identificador del movimiento |
| `productoId` | TEXT NOT NULL | Código del producto relacionado |
| `nombreProducto` | TEXT NOT NULL | Nombre registrado en el movimiento |
| `tipo` | TEXT NOT NULL | ALTA, ENTRADA, SALIDA, MODIFICACIÓN o ELIMINACIÓN |
| `cantidad` | INTEGER NOT NULL | Unidades afectadas |
| `detalle` | TEXT NOT NULL | Descripción de la operación |
| `fechaMillis` | INTEGER NOT NULL | Fecha/hora en milisegundos Unix |

## Operaciones conectadas

- `ProductoDao`: observa, guarda, busca, actualiza y elimina productos.
- `MovimientoDao`: observa el historial y registra movimientos.
- `ProductoRepository`: conecta las pantallas/ViewModel con los DAO y registra movimientos al crear, modificar stock o eliminar.
- `InventarioViewModel`: valida los campos antes de guardar.
- Los productos iniciales se insertan solamente cuando la tabla está vacía.

## Comprobar los datos en Android Studio

1. Ejecuta la aplicación en un emulador o dispositivo.
2. Abre **View > Tool Windows > App Inspection** (el nombre puede variar según la versión).
3. Selecciona **Database Inspector** y el proceso de la aplicación.
4. Busca `kaiju_inventory.db` y consulta las tablas `productos` y `movimientos`.

La base se guarda en el espacio privado de la aplicación; no se debe crear un archivo `.db` manualmente en la carpeta del proyecto. Room la crea al ejecutar la app.
