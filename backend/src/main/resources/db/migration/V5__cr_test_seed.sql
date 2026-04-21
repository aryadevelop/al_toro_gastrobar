-- =====================================================
-- V5: datos de soporte para tests Postman CR-10 y CR-12
-- =====================================================

-- Reserva 19: BASICA CONFIRMADA con anticipo para andres.morales@gmail.com (CR-10)
INSERT INTO Reserva (cliente_id, zona_id, decoracion_id, reserva_fecha_hora_llegada, reserva_numero_personas, reserva_notas, reserva_estado, reserva_tipo, reserva_fecha_creacion)
SELECT u.usuario_id, NULL, NULL, NOW() + INTERVAL '30 days', 2, NULL, 'CONFIRMADA', 'BASICA', NOW() - INTERVAL '1 day'
FROM Usuario u WHERE u.usuario_email = 'andres.morales@gmail.com';

-- Abono ANTICIPO para la reserva recién insertada (reserva_id=19)
INSERT INTO Abono (cajero_id, reserva_id, abono_monto, abono_fecha_hora, abono_metodo, abono_tipo)
SELECT 2, r.reserva_id, 40000, NOW() - INTERVAL '12 hours', 'TRANSFERENCIA', 'ANTICIPO'
FROM Reserva r
JOIN Usuario u ON u.usuario_id = r.cliente_id
WHERE u.usuario_email = 'andres.morales@gmail.com'
  AND r.reserva_estado = 'CONFIRMADA'
  AND r.reserva_tipo   = 'BASICA'
ORDER BY r.reserva_id DESC
LIMIT 1;

-- Reserva 20: ESPECIAL PENDIENTE con fecha pasada para andres.morales@gmail.com (CR-12)
-- La fecha de llegada < NOW() → el límite de las 16:00 ya pasó → sin reembolso, sin WhatsApp
INSERT INTO Reserva (cliente_id, zona_id, decoracion_id, reserva_fecha_hora_llegada, reserva_numero_personas, reserva_notas, reserva_estado, reserva_tipo, reserva_fecha_creacion)
SELECT u.usuario_id, NULL, NULL, NOW() - INTERVAL '1 day', 2, NULL, 'PENDIENTE', 'ESPECIAL', NOW() - INTERVAL '3 days'
FROM Usuario u WHERE u.usuario_email = 'andres.morales@gmail.com';
