# Bazar · Inventario y Caja (JSP + Servlets + JDBC + MySQL)

Migración de la app web de una sola página a Java: **JSP** para las vistas, **Servlets** (GET/POST) para la lógica,
**JDBC** para MySQL. Sin frameworks, sin JSTL, un único jar externo (el driver de MySQL).

## Requisitos (todo portable, sin instalar)
- **JDK 17+** (con `javac`)
- **Apache Tomcat 10.1 o 11** (zip/tar.gz, solo descomprimir). *No sirve Tomcat 9: usa `jakarta.*`.*
- **MySQL 8+** encendido
- `mysql-connector-j-*.jar` en la carpeta `lib/` (ver `lib/LEEME.txt`)

## Pasos
1. Revisa usuario/contraseña en `web/WEB-INF/db.properties` (por defecto `root` sin contraseña).
2. `./build.sh /ruta/apache-tomcat-10.1.x`  (Windows: `build.bat C:\ruta\apache-tomcat-10.1.x`)
3. Define `JAVA_HOME` y arranca Tomcat: `bin/startup.sh` o `bin\startup.bat`
4. Abre <http://localhost:8080/bazar/>

La base **`bazar_datos`** y sus tablas se crean solas al arrancar (`web/WEB-INF/schema.sql`; también puedes correrlo a mano con `mysql < schema.sql`).
También se genera `build/bazar.war` por si prefieres copiarlo tú a `webapps/`.

## Estructura
| Capa | Archivos |
|---|---|
| Vistas JSP | `web/WEB-INF/jsp/*.jsp` (+ `_top.jspf` / `_bottom.jspf` incluidos con `<%@ include %>`, `<jsp:include>`, `<jsp:forward>`). Quedan en `WEB-INF`: solo se llegan a ellas por un servlet |
| Servlets | `src/bazar/*Servlet.java`: `doGet` prepara datos y reenvía al JSP; `doPost` procesa el formulario y redirige (Post/Redirect/Get) |
| JDBC | `Db.java` (conexión, consultas con `PreparedStatement`, transacciones), `Stock.java` (inventario, costos, combos) |
| Esquema | `web/WEB-INF/schema.sql`, `Init.java` lo ejecuta al arrancar |

| URL | Qué hace |
|---|---|
| `/caja` | Buscar, lector de código de barras (teclado+Enter), carrito, cobrar, apartar |
| `/inventario` `/compras` `/combos` | Productos, compras con flete, combos |
| `/proveedores` `/clientes` | Altas, edición y bajas |
| `/apartados` | Abonar, editar, entregar, cancelar |
| `/ventas` | Periodo día/semana/mes/todo, utilidad, editar/cancelar, **exportar CSV** (`?formato=csv`) |
| `/corte` | Corte de caja y su historial |

## Qué cambió respecto a la versión web anterior
- Los datos ya no viven en el navegador: están en MySQL (varios equipos pueden usar la misma base).
- Las ventanas de confirmación ahora son `confirm()` del navegador; el cobro, descuento de stock y movimientos de caja se hacen en **una transacción**.
- Se quitaron: modo offline/PWA, escáner con cámara (el lector USB funciona como teclado), sincronización con GitHub y respaldo JSON (usa `mysqldump bazar_datos > respaldo.sql`), y los fondos decorativos SVG de los temas (los 9 temas siguen, solo con colores; se guardan en una cookie).
- La búsqueda de la caja se aplica al pulsar *Buscar* (ya no es "en vivo").
- En Inventario, si eliges una compra con proveedor, el artículo toma el proveedor de la compra.

## Si algo falla
- Pantalla de error → revisa MySQL encendido, `db.properties` y que el jar esté en `WEB-INF/lib`.
- Los detalles técnicos quedan en `logs/catalina.*.log` de Tomcat.
- Si MySQL protesta por la zona horaria, descomenta `db.params` en `db.properties` y agrega `&serverTimezone=America/Mexico_City`.
