-- =====================================================
-- SCRIPT DE BASE DE DATOS
-- PostgreSQL 12+
-- =====================================================

SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

-- =====================================================
-- EXTENSIONES
-- =====================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =====================================================
-- SCHEMA
-- =====================================================
CREATE SCHEMA IF NOT EXISTS restaurante;
SET search_path TO restaurante, public;

-- =====================================================
-- TABLAS PRINCIPALES
-- =====================================================

-- Tabla Usuario
CREATE TABLE Usuario (
    usuario_id BIGSERIAL PRIMARY KEY,
    usuario_email VARCHAR(150) UNIQUE NOT NULL,
    usuario_password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE Usuario IS 'Tabla principal de autenticación de usuarios';
COMMENT ON COLUMN Usuario.usuario_password IS 'Password hasheado - usar bcrypt o similar';

-- Tabla Sesion
CREATE TABLE Sesion (
    sesion_id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    sesion_token VARCHAR(512) UNIQUE NOT NULL,
    sesion_fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sesion_activa BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_sesion_usuario FOREIGN KEY (usuario_id)
        REFERENCES Usuario(usuario_id) ON DELETE CASCADE
);

COMMENT ON TABLE Sesion IS 'Gestión de sesiones activas de usuarios';
COMMENT ON COLUMN Sesion.sesion_token IS 'Token JWT o similar para autenticación';

-- Tabla Cliente
CREATE TABLE Cliente (
    usuario_id BIGINT PRIMARY KEY,
    cliente_nombre VARCHAR(100) NOT NULL,
    cliente_telefono VARCHAR(10) NOT NULL,
    cliente_direccion VARCHAR(255),
    cliente_fecha_nacimiento DATE,
    cliente_puntos INTEGER NOT NULL DEFAULT 0,
    cliente_acepta_terminos BOOLEAN NOT NULL,
    cliente_fecha_aceptacion TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cliente_usuario FOREIGN KEY (usuario_id)
        REFERENCES Usuario(usuario_id) ON DELETE CASCADE,
    CONSTRAINT chk_cliente_puntos CHECK (cliente_puntos >= 0),
    CONSTRAINT chk_cliente_telefono CHECK (cliente_telefono ~ '^[0-9]{10}$')
);

COMMENT ON TABLE Cliente IS 'Información específica de clientes';
COMMENT ON COLUMN Cliente.cliente_puntos IS 'Sistema de fidelización de clientes';

-- Tabla Empleado
CREATE TABLE Empleado (
    usuario_id BIGINT PRIMARY KEY,
    empleado_nombre VARCHAR(100) NOT NULL,
    empleado_direccion VARCHAR(255),
    empleado_telefono VARCHAR(10) NOT NULL,
    empleado_fecha_ingreso DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_empleado_usuario FOREIGN KEY (usuario_id) 
        REFERENCES Usuario(usuario_id) ON DELETE CASCADE,
    CONSTRAINT chk_empleado_telefono CHECK (empleado_telefono ~ '^[0-9]{10}$')
);

COMMENT ON TABLE Empleado IS 'Información específica de empleados';

-- Tabla Usuario_Rol
CREATE TABLE Usuario_Rol (
    usuario_id BIGINT NOT NULL,
    rol_nombre VARCHAR(20) NOT NULL,
    rol_estado VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (usuario_id, rol_nombre),
    CONSTRAINT fk_usuario_rol_usuario FOREIGN KEY (usuario_id)
        REFERENCES Usuario(usuario_id) ON DELETE CASCADE,
    CONSTRAINT chk_rol_nombre CHECK (rol_nombre IN ('CLIENTE', 'MESERO', 'CAJERO', 'COCINERO', 'BARTENDER', 'ADM')),
    CONSTRAINT chk_rol_estado CHECK (rol_estado IN ('ACTIVO', 'INACTIVO'))
);

COMMENT ON TABLE Usuario_Rol IS 'Roles asignados a usuarios - un usuario puede tener múltiples roles';

-- Tabla Zona
CREATE TABLE Zona (
    zona_id BIGSERIAL PRIMARY KEY,
    zona_nombre VARCHAR(100) NOT NULL,
    zona_capacidad_personas INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_zona_capacidad CHECK (zona_capacidad_personas > 0)
);

COMMENT ON TABLE Zona IS 'Zonas del restaurante (terraza, salón, VIP, etc.)';

-- Tabla Decoracion
CREATE TABLE Decoracion (
    decoracion_id BIGSERIAL PRIMARY KEY,
    decoracion_nombre VARCHAR(100) NOT NULL,
    decoracion_estado VARCHAR(20) NOT NULL,
    decoracion_costo_adicional DECIMAL(12,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_decoracion_estado CHECK (decoracion_estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT chk_decoracion_costo CHECK (decoracion_costo_adicional >= 0)
);

COMMENT ON TABLE Decoracion IS 'Tipos de decoración disponibles para reservas especiales';

-- Tabla Decoracion_Zona (relación muchos a muchos)
CREATE TABLE Decoracion_Zona (
    decoracion_id BIGINT NOT NULL,
    zona_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (decoracion_id, zona_id),
    CONSTRAINT fk_decoracion_zona_decoracion FOREIGN KEY (decoracion_id)
        REFERENCES Decoracion(decoracion_id) ON DELETE CASCADE,
    CONSTRAINT fk_decoracion_zona_zona FOREIGN KEY (zona_id)
        REFERENCES Zona(zona_id) ON DELETE CASCADE
);

COMMENT ON TABLE Decoracion_Zona IS 'Relación entre decoraciones y zonas disponibles';

-- Tabla Reserva
CREATE TABLE Reserva (
    reserva_id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    zona_id BIGINT,
    decoracion_id BIGINT,
    reserva_fecha_hora_llegada TIMESTAMP NOT NULL,
    reserva_numero_personas INTEGER NOT NULL,
    reserva_notas TEXT,
    reserva_estado VARCHAR(20) NOT NULL,
    reserva_tipo VARCHAR(20) NOT NULL,
    reserva_fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reserva_cliente FOREIGN KEY (cliente_id)
        REFERENCES Cliente(usuario_id) ON DELETE RESTRICT,
    CONSTRAINT fk_reserva_zona FOREIGN KEY (zona_id)
        REFERENCES Zona(zona_id) ON DELETE SET NULL,
    CONSTRAINT fk_reserva_decoracion FOREIGN KEY (decoracion_id)
        REFERENCES Decoracion(decoracion_id) ON DELETE SET NULL,
    CONSTRAINT chk_reserva_estado CHECK (reserva_estado IN ('PENDIENTE', 'CONFIRMADA', 'ATENDIDA', 'CANCELADA', 'DEVUELTA', 'INASISTENCIA')),
    CONSTRAINT chk_reserva_tipo CHECK (reserva_tipo IN ('BASICA', 'ESPECIAL')),
    CONSTRAINT chk_reserva_personas CHECK (reserva_numero_personas > 0)
);

COMMENT ON TABLE Reserva IS 'Reservas de clientes - básicas y especiales';
COMMENT ON COLUMN Reserva.reserva_estado IS 'Estado del ciclo de vida de la reserva';

-- Tabla Abono
CREATE TABLE Abono (
    abono_id BIGSERIAL PRIMARY KEY,
    cajero_id BIGINT NOT NULL,
    reserva_id BIGINT NOT NULL,
    abono_monto DECIMAL(12,2) NOT NULL,
    abono_fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    abono_metodo VARCHAR(20) NOT NULL,
    abono_tipo VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_abono_cajero FOREIGN KEY (cajero_id)
        REFERENCES Empleado(usuario_id) ON DELETE RESTRICT,
    CONSTRAINT fk_abono_reserva FOREIGN KEY (reserva_id)
        REFERENCES Reserva(reserva_id) ON DELETE RESTRICT,
    CONSTRAINT chk_abono_metodo CHECK (abono_metodo IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'OTRO')),
    CONSTRAINT chk_abono_tipo CHECK (abono_tipo IN ('ANTICIPO', 'DEVOLUCION')),
    CONSTRAINT chk_abono_monto CHECK (abono_monto > 0)
);

COMMENT ON TABLE Abono IS 'Anticipos y devoluciones de reservas';

-- Tabla Visita
CREATE TABLE Visita (
    visita_id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT,
    reserva_id BIGINT,
    visita_fecha_hora_inicio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    visita_fecha_hora_fin TIMESTAMP,
    CONSTRAINT fk_visita_cliente FOREIGN KEY (cliente_id)
        REFERENCES Cliente(usuario_id) ON DELETE SET NULL,
    CONSTRAINT fk_visita_reserva FOREIGN KEY (reserva_id)
        REFERENCES Reserva(reserva_id) ON DELETE SET NULL,
    CONSTRAINT chk_visita_fechas CHECK (visita_fecha_hora_fin IS NULL OR visita_fecha_hora_fin >= visita_fecha_hora_inicio)
);

COMMENT ON TABLE Visita IS 'Registro de visitas al restaurante - con o sin reserva';

-- Tabla Mesa
CREATE TABLE Mesa (
    visita_id BIGINT PRIMARY KEY,
    zona_id BIGINT,
    mesero_id BIGINT NOT NULL,
    mesa_identificador VARCHAR(20) NOT NULL,
    mesa_numero_personas INTEGER NOT NULL,
    mesa_estado VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mesa_visita FOREIGN KEY (visita_id)
        REFERENCES Visita(visita_id) ON DELETE CASCADE,
    CONSTRAINT fk_mesa_zona FOREIGN KEY (zona_id)
        REFERENCES Zona(zona_id) ON DELETE SET NULL,
    CONSTRAINT fk_mesa_mesero FOREIGN KEY (mesero_id)
        REFERENCES Empleado(usuario_id) ON DELETE RESTRICT,
    CONSTRAINT chk_mesa_estado CHECK (mesa_estado IN ('ESPERA', 'EN_PREPARACION', 'ATENDIDA', 'CERRADA')),
    CONSTRAINT chk_mesa_personas CHECK (mesa_numero_personas > 0)
);

COMMENT ON TABLE Mesa IS 'Información de mesa por visita - asignación de mesero';

-- Tabla Notificacion
CREATE TABLE Notificacion (
    notificacion_id BIGSERIAL PRIMARY KEY,
    mesa_id BIGINT NOT NULL,
    empleado_id BIGINT NOT NULL,
    notificacion_estado VARCHAR(20) NOT NULL,
    notificacion_tipo VARCHAR(20) NOT NULL,
    notificacion_fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notificacion_mesa FOREIGN KEY (mesa_id)
        REFERENCES Mesa(visita_id) ON DELETE CASCADE,
    CONSTRAINT fk_notificacion_empleado FOREIGN KEY (empleado_id)
        REFERENCES Empleado(usuario_id) ON DELETE RESTRICT,
    CONSTRAINT chk_notificacion_estado CHECK (notificacion_estado IN ('ACTIVA', 'ATENDIDA')),
    CONSTRAINT chk_notificacion_tipo CHECK (notificacion_tipo IN ('ATENCION', 'PLATOS_LISTOS', 'BEBIDAS_LISTAS', 'CAMBIO'))
);

COMMENT ON TABLE Notificacion IS 'Sistema de notificaciones entre empleados';

-- Tabla Venta
CREATE TABLE Venta (
    visita_id BIGINT PRIMARY KEY,
    cajero_id BIGINT NOT NULL,
    venta_fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    venta_subtotal DECIMAL(12,2) NOT NULL,
    venta_descuento DECIMAL(12,2) NOT NULL DEFAULT 0,
    venta_total DECIMAL(12,2) NOT NULL,
    venta_metodo VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_venta_visita FOREIGN KEY (visita_id)
        REFERENCES Visita(visita_id) ON DELETE RESTRICT,
    CONSTRAINT fk_venta_cajero FOREIGN KEY (cajero_id)
        REFERENCES Empleado(usuario_id) ON DELETE RESTRICT,
    CONSTRAINT chk_venta_metodo CHECK (venta_metodo IN ('EFECTIVO', 'TRANSFERENCIA', 'TARJETA', 'OTRO')),
    CONSTRAINT chk_venta_subtotal CHECK (venta_subtotal >= 0),
    CONSTRAINT chk_venta_descuento CHECK (venta_descuento >= 0),
    CONSTRAINT chk_venta_total CHECK (venta_total >= 0)
);

COMMENT ON TABLE Venta IS 'Registro de ventas por visita';

-- Tabla CategoriaCarta
CREATE TABLE CategoriaCarta (
    categoriacarta_id SERIAL PRIMARY KEY,
    categoria_nombre VARCHAR(100) UNIQUE NOT NULL,
    orden INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE CategoriaCarta IS 'Categorías para organizar productos en la carta';

-- Tabla Producto
CREATE TABLE Producto (
    producto_id BIGSERIAL PRIMARY KEY,
    categoriacarta_id INTEGER NOT NULL,
    producto_nombre VARCHAR(100) NOT NULL,
    producto_estado VARCHAR(20) NOT NULL,
    producto_precio DECIMAL(12,2) NOT NULL,
    producto_tipo VARCHAR(20) NOT NULL,
    producto_categoria VARCHAR(20) NOT NULL,
    menu_especial BOOLEAN,
    stock_actual DECIMAL(12,3),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_producto_categoria FOREIGN KEY (categoriacarta_id)
        REFERENCES CategoriaCarta(categoriacarta_id) ON DELETE RESTRICT,
    CONSTRAINT chk_producto_estado CHECK (producto_estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT chk_producto_tipo CHECK (producto_tipo IN ('VENTA_DIRECTA', 'PREPARACION')),
    CONSTRAINT chk_producto_categoria CHECK (producto_categoria IN ('PLATO', 'BEBIDA', 'OTRO')),
    CONSTRAINT chk_producto_precio CHECK (producto_precio >= 0),
    CONSTRAINT chk_producto_stock CHECK (stock_actual IS NULL OR stock_actual >= 0),
    CONSTRAINT chk_menu_especial CHECK (menu_especial IS NULL OR producto_tipo = 'PREPARACION'),
    CONSTRAINT chk_stock_venta_directa CHECK (producto_tipo != 'VENTA_DIRECTA' OR stock_actual IS NOT NULL)
);

COMMENT ON TABLE Producto IS 'Catálogo de productos - platos, bebidas y otros';
COMMENT ON COLUMN Producto.menu_especial IS 'Solo aplica para productos de tipo PREPARACION';
COMMENT ON COLUMN Producto.stock_actual IS 'Solo aplica para productos de tipo VENTA_DIRECTA';

-- Tabla Insumo
CREATE TABLE Insumo (
    insumo_id BIGSERIAL PRIMARY KEY,
    insumo_nombre VARCHAR(100) NOT NULL,
    insumo_unidad VARCHAR(20) NOT NULL,
    insumo_stock_actual DECIMAL(12,3) NOT NULL DEFAULT 0,
    insumo_estado VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_insumo_unidad CHECK (insumo_unidad IN ('KG', 'G', 'L', 'ML', 'UNIDAD', 'DOCENA', 'OTRO')),
    CONSTRAINT chk_insumo_estado CHECK (insumo_estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT chk_insumo_stock CHECK (insumo_stock_actual >= 0)
);

COMMENT ON TABLE Insumo IS 'Insumos para preparación de productos';

-- Tabla Receta
CREATE TABLE Receta (
    insumo_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    receta_cantidad DECIMAL(12,3) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (insumo_id, producto_id),
    CONSTRAINT fk_receta_insumo FOREIGN KEY (insumo_id)
        REFERENCES Insumo(insumo_id) ON DELETE CASCADE,
    CONSTRAINT fk_receta_producto FOREIGN KEY (producto_id)
        REFERENCES Producto(producto_id) ON DELETE CASCADE,
    CONSTRAINT chk_receta_cantidad CHECK (receta_cantidad > 0)
);

COMMENT ON TABLE Receta IS 'Recetas - relación insumo-producto con cantidades';

-- Tabla Movimiento_Inventario
CREATE TABLE Movimiento_Inventario (
    movimiento_id BIGSERIAL PRIMARY KEY,
    empleado_id BIGINT NOT NULL,
    producto_id BIGINT,
    insumo_id BIGINT,
    movimiento_cantidad DECIMAL(12,3) NOT NULL,
    movimiento_tipo VARCHAR(20) NOT NULL,
    movimiento_proveedor VARCHAR(150),
    movimiento_numero_factura VARCHAR(150),
    movimiento_observaciones TEXT,
    movimiento_fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movimiento_empleado FOREIGN KEY (empleado_id)
        REFERENCES Empleado(usuario_id) ON DELETE RESTRICT,
    CONSTRAINT fk_movimiento_producto FOREIGN KEY (producto_id)
        REFERENCES Producto(producto_id) ON DELETE SET NULL,
    CONSTRAINT fk_movimiento_insumo FOREIGN KEY (insumo_id)
        REFERENCES Insumo(insumo_id) ON DELETE SET NULL,
    CONSTRAINT chk_movimiento_tipo CHECK (movimiento_tipo IN ('INGRESO', 'EGRESO')),
    CONSTRAINT chk_movimiento_item CHECK ((producto_id IS NOT NULL AND insumo_id IS NULL) OR (producto_id IS NULL AND insumo_id IS NOT NULL)),
    CONSTRAINT chk_movimiento_cantidad CHECK (movimiento_cantidad > 0)
);

COMMENT ON TABLE Movimiento_Inventario IS 'Historial de movimientos de inventario - ingresos y egresos';

-- Tabla Comanda
CREATE TABLE Comanda (
    comanda_id BIGSERIAL PRIMARY KEY,
    visita_id BIGINT NOT NULL,
    comanda_estacion VARCHAR(20) NOT NULL,
    comanda_fecha_hora_inicio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    comanda_fecha_hora_listo TIMESTAMP,
    comanda_notas TEXT,
    comanda_estado VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comanda_visita FOREIGN KEY (visita_id)
        REFERENCES Visita(visita_id) ON DELETE CASCADE,
    CONSTRAINT chk_comanda_estacion CHECK (comanda_estacion IN ('COCINA', 'BARRA')),
    CONSTRAINT chk_comanda_estado CHECK (comanda_estado IN ('PENDIENTE', 'EN_PREPARACION', 'LISTO', 'COMPLETADO')),
    CONSTRAINT chk_comanda_fechas CHECK (comanda_fecha_hora_listo IS NULL OR comanda_fecha_hora_listo >= comanda_fecha_hora_inicio)
);

COMMENT ON TABLE Comanda IS 'Comandas de cocina o barra';

-- Tabla Comanda_Detalle
CREATE TABLE Comanda_Detalle (
    comanda_detalle_id BIGSERIAL PRIMARY KEY,
    comanda_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    comanda_detalle_cantidad INTEGER NOT NULL,
    comanda_detalle_precio DECIMAL(12,2) NOT NULL,
    comanda_detalle_nombre VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comanda_detalle_comanda FOREIGN KEY (comanda_id)
        REFERENCES Comanda(comanda_id) ON DELETE CASCADE,
    CONSTRAINT fk_comanda_detalle_producto FOREIGN KEY (producto_id)
        REFERENCES Producto(producto_id) ON DELETE RESTRICT,
    CONSTRAINT chk_comanda_detalle_cantidad CHECK (comanda_detalle_cantidad > 0),
    CONSTRAINT chk_comanda_detalle_precio CHECK (comanda_detalle_precio >= 0)
);

COMMENT ON TABLE Comanda_Detalle IS 'Detalle de productos en cada comanda';

-- Tabla PreOrden_Detalle
CREATE TABLE PreOrden_Detalle (
    preorden_detalle_id BIGSERIAL PRIMARY KEY,
    reserva_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    preorden_detalle_cantidad INTEGER NOT NULL,
    preorden_detalle_nombre VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_preorden_reserva FOREIGN KEY (reserva_id)
        REFERENCES Reserva(reserva_id) ON DELETE CASCADE,
    CONSTRAINT fk_preorden_producto FOREIGN KEY (producto_id)
        REFERENCES Producto(producto_id) ON DELETE RESTRICT,
    CONSTRAINT chk_preorden_cantidad CHECK (preorden_detalle_cantidad > 0)
);

COMMENT ON TABLE PreOrden_Detalle IS 'Pre-orden de productos asociados a una reserva';

-- =====================================================
-- ÍNDICES PARA OPTIMIZACIÓN DE CONSULTAS
-- =====================================================

CREATE INDEX idx_usuario_email ON Usuario(usuario_email);

CREATE INDEX idx_sesion_usuario_id ON Sesion(usuario_id);
CREATE INDEX idx_sesion_token ON Sesion(sesion_token);
CREATE INDEX idx_sesion_activa ON Sesion(sesion_activa) WHERE sesion_activa = TRUE;
CREATE INDEX idx_sesion_fecha_creacion ON Sesion(sesion_fecha_creacion DESC);

CREATE INDEX idx_cliente_telefono ON Cliente(cliente_telefono);
CREATE INDEX idx_cliente_puntos ON Cliente(cliente_puntos DESC);

CREATE INDEX idx_empleado_telefono ON Empleado(empleado_telefono);
CREATE INDEX idx_empleado_fecha_ingreso ON Empleado(empleado_fecha_ingreso);

CREATE INDEX idx_usuario_rol_rol_nombre ON Usuario_Rol(rol_nombre);
CREATE INDEX idx_usuario_rol_estado ON Usuario_Rol(rol_estado);
CREATE INDEX idx_usuario_rol_activo ON Usuario_Rol(usuario_id, rol_nombre) WHERE rol_estado = 'ACTIVO';

CREATE INDEX idx_reserva_cliente_id ON Reserva(cliente_id);
CREATE INDEX idx_reserva_fecha_hora_llegada ON Reserva(reserva_fecha_hora_llegada);
CREATE INDEX idx_reserva_estado ON Reserva(reserva_estado);
CREATE INDEX idx_reserva_fecha_creacion ON Reserva(reserva_fecha_creacion DESC);
CREATE INDEX idx_reserva_zona_id ON Reserva(zona_id);
CREATE INDEX idx_reserva_fecha_estado ON Reserva(reserva_fecha_hora_llegada, reserva_estado);
CREATE INDEX idx_reserva_activas_hoy ON Reserva(reserva_fecha_hora_llegada, reserva_estado)
    WHERE reserva_estado IN ('PENDIENTE', 'CONFIRMADA');

CREATE INDEX idx_abono_reserva_id ON Abono(reserva_id);
CREATE INDEX idx_abono_cajero_id ON Abono(cajero_id);
CREATE INDEX idx_abono_fecha_hora ON Abono(abono_fecha_hora DESC);

CREATE INDEX idx_visita_cliente_id ON Visita(cliente_id);
CREATE INDEX idx_visita_reserva_id ON Visita(reserva_id);
CREATE INDEX idx_visita_fecha_inicio ON Visita(visita_fecha_hora_inicio DESC);
CREATE INDEX idx_visita_fecha_fin ON Visita(visita_fecha_hora_fin);
CREATE INDEX idx_visita_activas ON Visita(visita_fecha_hora_inicio) WHERE visita_fecha_hora_fin IS NULL;

CREATE INDEX idx_mesa_mesero_id ON Mesa(mesero_id);
CREATE INDEX idx_mesa_zona_id ON Mesa(zona_id);
CREATE INDEX idx_mesa_estado ON Mesa(mesa_estado);
CREATE INDEX idx_mesa_identificador ON Mesa(mesa_identificador);
CREATE INDEX idx_mesa_activas ON Mesa(mesero_id, mesa_estado) WHERE mesa_estado IN ('ESPERA', 'EN_PREPARACION', 'ATENDIDA');

CREATE INDEX idx_notificacion_mesa_id ON Notificacion(mesa_id);
CREATE INDEX idx_notificacion_empleado_id ON Notificacion(empleado_id);
CREATE INDEX idx_notificacion_estado ON Notificacion(notificacion_estado);
CREATE INDEX idx_notificacion_fecha_hora ON Notificacion(notificacion_fecha_hora DESC);
CREATE INDEX idx_notificacion_activas ON Notificacion(empleado_id, notificacion_fecha_hora DESC)
    WHERE notificacion_estado = 'ACTIVA';

CREATE INDEX idx_venta_cajero_id ON Venta(cajero_id);
CREATE INDEX idx_venta_fecha_hora ON Venta(venta_fecha_hora DESC);
CREATE INDEX idx_venta_metodo ON Venta(venta_metodo);
CREATE INDEX idx_venta_fecha_total ON Venta(venta_fecha_hora DESC, venta_total);

CREATE INDEX idx_producto_categoria_id ON Producto(categoriacarta_id);
CREATE INDEX idx_producto_estado ON Producto(producto_estado);
CREATE INDEX idx_producto_tipo ON Producto(producto_tipo);
CREATE INDEX idx_producto_categoria ON Producto(producto_categoria);
CREATE INDEX idx_producto_nombre ON Producto(producto_nombre);
CREATE INDEX idx_producto_activos ON Producto(categoriacarta_id, producto_nombre)
    WHERE producto_estado = 'ACTIVO';

CREATE INDEX idx_insumo_nombre ON Insumo(insumo_nombre);
CREATE INDEX idx_insumo_estado ON Insumo(insumo_estado);
CREATE INDEX idx_insumo_stock ON Insumo(insumo_stock_actual);

CREATE INDEX idx_receta_producto_id ON Receta(producto_id);

CREATE INDEX idx_movimiento_empleado_id ON Movimiento_Inventario(empleado_id);
CREATE INDEX idx_movimiento_producto_id ON Movimiento_Inventario(producto_id);
CREATE INDEX idx_movimiento_insumo_id ON Movimiento_Inventario(insumo_id);
CREATE INDEX idx_movimiento_fecha_hora ON Movimiento_Inventario(movimiento_fecha_hora DESC);
CREATE INDEX idx_movimiento_tipo ON Movimiento_Inventario(movimiento_tipo);
CREATE INDEX idx_movimiento_fecha_tipo ON Movimiento_Inventario(movimiento_fecha_hora DESC, movimiento_tipo);

CREATE INDEX idx_comanda_visita_id ON Comanda(visita_id);
CREATE INDEX idx_comanda_estacion ON Comanda(comanda_estacion);
CREATE INDEX idx_comanda_estado ON Comanda(comanda_estado);
CREATE INDEX idx_comanda_fecha_inicio ON Comanda(comanda_fecha_hora_inicio DESC);
CREATE INDEX idx_comanda_pendientes ON Comanda(comanda_estacion, comanda_fecha_hora_inicio)
    WHERE comanda_estado IN ('PENDIENTE', 'EN_PREPARACION');

CREATE INDEX idx_comanda_detalle_comanda_id ON Comanda_Detalle(comanda_id);
CREATE INDEX idx_comanda_detalle_producto_id ON Comanda_Detalle(producto_id);

CREATE INDEX idx_preorden_reserva_id ON PreOrden_Detalle(reserva_id);
CREATE INDEX idx_preorden_producto_id ON PreOrden_Detalle(producto_id);

-- =====================================================
-- FUNCIONES Y TRIGGERS PARA AUDITORÍA
-- =====================================================

CREATE OR REPLACE FUNCTION actualizar_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_usuario_updated_at BEFORE UPDATE ON Usuario
    FOR EACH ROW EXECUTE FUNCTION actualizar_updated_at();

CREATE TRIGGER trg_cliente_updated_at BEFORE UPDATE ON Cliente
    FOR EACH ROW EXECUTE FUNCTION actualizar_updated_at();

CREATE TRIGGER trg_empleado_updated_at BEFORE UPDATE ON Empleado
    FOR EACH ROW EXECUTE FUNCTION actualizar_updated_at();

CREATE TRIGGER trg_usuario_rol_updated_at BEFORE UPDATE ON Usuario_Rol
    FOR EACH ROW EXECUTE FUNCTION actualizar_updated_at();

CREATE TRIGGER trg_zona_updated_at BEFORE UPDATE ON Zona
    FOR EACH ROW EXECUTE FUNCTION actualizar_updated_at();

CREATE TRIGGER trg_decoracion_updated_at BEFORE UPDATE ON Decoracion
    FOR EACH ROW EXECUTE FUNCTION actualizar_updated_at();

CREATE TRIGGER trg_reserva_updated_at BEFORE UPDATE ON Reserva
    FOR EACH ROW EXECUTE FUNCTION actualizar_updated_at();

CREATE TRIGGER trg_mesa_updated_at BEFORE UPDATE ON Mesa
    FOR EACH ROW EXECUTE FUNCTION actualizar_updated_at();

CREATE TRIGGER trg_categoriacarta_updated_at BEFORE UPDATE ON CategoriaCarta
    FOR EACH ROW EXECUTE FUNCTION actualizar_updated_at();

CREATE TRIGGER trg_producto_updated_at BEFORE UPDATE ON Producto
    FOR EACH ROW EXECUTE FUNCTION actualizar_updated_at();

CREATE TRIGGER trg_insumo_updated_at BEFORE UPDATE ON Insumo
    FOR EACH ROW EXECUTE FUNCTION actualizar_updated_at();

CREATE TRIGGER trg_receta_updated_at BEFORE UPDATE ON Receta
    FOR EACH ROW EXECUTE FUNCTION actualizar_updated_at();

CREATE TRIGGER trg_comanda_updated_at BEFORE UPDATE ON Comanda
    FOR EACH ROW EXECUTE FUNCTION actualizar_updated_at();

-- =====================================================
-- VISTAS ÚTILES
-- =====================================================

-- Vista de usuarios con sus roles activos
CREATE OR REPLACE VIEW v_usuarios_roles AS
SELECT
    u.usuario_id,
    u.usuario_email,
    ur.rol_nombre,
    ur.rol_estado,
    CASE
        WHEN c.usuario_id IS NOT NULL THEN c.cliente_nombre
        WHEN e.usuario_id IS NOT NULL THEN e.empleado_nombre
        ELSE NULL
    END AS nombre_completo
FROM Usuario u
LEFT JOIN Usuario_Rol ur ON u.usuario_id = ur.usuario_id
LEFT JOIN Cliente c ON u.usuario_id = c.usuario_id
LEFT JOIN Empleado e ON u.usuario_id = e.usuario_id;

COMMENT ON VIEW v_usuarios_roles IS 'Vista consolidada de usuarios con sus roles';

-- Vista de reservas con información del cliente
CREATE OR REPLACE VIEW v_reservas_detalle AS
SELECT
    r.reserva_id,
    r.reserva_fecha_hora_llegada,
    r.reserva_numero_personas,
    r.reserva_estado,
    r.reserva_tipo,
    c.cliente_nombre,
    c.cliente_telefono,
    u.usuario_email AS cliente_email,
    z.zona_nombre,
    d.decoracion_nombre,
    d.decoracion_costo_adicional,
    r.reserva_fecha_creacion
FROM Reserva r
INNER JOIN Cliente c ON r.cliente_id = c.usuario_id
INNER JOIN Usuario u ON c.usuario_id = u.usuario_id
LEFT JOIN Zona z ON r.zona_id = z.zona_id
LEFT JOIN Decoracion d ON r.decoracion_id = d.decoracion_id;

COMMENT ON VIEW v_reservas_detalle IS 'Vista detallada de reservas con información del cliente';

-- Vista de productos disponibles con categoría
CREATE OR REPLACE VIEW v_productos_disponibles AS
SELECT
    p.producto_id,
    p.producto_nombre,
    p.producto_precio,
    p.producto_categoria,
    p.producto_tipo,
    p.menu_especial,
    p.stock_actual,
    cc.categoria_nombre,
    cc.orden
FROM Producto p
INNER JOIN CategoriaCarta cc ON p.categoriacarta_id = cc.categoriacarta_id
WHERE p.producto_estado = 'ACTIVO'
  AND cc.activo = TRUE
ORDER BY cc.orden, p.producto_nombre;

COMMENT ON VIEW v_productos_disponibles IS 'Vista de productos activos ordenados por categoría';

-- Vista de comandas activas
CREATE OR REPLACE VIEW v_comandas_activas AS
SELECT
    c.comanda_id,
    c.visita_id,
    c.comanda_estacion,
    c.comanda_estado,
    c.comanda_fecha_hora_inicio,
    m.mesa_identificador,
    m.mesero_id,
    e.empleado_nombre AS mesero_nombre,
    COUNT(cd.comanda_detalle_id) AS cantidad_items
FROM Comanda c
INNER JOIN Mesa m ON c.visita_id = m.visita_id
INNER JOIN Empleado e ON m.mesero_id = e.usuario_id
LEFT JOIN Comanda_Detalle cd ON c.comanda_id = cd.comanda_id
WHERE c.comanda_estado IN ('PENDIENTE', 'EN_PREPARACION')
GROUP BY c.comanda_id, c.visita_id, c.comanda_estacion, c.comanda_estado,
         c.comanda_fecha_hora_inicio, m.mesa_identificador, m.mesero_id, e.empleado_nombre;

COMMENT ON VIEW v_comandas_activas IS 'Vista de comandas en proceso';

-- =====================================================
-- CONFIGURACIONES ADICIONALES
-- =====================================================

ALTER TABLE Sesion SET (autovacuum_vacuum_scale_factor = 0.1);
ALTER TABLE Visita SET (autovacuum_vacuum_scale_factor = 0.1);
ALTER TABLE Comanda SET (autovacuum_vacuum_scale_factor = 0.1);
ALTER TABLE Notificacion SET (autovacuum_vacuum_scale_factor = 0.1);
ALTER TABLE Movimiento_Inventario SET (autovacuum_vacuum_scale_factor = 0.1);

-- =====================================================
-- FIN DEL SCRIPT
-- =====================================================
