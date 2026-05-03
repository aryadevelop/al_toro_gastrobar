-- =====================================================
-- SCRIPT DE DATOS DE DESARROLLO
-- Depende de: V1__init_schema.sql, V2__seed_data.sql
-- Contraseña de todos los usuarios: Al.Toro2026! (bcrypt cost=12, hash precomputado)
-- =====================================================

SET client_encoding = 'UTF8';
SET search_path TO restaurante, public;

-- =====================================================
-- 1. Usuario
--    IDs 1-10: empleados | IDs 11-22: clientes
-- =====================================================
INSERT INTO Usuario (usuario_email, usuario_password) VALUES
-- Empleados
('admin@altoro.com',          '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('cajero1@altoro.com',        '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('cajero2@altoro.com',        '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('mesero1@altoro.com',        '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('mesero2@altoro.com',        '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('mesero3@altoro.com',        '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('cocinero1@altoro.com',      '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('cocinero2@altoro.com',      '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('bartender1@altoro.com',     '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('bartender2@altoro.com',     '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
-- Clientes
('carlos.perez@gmail.com',    '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('laura.gomez@gmail.com',     '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('andres.morales@gmail.com',  '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('sofia.ramirez@gmail.com',   '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('juan.torres@gmail.com',     '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('valentina.cruz@gmail.com',  '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('miguel.herrera@gmail.com',  '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('diana.lopez@gmail.com',     '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('sergio.castillo@gmail.com', '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('paola.rojas@gmail.com',     '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('nicolas.vargas@gmail.com',  '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('isabella.rios@gmail.com',   '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK');

-- =====================================================
-- 2. Empleado  (usuario_id 1-10)
-- =====================================================
INSERT INTO Empleado (usuario_id, empleado_nombre, empleado_direccion, empleado_telefono, empleado_fecha_ingreso) VALUES
(1,  'Martha Delgado',    'Calle 5 # 8-32 Popayán',        '3124567890', '2022-01-15'),
(2,  'Ricardo Zúñiga',    'Carrera 9 # 12-45 Popayán',     '3135678901', '2023-03-10'),
(3,  'Camila Ospina',     'Calle 15 # 4-67 Popayán',       '3146789012', '2023-06-01'),
(4,  'Diego Muñoz',       'Av. Bolívar # 20-11 Popayán',   '3157890123', '2023-07-15'),
(5,  'Tatiana Palacios',  'Calle 8 # 16-30 Popayán',       '3168901234', '2024-01-20'),
(6,  'Felipe Solano',     'Carrera 4 # 7-89 Popayán',      '3179012345', '2024-02-10'),
(7,  'Jorge Erazo',       'Calle 25 # 9-14 Popayán',       '3180123456', '2023-08-01'),
(8,  'Luz Marina Ruiz',   'Barrio Caldono Cll 3 Popayán',  '3191234567', '2023-09-15'),
(9,  'Andrés Caicedo',    'Calle 10 # 22-05 Popayán',      '3202345678', '2023-11-01'),
(10, 'Juliana Mosquera',  'Carrera 6 # 18-90 Popayán',     '3213456789', '2024-03-01');

-- =====================================================
-- 3. Cliente  (usuario_id 11-22)
-- =====================================================
INSERT INTO Cliente (usuario_id, cliente_nombre, cliente_telefono, cliente_direccion, cliente_fecha_nacimiento, cliente_puntos, cliente_puntos_acumulados, cliente_acepta_terminos, cliente_fecha_aceptacion) VALUES
(11, 'Carlos Andrés Pérez',    '3100001111', 'Calle 3 # 5-10 Popayán',         '1990-04-12', 1200, 1200, TRUE, '2024-01-10 10:30:00'),
(12, 'Laura Valentina Gómez',  '3111112222', 'Carrera 7 # 11-22 Popayán',      '1995-07-25', 800,  800,  TRUE, '2024-02-14 15:00:00'),
(13, 'Andrés Felipe Morales',  '3122223333', 'Barrio Bolívar Popayán',          '1988-11-03', 2500, 2500, TRUE, '2023-12-01 09:00:00'),
(14, 'Sofía Elena Ramírez',    '3133334444', 'Av. Panamericana # 40-12',        '1992-03-18', 350,  350,  TRUE, '2024-03-20 11:00:00'),
(15, 'Juan Camilo Torres',     '3144445555', 'Calle 20 # 8-55 Popayán',        '1985-09-30', 4100, 4100, TRUE, '2023-08-05 14:00:00'),
(16, 'Valentina Cruz Lemos',   '3155556666', 'Carrera 12 # 3-78 Popayán',      '1998-12-15', 150,  150,  TRUE, '2024-04-01 16:30:00'),
(17, 'Miguel Ángel Herrera',   '3166667777', 'Urb. Los Pinos Casa 12 Popayán', '1993-06-22', 900,  900,  TRUE, '2024-01-25 12:00:00'),
(18, 'Diana Marcela López',    '3177778888', 'Calle 9 # 15-43 Popayán',        '1991-02-08', 3200, 3200, TRUE, '2023-10-10 10:00:00'),
(19, 'Sergio Iván Castillo',   '3188889999', 'Barrio El Lago Popayán',          '1987-08-14', 600,  600,  TRUE, '2024-02-28 09:30:00'),
(20, 'Paola Andrea Rojas',     '3199990000', 'Carrera 2 # 6-88 Popayán',       '1996-05-05', 1800, 1800, TRUE, '2023-11-15 17:00:00'),
(21, 'Nicolás Esteban Vargas', '3200001122', 'Calle 14 # 10-23 Popayán',       '1994-10-28', 720,  720,  TRUE, '2024-03-05 13:00:00'),
(22, 'Isabella Sofía Ríos',    '3211112233', 'Av. Colombia # 55-10 Popayán',   '2000-01-19', 90,   90,   TRUE, '2024-04-08 18:00:00');

-- =====================================================
-- 4. Usuario_Rol
-- =====================================================
INSERT INTO Usuario_Rol (usuario_id, rol_nombre, rol_estado) VALUES
-- Admin
(1,  'ADMIN',       'ACTIVO'),
-- Cajeros
(2,  'CAJERO',    'ACTIVO'),
(2,  'MESERO',    'ACTIVO'),
(2,  'BARTENDER', 'ACTIVO'),
(2,  'ADMIN',       'INACTIVO'),
(3,  'CAJERO',    'ACTIVO'),
-- Meseros
(4,  'MESERO',    'ACTIVO'),
(4,  'ADMIN',       'ACTIVO'),
(5,  'MESERO',    'ACTIVO'),
(6,  'MESERO',    'ACTIVO'),
-- Cocineros
(7,  'COCINERO',  'INACTIVO'),
(7,  'BARTENDER',  'INACTIVO'),
(8,  'COCINERO',  'ACTIVO'),
(8,  'MESERO',  'INACTIVO'),
-- Bartenders
(9,  'BARTENDER', 'ACTIVO'),
(10, 'BARTENDER', 'ACTIVO'),
-- Clientes
(11, 'CLIENTE',   'INACTIVO'),
(12, 'CLIENTE',   'INACTIVO'),
(13, 'CLIENTE',   'ACTIVO'),
(14, 'CLIENTE',   'ACTIVO'),
(15, 'CLIENTE',   'ACTIVO'),
(16, 'CLIENTE',   'ACTIVO'),
(17, 'CLIENTE',   'ACTIVO'),
(18, 'CLIENTE',   'ACTIVO'),
(19, 'CLIENTE',   'ACTIVO'),
(20, 'CLIENTE',   'ACTIVO'),
(21, 'CLIENTE',   'ACTIVO'),
(22, 'CLIENTE',   'ACTIVO'),
(1,  'CAJERO',    'INACTIVO');

-- =====================================================
-- 5. Sesion
-- =====================================================
INSERT INTO Sesion (usuario_id, sesion_token, sesion_refresh_token, sesion_fecha_creacion, sesion_activa) VALUES
(1,  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.admin001',   'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh.admin001',   NOW() - INTERVAL '2 hours',   TRUE),
(4,  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mesero001',  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh.mesero001',  NOW() - INTERVAL '1 hour',    TRUE),
(5,  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mesero002',  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh.mesero002',  NOW() - INTERVAL '90 minutes',TRUE),
(9,  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.bartend001', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh.bartend001', NOW() - INTERVAL '45 minutes',TRUE),
(7,  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.cocinero01', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh.cocinero01', NOW() - INTERVAL '3 hours',   TRUE),
(2,  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.cajero0001', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh.cajero0001', NOW() - INTERVAL '30 minutes',TRUE),
(11, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.cliente011', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh.cliente011', NOW() - INTERVAL '5 days',    FALSE),
(12, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.cliente012', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh.cliente012', NOW() - INTERVAL '2 days',    FALSE),
(13, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.cliente013', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh.cliente013', NOW() - INTERVAL '10 hours',  FALSE),
(15, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.cliente015', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh.cliente015', NOW() - INTERVAL '1 day',     FALSE);

-- =====================================================
-- 6. Reserva
--    Decoración IDs (V2): 1=Velas Románticas, 2=Cumpleaños Premium,
--    3=Decoración Empresarial, 4=Mesa Temática Toro, 5=Minimalista Elegante,
--    6=Globos y Flores
--    Zona IDs (V2): 1=Salón Principal, 2=Terraza, 3=VIP, 6=Sala Privada
-- =====================================================
INSERT INTO Reserva (cliente_id, zona_id, decoracion_id, reserva_fecha_hora_llegada, reserva_numero_personas, reserva_notas, reserva_estado, reserva_tipo, reserva_fecha_creacion) VALUES
-- Pasadas atendidas o canceladas
(13, 1, NULL, NOW() - INTERVAL '30 days',  4,  'Sin notas',                                 'ATENDIDA',     'BASICA',   NOW() - INTERVAL '32 days'),
(15, 3, 2,    NOW() - INTERVAL '25 days',  18, 'Cumpleaños de Laura, decoración VIP',        'ATENDIDA',     'ESPECIAL', NOW() - INTERVAL '27 days'),
(18, 2, 3,    NOW() - INTERVAL '20 days',  16, 'Reunión de negocios',                        'ATENDIDA',     'ESPECIAL', NOW() - INTERVAL '22 days'),
(11, 1, NULL, NOW() - INTERVAL '15 days',  2,  NULL,                                         'ATENDIDA',     'BASICA',   NOW() - INTERVAL '16 days'),
(20, 3, 1,    NOW() - INTERVAL '10 days',  2,  'Aniversario pareja',                         'ATENDIDA',     'ESPECIAL', NOW() - INTERVAL '12 days'),
(14, 1, NULL, NOW() - INTERVAL '7 days',   3,  NULL,                                         'CANCELADA',    'BASICA',   NOW() - INTERVAL '9 days'),
(12, 2, NULL, NOW() - INTERVAL '5 days',   5,  'Mesa exterior preferida',                    'INASISTENCIA', 'BASICA',   NOW() - INTERVAL '7 days'),
(17, NULL, 4, NOW() - INTERVAL '3 days',   98, 'Decoración toro para evento',                'ATENDIDA',     'ESPECIAL', NOW() - INTERVAL '5 days'),
-- Próximas confirmadas o pendientes
(13, 1, NULL, NOW() - INTERVAL '1 hour',   4,  NULL,                                         'ATENDIDA',     'BASICA',   NOW() - INTERVAL '2 days'),
(15, 3, 2,    NOW() + INTERVAL '2 days',   12, 'Evento corporativo con decoración',          'CONFIRMADA',   'ESPECIAL', NOW() - INTERVAL '3 days'),
(18, 2, NULL, NOW() + INTERVAL '3 days',   6,  NULL,                                         'PENDIENTE',    'BASICA',   NOW() - INTERVAL '1 day'),
(21, 3, 1,    NOW() + INTERVAL '5 days',   2,  'Cena romántica velas',                       'PENDIENTE',    'ESPECIAL', NOW()),
(19, 1, NULL, NOW() + INTERVAL '7 days',   3,  NULL,                                         'PENDIENTE',    'BASICA',   NOW()),
(22, 2, 5,    NOW() + INTERVAL '10 days',  14, 'Primera visita del cliente',                 'PENDIENTE',    'ESPECIAL', NOW()),
-- Con devolución  (IDs 15 y 16 → referenciados en los Abonos de devolución)
(16, 3, NULL, NOW() - INTERVAL '2 days',   2,  'Cancelación con devolución',                 'DEVUELTA',     'BASICA',   NOW() - INTERVAL '10 days'),
(13, 3, NULL, NOW() - INTERVAL '8 days',   2,  'Cancelación con devolución',                 'DEVUELTA',     'ESPECIAL', NOW() - INTERVAL '40 days'),
-- Reserva 17 → Menú 8d (Cerdo y Res en Vino) — cubre el último menú especial disponible
(19, 2, 5,    NOW() + INTERVAL '15 days', 11,  'Fiesta de graduación',                       'PENDIENTE',    'ESPECIAL', NOW());
-- =====================================================
-- 7. Abono
-- =====================================================
INSERT INTO Abono (cajero_id, reserva_id, abono_monto, abono_fecha_hora, abono_metodo, abono_tipo) VALUES
(2, 2,  50000, NOW() - INTERVAL '27 days', 'TRANSFERENCIA', 'ANTICIPO'),
(2, 3,  40000, NOW() - INTERVAL '22 days', 'TARJETA',       'ANTICIPO'),
(3, 5,  60000, NOW() - INTERVAL '12 days', 'EFECTIVO',      'ANTICIPO'),
(2, 8,  30000, NOW() - INTERVAL '5 days',  'TRANSFERENCIA', 'ANTICIPO'),
(2, 10, 80000, NOW() - INTERVAL '3 days',  'TARJETA',       'ANTICIPO'),
(3, 12, 50000, NOW() - INTERVAL '1 day',   'TRANSFERENCIA', 'ANTICIPO'),
(3, 10, 50000, NOW() - INTERVAL '1 day',   'TRANSFERENCIA', 'ANTICIPO'),
(3, 10, 50000, NOW() - INTERVAL '1 day',   'TRANSFERENCIA', 'ANTICIPO'),
-- Anticipos para reservas que luego fueron devueltas
(2, 15, 30000, NOW() - INTERVAL '12 days', 'EFECTIVO',      'ANTICIPO'),
(2, 16, 80000, NOW() - INTERVAL '12 days', 'TRANSFERENCIA', 'ANTICIPO'),
-- Anticipo reserva 17 con método OTRO → cubre enum faltante
(3, 17, 45000, NOW() - INTERVAL '1 day',   'OTRO',          'ANTICIPO'),
-- Devoluciones por reservas DEVUELTA (reservas 15 y 16)
(2, 15, 30000, NOW() - INTERVAL '2 days',  'TRANSFERENCIA', 'DEVOLUCION'),
(2, 16, 80000, NOW() - INTERVAL '8 days',  'TRANSFERENCIA', 'DEVOLUCION');

-- =====================================================
-- 9. Visita
-- =====================================================
INSERT INTO Visita (cliente_id, reserva_id, visita_fecha_hora_inicio, visita_fecha_hora_fin) VALUES
-- Visitas cerradas (historial)
(13, 1,  NOW() - INTERVAL '30 days 7 hours', NOW() - INTERVAL '30 days 4 hours'),
(15, 2,  NOW() - INTERVAL '25 days 7 hours', NOW() - INTERVAL '25 days 3 hours'),
(18, 3,  NOW() - INTERVAL '20 days 8 hours', NOW() - INTERVAL '20 days 5 hours'),
(11, 4,  NOW() - INTERVAL '15 days 7 hours', NOW() - INTERVAL '15 days 5 hours'),
(20, 5,  NOW() - INTERVAL '10 days 8 hours', NOW() - INTERVAL '10 days 4 hours'),
(17, 8,  NOW() - INTERVAL '3 days 7 hours',  NOW() - INTERVAL '3 days 4 hours'),
-- Walk-in sin reserva
(12, NULL, NOW() - INTERVAL '5 days 8 hours', NOW() - INTERVAL '5 days 5 hours'),
(NULL, NULL, NOW() - INTERVAL '4 days 7 hours', NOW() - INTERVAL '4 days 4 hours'),
(19, NULL, NOW() - INTERVAL '2 days 8 hours', NOW() - INTERVAL '2 days 5 hours'),
-- Visitas activas (en curso)
(11, NULL, NOW() - INTERVAL '1 hour',   NULL),
(13, 9,   NOW() - INTERVAL '45 minutes',NULL),
(15, NULL, NOW() - INTERVAL '30 minutes',NULL);

-- =====================================================
-- 10. Mesa
-- =====================================================
INSERT INTO Mesa (visita_id, zona_id, mesero_id, mesa_identificador, mesa_numero_personas, mesa_estado, mesa_notas) VALUES
-- Mesas de visitas históricas (CERRADA)
(1,  1, 4, 'M-01',   4,  'CERRADA',        NULL),
(2,  3, 5, 'VIP-01', 18, 'CERRADA',        'Celebración cumpleaños 50 años, requirió decoración especial'),
(3,  2, 4, 'T-03',   16, 'CERRADA',        NULL),
(4,  1, 6, 'M-05',   2,  'CERRADA',        'Cliente frecuente, prefiere mesa tranquila'),
(5,  3, 5, 'VIP-02', 2,  'CERRADA',        'Aniversario, cliente pidió música romántica'),
(6,  1, 4, 'T-01',   98, 'CERRADA',        NULL),
(7,  1, 5, 'M-03',   5,  'CERRADA',        NULL),
(8,  1, 6, 'M-07',   3,  'CERRADA',        NULL),
(9,  2, 4, 'T-02',   4,  'CERRADA',        'Familia con niño pequeño, requirió silla alta'),
-- Mesas activas
(10, 1, 4, 'M-02',   2,  'ATENDIDA',       'Pareja VIP, atención preferencial'),
(11, 1, 5, 'M-04',   4,  'EN_PREPARACION', NULL),
(12, 2, 6, 'T-04',   3,  'ESPERA',         'Cliente alérgico al maní, verificar con cocina');

-- =====================================================
-- 11. Comanda (de visitas)
--     IDs resultantes: 1-17
-- =====================================================
INSERT INTO Comanda (visita_id, comanda_estacion, comanda_fecha_hora_inicio, comanda_fecha_hora_listo, comanda_notas, comanda_estado) VALUES
-- Históricas completadas
(1,  'COCINA', NOW() - INTERVAL '30 days 7 hours', NOW() - INTERVAL '30 days 6 hours',               NULL,                    'COMPLETADO'),
(1,  'BARRA',  NOW() - INTERVAL '30 days 7 hours', NOW() - INTERVAL '30 days 6 hours 30 minutes',    NULL,                    'COMPLETADO'),
(2,  'COCINA', NOW() - INTERVAL '25 days 7 hours', NOW() - INTERVAL '25 days 5 hours 30 minutes',    'Platos del cumpleaños', 'COMPLETADO'),
(2,  'BARRA',  NOW() - INTERVAL '25 days 7 hours', NOW() - INTERVAL '25 days 6 hours',               NULL,                    'COMPLETADO'),
(3,  'COCINA', NOW() - INTERVAL '20 days 8 hours', NOW() - INTERVAL '20 days 6 hours 30 minutes',    NULL,                    'COMPLETADO'),
(4,  'COCINA', NOW() - INTERVAL '15 days 7 hours', NOW() - INTERVAL '15 days 6 hours',               NULL,                    'COMPLETADO'),
(5,  'COCINA', NOW() - INTERVAL '10 days 8 hours', NOW() - INTERVAL '10 days 6 hours',               'Aniversario',           'COMPLETADO'),
(5,  'BARRA',  NOW() - INTERVAL '10 days 8 hours', NOW() - INTERVAL '10 days 7 hours',               NULL,                    'COMPLETADO'),
(6,  'COCINA', NOW() - INTERVAL '3 days 7 hours',  NOW() - INTERVAL '3 days 5 hours 30 minutes',     NULL,                    'COMPLETADO'),
(7,  'COCINA', NOW() - INTERVAL '5 days 8 hours',  NOW() - INTERVAL '5 days 6 hours 30 minutes',     NULL,                    'COMPLETADO'),
(8,  'COCINA', NOW() - INTERVAL '4 days 7 hours',  NOW() - INTERVAL '4 days 5 hours 30 minutes',     NULL,                    'COMPLETADO'),
(9,  'COCINA', NOW() - INTERVAL '2 days 8 hours',  NOW() - INTERVAL '2 days 6 hours 30 minutes',     NULL,                    'COMPLETADO'),
-- Activas
(10, 'COCINA', NOW() - INTERVAL '40 minutes', NULL,                          'Acelerar platos, cliente tiene prisa',  'EN_PREPARACION'),
(10, 'BARRA',  NOW() - INTERVAL '40 minutes', NOW() - INTERVAL '20 minutes', 'Preparar cócteles primero',              'LISTO'),
(11, 'COCINA', NOW() - INTERVAL '20 minutes', NULL,                          'Cliente alérgico al maní',               'PENDIENTE'),
(11, 'BARRA',  NOW() - INTERVAL '20 minutes', NULL,                          'Servir bebidas sin alcohol para niños',  'EN_PREPARACION'),
(12, 'BARRA',  NOW() - INTERVAL '10 minutes', NULL,                          'Cliente prefiere bebidas frías',         'PENDIENTE');

-- =====================================================
-- 8. Comandas PRE_RESERVA (pre-órdenes unificadas)
--
--    Modelo unificado: no existe PreOrden_Detalle.
--    Las pre-órdenes son Comandas con estado PRE_RESERVA,
--    reserva_id poblado y visita_id = NULL.
--    Al iniciar la visita, el servicio establece visita_id.
--
--    Reservas ATENDIDAS (2, 3, 5, 8): sus pre-órdenes ya fueron
--    incorporadas a las comandas de visita (sección 11).
--    Aquí solo se crean las PRE_RESERVA para reservas PENDIENTES
--    o CONFIRMADAS que aún no tienen visita:
--      Reserva 10 (12p, CONFIRMADA) → Menú 8b
--      Reserva 12  (2p, PENDIENTE)  → Picanha + Salmón + Gnomo
--      Reserva 14 (14p, PENDIENTE)  → Menú 8c
--      Reserva 17 (11p, PENDIENTE)  → Menú 8d
--
--    IDs de Comanda resultantes: 18, 19, 20, 21
--    (las 17 comandas de visita ocupan los IDs 1-17)
-- =====================================================

-- ─── Comandas PRE_RESERVA ────────────────────────────────────────────────────
INSERT INTO Comanda (visita_id, reserva_id, comanda_estacion, comanda_fecha_hora_inicio, comanda_notas, comanda_estado) VALUES
(NULL, 10, NULL, NOW() - INTERVAL '3 days',  NULL,                       'PRE_RESERVA'),
(NULL, 12, NULL, NOW() - INTERVAL '1 day',   'Cena romántica - velas',   'PRE_RESERVA'),
(NULL, 14, NULL, NOW(),                       NULL,                       'PRE_RESERVA'),
(NULL, 17, NULL, NOW() - INTERVAL '1 day',   'Fiesta de graduación',     'PRE_RESERVA');

-- ─── ComandaItem de las pre-órdenes ───────────────────────────────────────
-- precio capturado desde el catálogo en el momento de la reserva
INSERT INTO Comanda_Item (comanda_id, producto_id, comanda_item_cantidad, comanda_item_precio)
SELECT v.comanda_id, p.producto_id, v.cantidad, p.producto_precio
FROM (VALUES
    (18, 'Menú 8b - Pechuga y Cerdo',       12),
    (19, 'Picanha',                           2),
    (19, 'Salmón a la Marinera',              2),
    (19, 'Gnomo',                             2),
    (20, 'Menú 8c - Pechuga y Res en Vino', 14),
    (21, 'Menú 8d - Cerdo y Res en Vino',   11)
) AS v(comanda_id, nombre, cantidad)
JOIN Producto p ON p.producto_nombre = v.nombre;

-- ─── comanda_menu_modificacion para menús especiales ─────────────────────────
-- JOIN por (comanda_id + nombre_producto) → comanda_item_id sin hardcodear IDs.
--   Comanda 18 (Menú 8b): SALSA_P1=BBQ | SALSA_P2=Uchuvas | BEBIDA=Fresa
--   Comanda 20 (Menú 8c): SALSA_P1=Uchuvas | SALSA_P2=Vino Tinto | BEBIDA=Lulo
--   Comanda 21 (Menú 8d): SALSA_P1=Vino Tinto | SALSA_P2=BBQ | BEBIDA=Fresa
INSERT INTO comanda_menu_modificacion (comanda_item_id, opcion_id)
SELECT ci.comanda_item_id, o.opcion_id
FROM Comanda_Item ci
JOIN Comanda          c ON ci.comanda_id  = c.comanda_id
JOIN Producto         p ON ci.producto_id = p.producto_id
JOIN (VALUES
    (18::bigint, 'Menú 8b - Pechuga y Cerdo',       'SALSA_PROTEINA_1', 'Salsa BBQ'),
    (18,         'Menú 8b - Pechuga y Cerdo',       'SALSA_PROTEINA_2', 'Salsa de Uchuvas'),
    (18,         'Menú 8b - Pechuga y Cerdo',       'BEBIDA',           'Jugo de Fresa'),
    (20,         'Menú 8c - Pechuga y Res en Vino', 'SALSA_PROTEINA_1', 'Salsa de Uchuvas'),
    (20,         'Menú 8c - Pechuga y Res en Vino', 'SALSA_PROTEINA_2', 'Salsa de Vino Tinto'),
    (20,         'Menú 8c - Pechuga y Res en Vino', 'BEBIDA',           'Jugo de Lulo'),
    (21,         'Menú 8d - Cerdo y Res en Vino',   'SALSA_PROTEINA_1', 'Salsa de Vino Tinto'),
    (21,         'Menú 8d - Cerdo y Res en Vino',   'SALSA_PROTEINA_2', 'Salsa BBQ'),
    (21,         'Menú 8d - Cerdo y Res en Vino',   'BEBIDA',           'Jugo de Fresa')
) AS v(cmd_id, prod_nombre, tipo, opcion_nombre)
  ON c.comanda_id = v.cmd_id AND p.producto_nombre = v.prod_nombre
JOIN opcion_modificacion o
  ON o.tipo_componente = v.tipo
 AND o.opcion_nombre   = v.opcion_nombre
 AND o.opcion_estado   = 'ACTIVO';

-- =====================================================
-- 12. Comanda_Item (de visitas)
-- =====================================================
INSERT INTO Comanda_Item (comanda_id, producto_id, comanda_item_cantidad, comanda_item_precio, comanda_item_descripcion)
SELECT v.comanda_id, p.producto_id, v.cantidad, v.precio, v.descripcion
FROM (VALUES
    -- Comanda 1 (visita 1 - cocina)
    (1,  'Picanha',                1, 42000::numeric, 'Término medio'),
    (1,  'Pechuga a la Plancha',   1, 31000,          NULL),
    -- Comanda 2 (visita 1 - barra)
    (2,  'Mojito Clásico',         2, 22000,          NULL),
    (2,  'Coca-Cola',              2,  6000,          'Sin hielo'),
    -- Comanda 3 (visita 2 - cocina, cumpleaños)
    (3,  'Picada Gran Toro',       2, 70000,          'Sin chorizo'),
    (3,  'Picanha',                4, 42000,          'Término tres cuartos'),
    (3,  'Salmón a la Plancha',    2, 49000,          NULL),
    -- Comanda 4 (visita 2 - barra, cumpleaños)
    (4,  'Bambuco',                8, 22000,          NULL),
    (4,  'Cumpleaños',             4, 30000,          'Decorar con velas'),
    -- Comanda 5 (visita 3 - cocina)
    (5,  'Lomo Fino Fajón',        2, 45000,          'Término medio'),
    (5,  'Ceviche de Pescado',     2, 33000,          'Picante suave'),
    -- Comanda 6 (visita 4)
    (6,  'Hamburguesa Al Toro',    2, 25000,          'Sin tomate'),
    (6,  'Coca-Cola',              2,  6000,          NULL),
    -- Comanda 7 (visita 5 - cocina, aniversario)
    (7,  'Filet Mignon',           1, 42000,          'Jugoso'),
    (7,  'Salmón a la Marinera',   1, 57000,          NULL),
    -- Comanda 8 (visita 5 - barra, aniversario)
    (8,  'Gato Negro Tinto',       1, 65000,          'Servir a temperatura ambiente'),
    (8,  'Lambrusco Reggiano',     1, 65000,          NULL),
    -- Comanda 9 (visita 6)
    (9,  'La Taurina',             1, 46000,          'Extra picante'),
    (9,  'Rodeo Tropical',         1, 35000,          NULL),
    -- Comanda 10 (visita 7 - walk-in)
    (10, 'Hamburguesa Tropitoro',  2, 29000,          'Sin cebolla'),
    (10, 'Botella de Agua',        2,  4000,          NULL),
    -- Comanda 11 (visita 8 - walk-in)
    (11, 'Meros Nachos',           1, 32000,          'Extra queso'),
    (11, 'Tomahawk',               1, 90000,          'Término medio'),
    -- Comanda 12 (visita 9)
    (12, 'Fettuccine con Salmón',  2, 38000,          'Salsa aparte'),
    (12, 'Salmón a la Plancha',    1, 49000,          'Bien cocido'),
    -- Comanda 13 (visita 10 - cocina activa)
    (13, 'Picanha',                1, 42000,          'Término medio'),
    (13, 'Costillas BBQ',          1, 28000,          'Extra salsa BBQ'),
    -- Comanda 14 (visita 10 - barra lista)
    (14, 'Mojito Clásico',         2, 22000,          'Poco azúcar'),
    (14, 'Moscow Mule',            1, 24000,          NULL),
    -- Comanda 15 (visita 11 - cocina pendiente)
    (15, 'Picada Gran Toro',       1, 70000,          NULL),
    (15, 'La Taurina',             2, 46000,          'Término tres cuartos'),
    -- Comanda 16 (visita 11 - barra en preparación)
    (16, 'Negroni',                2, 27000,          NULL),
    (16, 'Piña Colada',            2, 22000,          'Sin alcohol (virgen)'),
    -- Comanda 17 (visita 12 - barra pendiente)
    (17, 'Gnomo',                  2, 20000,          NULL)
) AS v(comanda_id, nombre, cantidad, precio, descripcion)
JOIN Producto p ON p.producto_nombre = v.nombre;

-- =====================================================
-- 13. Notificacion
-- =====================================================
INSERT INTO Notificacion (mesa_id, empleado_id, notificacion_estado, notificacion_tipo, notificacion_fecha_hora) VALUES
-- Históricas atendidas
(1,  4,  'ATENDIDA', 'ATENCION',       NOW() - INTERVAL '30 days 7 hours'),
(1,  7,  'ATENDIDA', 'PLATOS_LISTOS',  NOW() - INTERVAL '30 days 6 hours'),
(2,  5,  'ATENDIDA', 'PLATOS_LISTOS',  NOW() - INTERVAL '25 days 5 hours 30 minutes'),
(3,  4,  'ATENDIDA', 'BEBIDAS_LISTAS', NOW() - INTERVAL '20 days 7 hours'),
(5,  5,  'ATENDIDA', 'PLATOS_LISTOS',  NOW() - INTERVAL '10 days 6 hours'),
(5,  9,  'ATENDIDA', 'BEBIDAS_LISTAS', NOW() - INTERVAL '10 days 7 hours'),
-- Activas (mesas en curso)
(10, 7,  'ACTIVA',   'PLATOS_LISTOS',  NOW() - INTERVAL '5 minutes'),
(10, 9,  'ACTIVA',   'BEBIDAS_LISTAS', NOW() - INTERVAL '18 minutes'),
(11, 6,  'ACTIVA',   'CAMBIO',         NOW() - INTERVAL '7 minutes'),
(12, 6,  'ACTIVA',   'ATENCION',       NOW() - INTERVAL '2 minutes');

-- =====================================================
-- 14. Venta (una por visita cerrada)
-- =====================================================
INSERT INTO Venta (visita_id, cajero_id, venta_fecha_hora, venta_subtotal, venta_descuento, venta_total, venta_metodo) VALUES
(1, 2, NOW() - INTERVAL '30 days 4 hours', 129000, 0,      129000, 'TARJETA'),
(2, 3, NOW() - INTERVAL '25 days 3 hours', 702000, 20000, 682000, 'TARJETA'),
(3, 2, NOW() - INTERVAL '20 days 5 hours', 156000, 0,     156000, 'TRANSFERENCIA'),
(4, 3, NOW() - INTERVAL '15 days 5 hours', 62000,  0,     62000,  'EFECTIVO'),
(5, 2, NOW() - INTERVAL '10 days 4 hours', 229000, 15000, 214000, 'TARJETA'),
(6, 3, NOW() - INTERVAL '3 days 4 hours',  81000,  0,     81000,  'OTRO'),
(7, 2, NOW() - INTERVAL '5 days 5 hours',  66000,  0,     66000,  'EFECTIVO'),
(8, 3, NOW() - INTERVAL '4 days 4 hours',  122000, 0,     122000, 'TRANSFERENCIA'),
(9, 2, NOW() - INTERVAL '2 days 5 hours',  125000, 0,     125000, 'TARJETA');

-- =====================================================
-- 15. Movimiento_Inventario
--    Insumo IDs (V2): 1=Punta de Anca, 4=Pechuga, 11=Camarón,
--    13=Salmón, 21=Papa Francesa, 48=Ron, 49=Vodka, 50=Gin
-- =====================================================

-- Ingresos de insumos (compras a proveedores)
INSERT INTO Movimiento_Inventario (empleado_id, producto_id, insumo_id, movimiento_cantidad, movimiento_tipo, movimiento_proveedor, movimiento_numero_factura, movimiento_observaciones, movimiento_fecha_hora) VALUES
(1, NULL, 1,  20.000, 'INGRESO', 'Carnes del Cauca S.A.S',   'FAC-2026-001', 'Compra semanal carnes',   NOW() - INTERVAL '7 days'),
(1, NULL, 4,  30.000, 'INGRESO', 'Avícola La Merced',         'FAC-2026-002', 'Compra pechuga semana',   NOW() - INTERVAL '7 days'),
(1, NULL, 11, 15.000, 'INGRESO', 'Pescados y Mariscos Ltda',  'FAC-2026-003', 'Mariscos semanales',      NOW() - INTERVAL '7 days'),
(1, NULL, 13, 12.000, 'INGRESO', 'Pescados y Mariscos Ltda',  'FAC-2026-003', 'Salmón fresco',           NOW() - INTERVAL '7 days'),
(1, NULL, 21, 50.000, 'INGRESO', 'Distribuidora El Campo',    'FAC-2026-004', 'Papa francesa',           NOW() - INTERVAL '7 days'),
(1, NULL, 48, 20000,  'INGRESO', 'Licores Macondo Popayán',   'FAC-2026-005', 'Ron para coctelería',     NOW() - INTERVAL '7 days'),
(1, NULL, 49, 15000,  'INGRESO', 'Licores Macondo Popayán',   'FAC-2026-005', 'Vodka para coctelería',   NOW() - INTERVAL '7 days'),
(1, NULL, 50, 12000,  'INGRESO', 'Licores Macondo Popayán',   'FAC-2026-005', 'Gin premium',             NOW() - INTERVAL '7 days'),
-- Egresos por consumo en servicio
(7, NULL, 1,   1.500, 'EGRESO',  NULL, NULL, 'Consumo servicio lunes',  NOW() - INTERVAL '4 days'),
(7, NULL, 4,   3.200, 'EGRESO',  NULL, NULL, 'Consumo pechuga semana',  NOW() - INTERVAL '4 days'),
(9, NULL, 48,  2500,  'EGRESO',  NULL, NULL, 'Consumo ron coctelería',  NOW() - INTERVAL '3 days'),
(7, NULL, 11,  2.000, 'EGRESO',  NULL, NULL, 'Consumo camarón semana',  NOW() - INTERVAL '3 days'),
-- Nuevo ingreso esta semana
(1, NULL, 1,  25.000, 'INGRESO', 'Carnes del Cauca S.A.S',   'FAC-2026-020', 'Compra semanal carnes',   NOW() - INTERVAL '1 day'),
(1, NULL, 13, 10.000, 'INGRESO', 'Pescados y Mariscos Ltda',  'FAC-2026-021', 'Salmón fresco',           NOW() - INTERVAL '1 day');

-- Ingresos de productos de venta directa (cervezas, vinos)
INSERT INTO Movimiento_Inventario (empleado_id, producto_id, insumo_id, movimiento_cantidad, movimiento_tipo, movimiento_proveedor, movimiento_numero_factura, movimiento_observaciones, movimiento_fecha_hora)
SELECT 1, p.producto_id, NULL, v.cantidad, 'INGRESO', v.proveedor, v.factura, v.obs, NOW() - INTERVAL '5 days'
FROM (VALUES
    ('Corona',            24::numeric,  'Distribuidora Bavaria', 'FAC-2026-010', 'Corona x24'),
    ('Coronita',          48,           'Distribuidora Bavaria', 'FAC-2026-010', 'Coronita x48'),
    ('Águila Light',      48,           'Distribuidora Bavaria', 'FAC-2026-010', 'Águila Light x48'),
    ('Gato Negro Tinto',  12,           'Vinova SAS',            'FAC-2026-011', 'Gato Negro Tinto'),
    ('Lambrusco Reggiano',10,           'Vinova SAS',            'FAC-2026-011', 'Lambrusco Reggiano')
) AS v(nombre, cantidad, proveedor, factura, obs)
JOIN Producto p ON p.producto_nombre = v.nombre;

-- =====================================================
-- N. Bloqueos de disponibilidad (datos de prueba)
--
-- Fechas usadas en las pruebas de Postman:
--   fechaLibre          2026-12-15  → NO bloqueada  (pruebas normales)
--   fechaCapacidad      2026-12-16  → NO bloqueada  (pruebas de capacidad)
--   fechaDecoracion     2026-12-17  → NO bloqueada  (pruebas de decoración)
--   fechaTodasOcupadas  2026-12-20  → NO bloqueada  (pruebas sin disponibilidad)
--   fechaBloqueadaDia   2026-12-25  → BLOQUEADA día completo (Navidad)
--   fechaBloqueadaFranja 2026-12-22 → BLOQUEADA 19:00–21:00 (mantenimiento)
-- =====================================================
INSERT INTO Bloque_Disponibilidad (bloque_fecha_inicio, bloque_fecha_fin, bloque_hora_inicio, bloque_hora_fin, bloque_motivo, admin_id) VALUES
-- Bloqueo 1: Navidad — día completo bloqueado
('2026-12-25', '2026-12-25', NULL, NULL,
 'Cierre por festividad de Navidad', 1),

-- Bloqueo 2: Mantenimiento de salón — solo franja 7 PM a 9 PM el 22 de diciembre
('2026-12-22', '2026-12-22', '19:00', '21:00',
 'Mantenimiento de equipos de sonido en horario nocturno', 1),

-- Bloqueo 3: Cierre por inventario de fin de año — 30 y 31 de diciembre todo el día
('2026-12-30', '2026-12-31', NULL, NULL,
 'Cierre por inventario de fin de año', 1);

-- =====================================================
-- Producto INACTIVO para pruebas (RP-PO-21)
-- ID esperado: 138 (último de V2 es 137)
-- =====================================================
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, menu_especial)
VALUES (9, 'Producto de Prueba Inactivo', 'INACTIVO', 10000, 'PREPARACION', 'PLATO', FALSE);

-- =====================================================
-- Clientes adicionales para pruebas de borde
--   sinpuntos@altoro.com   → clienteIdSinPuntos  (cliente_puntos=0)
--   sinhistorial@altoro.com → emailSinHistorial   (sin visitas ni reservas)
-- =====================================================
INSERT INTO Usuario (usuario_email, usuario_password) VALUES
('sinpuntos@altoro.com',    '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK'),
('sinhistorial@altoro.com', '$2a$12$rGT4QIRzw47iaPwfpLverejOFI4oY36WdbJu1QMZfn.CXd.JBs/CK');

INSERT INTO Cliente (usuario_id, cliente_nombre, cliente_telefono, cliente_direccion, cliente_fecha_nacimiento, cliente_puntos, cliente_puntos_acumulados, cliente_acepta_terminos, cliente_fecha_aceptacion)
SELECT u.usuario_id, 'Sin Puntos', '3001234567', 'Dirección de Prueba Popayán', '1995-01-01', 0, 0, TRUE, NOW()
FROM Usuario u WHERE u.usuario_email = 'sinpuntos@altoro.com';

INSERT INTO Cliente (usuario_id, cliente_nombre, cliente_telefono, cliente_direccion, cliente_fecha_nacimiento, cliente_puntos, cliente_puntos_acumulados, cliente_acepta_terminos, cliente_fecha_aceptacion)
SELECT u.usuario_id, 'Sin Historial', '3009876543', 'Dirección de Prueba Popayán', '1998-06-15', 500, 500, TRUE, NOW()
FROM Usuario u WHERE u.usuario_email = 'sinhistorial@altoro.com';

INSERT INTO Usuario_Rol (usuario_id, rol_nombre, rol_estado)
SELECT u.usuario_id, 'CLIENTE', 'ACTIVO'
FROM Usuario u WHERE u.usuario_email IN ('sinpuntos@altoro.com', 'sinhistorial@altoro.com');

-- =====================================================
-- DATOS DE SOPORTE PARA TESTS POSTMAN
-- =====================================================

-- Segunda decoración con costo para test MR-13
INSERT INTO Decoracion (decoracion_nombre, decoracion_estado, decoracion_costo_adicional, decoracion_imagen_url)
VALUES ('Bodas Premium', 'ACTIVO', 60000.00, 'https://picsum.photos/seed/decor-bodas/360/220');

-- Reserva CANCELADA para carlos.perez@gmail.com (test MR-08)
INSERT INTO Reserva (cliente_id, zona_id, decoracion_id, reserva_fecha_hora_llegada, reserva_numero_personas, reserva_notas, reserva_estado, reserva_tipo, reserva_fecha_creacion)
SELECT u.usuario_id, NULL, NULL, NOW() - INTERVAL '2 days', 2, NULL, 'CANCELADA', 'BASICA', NOW() - INTERVAL '3 days'
FROM Usuario u WHERE u.usuario_email = 'carlos.perez@gmail.com';

-- Reserva BASICA CONFIRMADA con anticipo para andres.morales@gmail.com (CR-10)
INSERT INTO Reserva (cliente_id, zona_id, decoracion_id, reserva_fecha_hora_llegada, reserva_numero_personas, reserva_notas, reserva_estado, reserva_tipo, reserva_fecha_creacion)
SELECT u.usuario_id, NULL, NULL, NOW() + INTERVAL '30 days', 2, NULL, 'CONFIRMADA', 'BASICA', NOW() - INTERVAL '1 day'
FROM Usuario u WHERE u.usuario_email = 'andres.morales@gmail.com';

-- Abono ANTICIPO para la reserva
INSERT INTO Abono (cajero_id, reserva_id, abono_monto, abono_fecha_hora, abono_metodo, abono_tipo)
SELECT 2, r.reserva_id, 40000, NOW() - INTERVAL '12 hours', 'TRANSFERENCIA', 'ANTICIPO'
FROM Reserva r
JOIN Usuario u ON u.usuario_id = r.cliente_id
WHERE u.usuario_email = 'andres.morales@gmail.com'
  AND r.reserva_estado = 'CONFIRMADA'
  AND r.reserva_tipo = 'BASICA'
ORDER BY r.reserva_id DESC
LIMIT 1;

-- Reserva ESPECIAL PENDIENTE con fecha pasada para andres.morales@gmail.com (CR-12)
INSERT INTO Reserva (cliente_id, zona_id, decoracion_id, reserva_fecha_hora_llegada, reserva_numero_personas, reserva_notas, reserva_estado, reserva_tipo, reserva_fecha_creacion)
SELECT u.usuario_id, NULL, NULL, NOW() - INTERVAL '1 day', 2, NULL, 'PENDIENTE', 'ESPECIAL', NOW() - INTERVAL '3 days'
FROM Usuario u WHERE u.usuario_email = 'andres.morales@gmail.com';

-- Reserva BASICA CONFIRMADA para test marcar inasistencia
INSERT INTO Reserva (cliente_id, zona_id, decoracion_id, reserva_fecha_hora_llegada, reserva_numero_personas, reserva_notas, reserva_estado, reserva_tipo, reserva_fecha_creacion)
SELECT u.usuario_id, NULL, NULL, NOW() - INTERVAL '2 hours', 3, 'Test marcar inasistencia', 'CONFIRMADA', 'BASICA', NOW() - INTERVAL '1 day'
FROM Usuario u WHERE u.usuario_email = 'andres.morales@gmail.com';
