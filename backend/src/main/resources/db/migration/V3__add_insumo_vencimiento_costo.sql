-- V3: Agrega campos de fecha de vencimiento y costo unitario a la tabla Insumo
-- Requerido para los casos de prueba BDD: alerta de vencimiento próximo y
-- pre-diligenciado del costo unitario en el formulario de ingreso de insumo.

ALTER TABLE restaurante.Insumo
    ADD COLUMN IF NOT EXISTS insumo_fecha_vencimiento DATE        NULL,
    ADD COLUMN IF NOT EXISTS insumo_costo_unitario    DECIMAL(12,2) NULL;

COMMENT ON COLUMN restaurante.Insumo.insumo_fecha_vencimiento IS
    'Fecha de vencimiento del insumo. Nulo si no aplica.';

COMMENT ON COLUMN restaurante.Insumo.insumo_costo_unitario IS
    'Costo unitario actual del insumo. Se actualiza al registrar un ingreso con costo.';
