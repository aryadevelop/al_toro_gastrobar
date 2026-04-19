-- =====================================================
-- V4: datos de soporte para tests Postman MR-08 y MR-13
-- =====================================================

-- Segunda decoración con costo para test MR-13 (ESPECIAL→ESPECIAL con cambio de valor)
INSERT INTO Decoracion (decoracion_nombre, decoracion_estado, decoracion_costo_adicional, decoracion_imagen_url) VALUES
('Bodas Premium', 'ACTIVO', 60000.00, 'https://picsum.photos/seed/decor-bodas/360/220');

-- Reserva CANCELADA para carlos.perez@gmail.com (test MR-08: modificar reserva ya cancelada)
INSERT INTO Reserva (cliente_id, zona_id, decoracion_id, reserva_fecha_hora_llegada, reserva_numero_personas, reserva_notas, reserva_estado, reserva_tipo, reserva_fecha_creacion)
SELECT u.usuario_id, NULL, NULL, NOW() - INTERVAL '2 days', 2, NULL, 'CANCELADA', 'BASICA', NOW() - INTERVAL '3 days'
FROM Usuario u WHERE u.usuario_email = 'carlos.perez@gmail.com';
