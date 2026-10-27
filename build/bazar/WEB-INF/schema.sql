-- Esquema de Bazar. Se ejecuta solo al arrancar la aplicacion (Init.java).
-- Tambien puedes correrlo a mano:  mysql -u root -p < schema.sql
CREATE DATABASE IF NOT EXISTS bazar_datos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bazar_datos;

CREATE TABLE IF NOT EXISTS proveedor (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(120) NOT NULL,
  telefono VARCHAR(40) NOT NULL DEFAULT '',
  notas VARCHAR(255) NOT NULL DEFAULT ''
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cliente (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(120) NOT NULL,
  telefono VARCHAR(40) NOT NULL DEFAULT '',
  notas VARCHAR(255) NOT NULL DEFAULT ''
) ENGINE=InnoDB;

-- Compra a proveedor: flete y cuantos articulos llegaron (el flete se reparte entre ellos)
CREATE TABLE IF NOT EXISTS compra (
  id INT AUTO_INCREMENT PRIMARY KEY,
  fecha DATE NULL,
  proveedor_id INT NULL,
  flete DECIMAL(10,2) NOT NULL DEFAULT 0,
  num_articulos INT NOT NULL,
  CONSTRAINT fk_compra_prov FOREIGN KEY (proveedor_id) REFERENCES proveedor(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- costo = costo total del lote del articulo; stock_inicial sirve para sacar el costo por pieza
CREATE TABLE IF NOT EXISTS producto (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(160) NOT NULL,
  codigo VARCHAR(60) NULL,
  precio DECIMAL(10,2) NOT NULL DEFAULT 0,
  costo DECIMAL(10,2) NOT NULL DEFAULT 0,
  stock INT NOT NULL DEFAULT 0,
  stock_inicial INT NOT NULL DEFAULT 0,
  proveedor_id INT NULL,
  compra_id INT NULL,
  INDEX ix_producto_codigo (codigo),
  CONSTRAINT fk_prod_prov FOREIGN KEY (proveedor_id) REFERENCES proveedor(id) ON DELETE SET NULL,
  CONSTRAINT fk_prod_compra FOREIGN KEY (compra_id) REFERENCES compra(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS combo (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(160) NOT NULL,
  precio DECIMAL(10,2) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS combo_item (
  combo_id INT NOT NULL,
  producto_id INT NOT NULL,
  cantidad INT NOT NULL,
  PRIMARY KEY (combo_id, producto_id),
  CONSTRAINT fk_ci_combo FOREIGN KEY (combo_id) REFERENCES combo(id) ON DELETE CASCADE,
  CONSTRAINT fk_ci_prod FOREIGN KEY (producto_id) REFERENCES producto(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS venta (
  id INT AUTO_INCREMENT PRIMARY KEY,
  fecha DATETIME(3) NOT NULL,
  cliente_id INT NULL,
  total DECIMAL(10,2) NOT NULL,
  estado VARCHAR(12) NOT NULL DEFAULT 'ok',      -- ok | cancelada
  apartado_id INT NULL,                          -- si viene de un apartado entregado
  INDEX ix_venta_fecha (fecha),
  CONSTRAINT fk_venta_cli FOREIGN KEY (cliente_id) REFERENCES cliente(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS apartado (
  id INT AUTO_INCREMENT PRIMARY KEY,
  fecha DATETIME(3) NOT NULL,
  cliente_id INT NULL,
  total DECIMAL(10,2) NOT NULL,
  abonado DECIMAL(10,2) NOT NULL DEFAULT 0,
  estado VARCHAR(12) NOT NULL DEFAULT 'abierto', -- abierto | liquidado | cancelado
  CONSTRAINT fk_apa_cli FOREIGN KEY (cliente_id) REFERENCES cliente(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Renglones de una venta o de un apartado (solo uno de los dos ids viene lleno)
CREATE TABLE IF NOT EXISTS linea (
  id INT AUTO_INCREMENT PRIMARY KEY,
  venta_id INT NULL,
  apartado_id INT NULL,
  nombre VARCHAR(180) NOT NULL,
  precio DECIMAL(10,2) NOT NULL,
  cantidad INT NOT NULL,
  costo_unit DECIMAL(14,4) NOT NULL DEFAULT 0,
  CONSTRAINT fk_linea_venta FOREIGN KEY (venta_id) REFERENCES venta(id) ON DELETE CASCADE,
  CONSTRAINT fk_linea_apa FOREIGN KEY (apartado_id) REFERENCES apartado(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Que productos (y cuantas piezas por unidad) descuenta cada renglon; sirve para devolver stock
CREATE TABLE IF NOT EXISTS linea_comp (
  linea_id INT NOT NULL,
  producto_id INT NOT NULL,
  cantidad INT NOT NULL,
  INDEX ix_comp_linea (linea_id),
  CONSTRAINT fk_comp_linea FOREIGN KEY (linea_id) REFERENCES linea(id) ON DELETE CASCADE,
  CONSTRAINT fk_comp_prod FOREIGN KEY (producto_id) REFERENCES producto(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Movimientos de efectivo: venta | abono | cancel | ajuste
CREATE TABLE IF NOT EXISTS mov_caja (
  id INT AUTO_INCREMENT PRIMARY KEY,
  fecha DATETIME(3) NOT NULL,
  tipo VARCHAR(10) NOT NULL,
  monto DECIMAL(10,2) NOT NULL,
  INDEX ix_mov_fecha (fecha)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS corte (
  id INT AUTO_INCREMENT PRIMARY KEY,
  fecha DATETIME(3) NOT NULL,
  fondo DECIMAL(10,2) NOT NULL DEFAULT 0,
  esperado DECIMAL(10,2) NOT NULL,
  contado DECIMAL(10,2) NOT NULL,
  diferencia DECIMAL(10,2) NOT NULL,
  nota VARCHAR(255) NOT NULL DEFAULT '',
  modificado DATETIME(3) NULL
) ENGINE=InnoDB;
