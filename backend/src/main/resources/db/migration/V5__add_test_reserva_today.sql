-- Insertar una nueva reserva CONFIRMADA con pre-orden PARA HOY
SET search_path TO restaurante, public;
WITH new_reserva AS (
    INSERT INTO Reserva (cliente_id, zona_id, decoracion_id, reserva_fecha_hora_llegada, reserva_numero_personas, reserva_notas, reserva_estado, reserva_tipo, reserva_fecha_creacion)
    VALUES (13, 1, NULL, NOW(), 2, 'Prueba de Anticipo HOY', 'CONFIRMADA', 'BASICA', NOW() - INTERVAL '1 day')
    RETURNING reserva_id
), new_comanda AS (
    INSERT INTO Comanda (visita_id, reserva_id, comanda_estacion, comanda_fecha_hora_inicio, comanda_notas, comanda_estado)
    SELECT NULL, reserva_id, 'COCINA', NOW(), NULL, 'PRE_RESERVA'
    FROM new_reserva
    RETURNING comanda_id
)
INSERT INTO Comanda_Item (comanda_id, producto_id, comanda_item_cantidad, comanda_item_precio, comanda_item_descripcion)
SELECT comanda_id, p.producto_id, 2, p.producto_precio, 'Prueba de Anticipo'
FROM new_comanda
JOIN Producto p ON p.producto_nombre = 'Picanha'
LIMIT 1;
