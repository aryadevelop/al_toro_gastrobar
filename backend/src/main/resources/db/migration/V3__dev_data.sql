-- =====================================================
-- SCRIPT DE DATOS DE DESARROLLO
-- Depende de: V1__init_schema.sql, V2__seed_data.sql
-- Contraseña de todos los usuarios: Al.Toro2026!
--   (bcrypt cost=12, hash precomputado)
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
(1,  'Martha Delgado',    'Calle 5 # 8-32 Popayan',        '3124567890', '2022-01-15'),
(2,  'Ricardo Zuniga',    'Carrera 9 # 12-45 Popayan',     '3135678901', '2023-03-10'),
(3,  'Camila Ospina',     'Calle 15 # 4-67 Popayan',       '3146789012', '2023-06-01'),
(4,  'Diego Munoz',       'Av. Bolivar # 20-11 Popayan',   '3157890123', '2023-07-15'),
(5,  'Tatiana Palacios',  'Calle 8 # 16-30 Popayan',       '3168901234', '2024-01-20'),
(6,  'Felipe Solano',     'Carrera 4 # 7-89 Popayan',      '3179012345', '2024-02-10'),
(7,  'Jorge Erazo',       'Calle 25 # 9-14 Popayan',       '3180123456', '2023-08-01'),
(8,  'Luz Marina Ruiz',   'Barrio Caldono Cll 3 Popayan',  '3191234567', '2023-09-15'),
(9,  'Andres Caicedo',    'Calle 10 # 22-05 Popayan',      '3202345678', '2023-11-01'),
(10, 'Juliana Mosquera',  'Carrera 6 # 18-90 Popayan',     '3213456789', '2024-03-01');

-- =====================================================
-- 3. Cliente  (usuario_id 11-22)
-- =====================================================
INSERT INTO Cliente (usuario_id, cliente_nombre, cliente_telefono, cliente_direccion, cliente_fecha_nacimiento, cliente_puntos, cliente_acepta_terminos, cliente_fecha_aceptacion) VALUES
(11, 'Carlos Andres Perez',    '3100001111', 'Calle 3 # 5-10 Popayan',         '1990-04-12', 1200, TRUE, '2024-01-10 10:30:00'),
(12, 'Laura Valentina Gomez',  '3111112222', 'Carrera 7 # 11-22 Popayan',      '1995-07-25', 800,  TRUE, '2024-02-14 15:00:00'),
(13, 'Andres Felipe Morales',  '3122223333', 'Barrio Bolivar Popayan',          '1988-11-03', 2500, TRUE, '2023-12-01 09:00:00'),
(14, 'Sofia Elena Ramirez',    '3133334444', 'Av. Panamericana # 40-12',        '1992-03-18', 350,  TRUE, '2024-03-20 11:00:00'),
(15, 'Juan Camilo Torres',     '3144445555', 'Calle 20 # 8-55 Popayan',        '1985-09-30', 4100, TRUE, '2023-08-05 14:00:00'),
(16, 'Valentina Cruz Lemos',   '3155556666', 'Carrera 12 # 3-78 Popayan',      '1998-12-15', 150,  TRUE, '2024-04-01 16:30:00'),
(17, 'Miguel Angel Herrera',   '3166667777', 'Urb. Los Pinos Casa 12 Popayan', '1993-06-22', 900,  TRUE, '2024-01-25 12:00:00'),
(18, 'Diana Marcela Lopez',    '3177778888', 'Calle 9 # 15-43 Popayan',        '1991-02-08', 3200, TRUE, '2023-10-10 10:00:00'),
(19, 'Sergio Ivan Castillo',   '3188889999', 'Barrio El Lago Popayan',          '1987-08-14', 600,  TRUE, '2024-02-28 09:30:00'),
(20, 'Paola Andrea Rojas',     '3199990000', 'Carrera 2 # 6-88 Popayan',       '1996-05-05', 1800, TRUE, '2023-11-15 17:00:00'),
(21, 'Nicolas Esteban Vargas', '3200001122', 'Calle 14 # 10-23 Popayan',       '1994-10-28', 720,  TRUE, '2024-03-05 13:00:00'),
(22, 'Isabella Sofia Rios',    '3211112233', 'Av. Colombia # 55-10 Popayan',   '2000-01-19', 90,   TRUE, '2024-04-08 18:00:00');

-- =====================================================
-- 4. Usuario_Rol
-- =====================================================
INSERT INTO Usuario_Rol (usuario_id, rol_nombre, rol_estado) VALUES
-- Admin
(1,  'ADM',       'ACTIVO'),
-- Cajeros
(2,  'CAJERO',    'ACTIVO'),
(2,  'MESERO',    'ACTIVO'),
(2,  'BARTENDER', 'ACTIVO'),
(2,  'ADM',       'INACTIVO'),
(3,  'CAJERO',    'ACTIVO'),
-- Meseros
(4,  'MESERO',    'ACTIVO'),
(4,  'ADM',    'ACTIVO'),
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
INSERT INTO Sesion (usuario_id, sesion_token, sesion_fecha_creacion, sesion_activa) VALUES
(1,  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.admin001',   NOW() - INTERVAL '2 hours',   TRUE),
(4,  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mesero001',  NOW() - INTERVAL '1 hour',    TRUE),
(5,  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mesero002',  NOW() - INTERVAL '90 minutes',TRUE),
(9,  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.bartend001', NOW() - INTERVAL '45 minutes',TRUE),
(7,  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.cocinero01', NOW() - INTERVAL '3 hours',   TRUE),
(2,  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.cajero0001', NOW() - INTERVAL '30 minutes',TRUE),
(11, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.cliente011', NOW() - INTERVAL '5 days',    FALSE),
(12, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.cliente012', NOW() - INTERVAL '2 days',    FALSE),
(13, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.cliente013', NOW() - INTERVAL '10 hours',  FALSE),
(15, 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.cliente015', NOW() - INTERVAL '1 day',     FALSE);

-- =====================================================
-- 6. Reserva
--    Decoracion IDs (V2): 1=Velas Romanticas, 2=Cumpleanos Premium,
--    3=Decoracion Empresarial, 4=Mesa Tematica Toro, 5=Minimalista Elegante,
--    6=Globos y Flores
--    Zona IDs (V2): 1=Salon Principal, 2=Terraza, 3=VIP, 6=Sala Privada
-- =====================================================
INSERT INTO Reserva (cliente_id, zona_id, decoracion_id, reserva_fecha_hora_llegada, reserva_numero_personas, reserva_notas, reserva_estado, reserva_tipo, reserva_fecha_creacion) VALUES
-- Pasadas atendidas o canceladas
(13, 1, NULL, NOW() - INTERVAL '30 days',  4,  'Sin notas',                            'ATENDIDA',     'BASICA',   NOW() - INTERVAL '32 days'),
(15, 3, 2,    NOW() - INTERVAL '25 days',  18,  'Cumpleanos de Laura, decoracion VIP', 'ATENDIDA',     'ESPECIAL', NOW() - INTERVAL '27 days'),
(18, 2, 3,    NOW() - INTERVAL '20 days',  16,  'Reunion de negocios',                 'ATENDIDA',     'ESPECIAL', NOW() - INTERVAL '22 days'),
(11, 1, NULL, NOW() - INTERVAL '15 days',  2,  NULL,                                   'ATENDIDA',     'BASICA',   NOW() - INTERVAL '16 days'),
(20, 3, 1,    NOW() - INTERVAL '10 days',  2, 'Aniversario pareja',                    'ATENDIDA',     'ESPECIAL', NOW() - INTERVAL '12 days'),
(14, 1, NULL, NOW() - INTERVAL '7 days',   3,  NULL,                                   'CANCELADA',    'BASICA',   NOW() - INTERVAL '9 days'),
(12, 2, NULL, NOW() - INTERVAL '5 days',   5,  'Mesa exterior preferida',              'INASISTENCIA', 'BASICA',   NOW() - INTERVAL '7 days'),
(17, 3, 4,    NOW() - INTERVAL '3 days',   98,  'Decoracion toro para evento',         'ATENDIDA',     'ESPECIAL', NOW() - INTERVAL '5 days'),
-- Proximas confirmadas o pendientes
(13, 1, NULL, NOW() + INTERVAL '1 day',    4,  NULL,                                   'CONFIRMADA',   'BASICA',   NOW() - INTERVAL '2 days'),
(15, 3, 2,    NOW() + INTERVAL '2 days',   12, 'Evento corporativo con decoracion',    'CONFIRMADA',   'ESPECIAL', NOW() - INTERVAL '3 days'),
(18, 2, NULL, NOW() + INTERVAL '3 days',   6,  NULL,                                   'PENDIENTE',    'BASICA',   NOW() - INTERVAL '1 day'),
(21, 6, 1,    NOW() + INTERVAL '5 days',   2,  'Cena romantica velas',                 'PENDIENTE',    'ESPECIAL', NOW()),
(19, 1, NULL, NOW() + INTERVAL '7 days',   3,  NULL,                                   'PENDIENTE',    'BASICA',   NOW()),
(22, 2, 6,    NOW() + INTERVAL '10 days',  14,  'Primera visita del cliente',          'PENDIENTE',    'ESPECIAL', NOW()),
-- Con devolucion
(16, 3, NULL, NOW() - INTERVAL '2 days',   2,  'Cancelacion con devolucion',           'DEVUELTA',     'BASICA',   NOW() - INTERVAL '10 days'),
(13, 3, NULL, NOW() - INTERVAL '8 days',   2,  'Cancelacion con devolucion',           'DEVUELTA',     'ESPECIAL',   NOW() - INTERVAL '40 days');

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
-- Devolucion por reserva cancelada (reserva 15 = DEVUELTA)
(2, 15, 30000, NOW() - INTERVAL '2 days',   'TRANSFERENCIA', 'DEVOLUCION'),
(2, 16, 800000, NOW() - INTERVAL '8 days',   'TRANSFERENCIA', 'DEVOLUCION');

-- =====================================================
-- 8. PreOrden_Detalle 
-- =====================================================
INSERT INTO PreOrden_Detalle (reserva_id, producto_id, preorden_detalle_cantidad, preorden_detalle_nombre)
SELECT v.reserva_id, p.producto_id, v.cantidad, v.nombre
FROM (VALUES
    -- Reserva 2 (cumpleaños 8 personas)
    (2, 'Picada Gran Toro',        2),
    (2, 'Picanha',                 4),
    (2, 'Salmon a la Plancha',     2),
    (2, 'Bambuco',                 8),
    -- Reserva 10 (evento corporativo 12 personas)
    (10, 'Meros Nachos',           4),
    (10, 'Picada Gran Toro',       2),
    (10, 'Tomahawk',               6),
    (10, 'Salmon a la Plancha',    3),
    (10, 'Fettuccine de la Casa',  3),
    -- Reserva 12 (cena romantica)
    (12, 'Picanha',                2),
    (12, 'Salmon a la Marinera',   2),
    (12, 'Gnomo',                  2)
) AS v(reserva_id, nombre, cantidad)
JOIN Producto p ON p.producto_nombre = v.nombre;

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
INSERT INTO Mesa (visita_id, zona_id, mesero_id, mesa_identificador, mesa_numero_personas, mesa_estado) VALUES
-- Mesas de visitas historicas (CERRADA)
(1,  1, 4, 'M-01',   4,  'CERRADA'),
(2,  3, 5, 'VIP-01', 8,  'CERRADA'),
(3,  2, 4, 'T-03',   6,  'CERRADA'),
(4,  1, 6, 'M-05',   2,  'CERRADA'),
(5,  3, 5, 'VIP-02', 10, 'CERRADA'),
(6,  2, 4, 'T-01',   4,  'CERRADA'),
(7,  1, 5, 'M-03',   5,  'CERRADA'),
(8,  1, 6, 'M-07',   3,  'CERRADA'),
(9,  2, 4, 'T-02',   4,  'CERRADA'),
-- Mesas activas
(10, 1, 4, 'M-02',   2,  'ATENDIDA'),
(11, 1, 5, 'M-04',   4,  'EN_PREPARACION'),
(12, 2, 6, 'T-04',   3,  'ESPERA');

-- =====================================================
-- 11. Comanda
-- =====================================================
INSERT INTO Comanda (visita_id, comanda_estacion, comanda_fecha_hora_inicio, comanda_fecha_hora_listo, comanda_notas, comanda_estado) VALUES
-- Historicas completadas
(1,  'COCINA', NOW() - INTERVAL '30 days 7 hours', NOW() - INTERVAL '30 days 6 hours',               NULL,                   'COMPLETADO'),
(1,  'BARRA',  NOW() - INTERVAL '30 days 7 hours', NOW() - INTERVAL '30 days 6 hours 30 minutes',    NULL,                   'COMPLETADO'),
(2,  'COCINA', NOW() - INTERVAL '25 days 7 hours', NOW() - INTERVAL '25 days 5 hours 30 minutes',    'Platos del cumpleanos', 'COMPLETADO'),
(2,  'BARRA',  NOW() - INTERVAL '25 days 7 hours', NOW() - INTERVAL '25 days 6 hours',               NULL,                   'COMPLETADO'),
(3,  'COCINA', NOW() - INTERVAL '20 days 8 hours', NOW() - INTERVAL '20 days 6 hours 30 minutes',    NULL,                   'COMPLETADO'),
(4,  'COCINA', NOW() - INTERVAL '15 days 7 hours', NOW() - INTERVAL '15 days 6 hours',               NULL,                   'COMPLETADO'),
(5,  'COCINA', NOW() - INTERVAL '10 days 8 hours', NOW() - INTERVAL '10 days 6 hours',               'Aniversario',          'COMPLETADO'),
(5,  'BARRA',  NOW() - INTERVAL '10 days 8 hours', NOW() - INTERVAL '10 days 7 hours',               NULL,                   'COMPLETADO'),
(6,  'COCINA', NOW() - INTERVAL '3 days 7 hours',  NOW() - INTERVAL '3 days 5 hours 30 minutes',     NULL,                   'COMPLETADO'),
(7,  'COCINA', NOW() - INTERVAL '5 days 8 hours',  NOW() - INTERVAL '5 days 6 hours 30 minutes',     NULL,                   'COMPLETADO'),
(8,  'COCINA', NOW() - INTERVAL '4 days 7 hours',  NOW() - INTERVAL '4 days 5 hours 30 minutes',     NULL,                   'COMPLETADO'),
(9,  'COCINA', NOW() - INTERVAL '2 days 8 hours',  NOW() - INTERVAL '2 days 6 hours 30 minutes',     NULL,                   'COMPLETADO'),
-- Activas
(10, 'COCINA', NOW() - INTERVAL '40 minutes', NULL,                          NULL,                   'EN_PREPARACION'),
(10, 'BARRA',  NOW() - INTERVAL '40 minutes', NOW() - INTERVAL '20 minutes', NULL,                   'LISTO'),
(11, 'COCINA', NOW() - INTERVAL '20 minutes', NULL,                          'Sin sal en las papas', 'PENDIENTE'),
(11, 'BARRA',  NOW() - INTERVAL '20 minutes', NULL,                          NULL,                   'EN_PREPARACION'),
(12, 'BARRA',  NOW() - INTERVAL '10 minutes', NULL,                          NULL,                   'PENDIENTE');

-- =====================================================
-- 12. Comanda_Detalle
-- =====================================================
INSERT INTO Comanda_Detalle (comanda_id, producto_id, comanda_detalle_cantidad, comanda_detalle_precio, comanda_detalle_nombre)
SELECT v.comanda_id, p.producto_id, v.cantidad, v.precio, v.nombre
FROM (VALUES
    -- Comanda 1 (visita 1 - cocina)
    (1,  'Picanha',                1, 42000::numeric),
    (1,  'Pechuga a la Plancha',   1, 31000),
    -- Comanda 2 (visita 1 - barra)
    (2,  'Mojito Clasico',         2, 22000),
    (2,  'Coca-Cola',              2,  6000),
    -- Comanda 3 (visita 2 - cocina, cumpleaños)
    (3,  'Picada Gran Toro',       2, 70000),
    (3,  'Picanha',                4, 42000),
    (3,  'Salmon a la Plancha',    2, 49000),
    -- Comanda 4 (visita 2 - barra, cumpleaños)
    (4,  'Bambuco',                8, 22000),
    (4,  'Cumpleanos',             4, 30000),
    -- Comanda 5 (visita 3 - cocina)
    (5,  'Lomo Fino Fajon',        2, 45000),
    (5,  'Ceviche de Pescado',     2, 33000),
    -- Comanda 6 (visita 4)
    (6,  'Hamburguesa Al Toro',    2, 25000),
    (6,  'Coca-Cola',              2,  6000),
    -- Comanda 7 (visita 5 - cocina, aniversario)
    (7,  'Filet Mignon',           1, 42000),
    (7,  'Salmon a la Marinera',   1, 57000),
    -- Comanda 8 (visita 5 - barra, aniversario)
    (8,  'Gato Negro Tinto',       1, 65000),
    (8,  'Lambrusco Reggiano',     1, 65000),
    -- Comanda 9 (visita 6)
    (9,  'La Taurina',             1, 46000),
    (9,  'Rodeo Tropical',         1, 35000),
    -- Comanda 10 (visita 7 - walk-in)
    (10, 'Hamburguesa Tropitoro',  2, 29000),
    (10, 'Botella de Agua',        2,  4000),
    -- Comanda 11 (visita 8 - walk-in)
    (11, 'Meros Nachos',           1, 32000),
    (11, 'Tomahawk',               1, 90000),
    -- Comanda 12 (visita 9)
    (12, 'Fettuccine con Salmon',  2, 38000),
    (12, 'Salmon a la Plancha',    1, 49000),
    -- Comanda 13 (visita 10 - cocina activa)
    (13, 'Picanha',                1, 42000),
    (13, 'Costillas BBQ',          1, 28000),
    -- Comanda 14 (visita 10 - barra lista)
    (14, 'Mojito Clasico',         2, 22000),
    (14, 'Moscow Mule',            1, 24000),
    -- Comanda 15 (visita 11 - cocina pendiente)
    (15, 'Picada Gran Toro',       1, 70000),
    (15, 'La Taurina',             2, 46000),
    -- Comanda 16 (visita 11 - barra en preparacion)
    (16, 'Negroni',                2, 27000),
    (16, 'Piña Colada',            2, 22000),
    -- Comanda 17 (visita 12 - barra pendiente)
    (17, 'Gnomo',                  2, 20000)
) AS v(comanda_id, nombre, cantidad, precio)
JOIN Producto p ON p.producto_nombre = v.nombre;

-- =====================================================
-- 13. Notificacion
-- =====================================================
INSERT INTO Notificacion (mesa_id, empleado_id, notificacion_estado, notificacion_tipo, notificacion_fecha_hora) VALUES
-- Historicas atendidas
(1,  4,  'ATENDIDA', 'ATENCION',       NOW() - INTERVAL '30 days 7 hours'),
(1,  7,  'ATENDIDA', 'PLATOS_LISTOS',  NOW() - INTERVAL '30 days 6 hours'),
(2,  5,  'ATENDIDA', 'PLATOS_LISTOS',  NOW() - INTERVAL '25 days 5 hours 30 minutes'),
(3,  4,  'ATENDIDA', 'BEBIDAS_LISTAS', NOW() - INTERVAL '20 days 7 hours'),
(5,  5,  'ATENDIDA', 'PLATOS_LISTOS',  NOW() - INTERVAL '10 days 6 hours'),
(5,  9,  'ATENDIDA', 'BEBIDAS_LISTAS', NOW() - INTERVAL '10 days 7 hours'),
-- Activas (mesas en curso)
(10, 7,  'ACTIVA',   'PLATOS_LISTOS',  NOW() - INTERVAL '5 minutes'),
(10, 9,  'ACTIVA',   'BEBIDAS_LISTAS', NOW() - INTERVAL '18 minutes'),
(11, 4,  'ACTIVA',   'ATENCION',       NOW() - INTERVAL '3 minutes'),
(12, 6,  'ACTIVA',   'ATENCION',       NOW() - INTERVAL '2 minutes');

-- =====================================================
-- 14. Venta (una por visita cerrada)
-- =====================================================
INSERT INTO Venta (visita_id, cajero_id, venta_fecha_hora, venta_subtotal, venta_descuento, venta_total, venta_metodo) VALUES
(1, 2, NOW() - INTERVAL '30 days 4 hours', 95000,  0,     95000,  'TARJETA'),
(2, 3, NOW() - INTERVAL '25 days 3 hours', 420000, 20000, 400000, 'TARJETA'),
(3, 2, NOW() - INTERVAL '20 days 5 hours', 156000, 0,     156000, 'TRANSFERENCIA'),
(4, 3, NOW() - INTERVAL '15 days 5 hours', 62000,  0,     62000,  'EFECTIVO'),
(5, 2, NOW() - INTERVAL '10 days 4 hours', 315000, 15000, 300000, 'TARJETA'),
(6, 3, NOW() - INTERVAL '3 days 4 hours',  118000, 0,     118000, 'EFECTIVO'),
(7, 2, NOW() - INTERVAL '5 days 5 hours',  80000,  0,     80000,  'EFECTIVO'),
(8, 3, NOW() - INTERVAL '4 days 4 hours',  212000, 0,     212000, 'TRANSFERENCIA'),
(9, 2, NOW() - INTERVAL '2 days 5 hours',  98000,  0,     98000,  'TARJETA');

-- =====================================================
-- 15. Movimiento_Inventario
--    Insumo IDs (V2): 1=Punta de Anca, 4=Pechuga, 11=Camaron,
--    13=Salmon, 21=Papa Francesa, 48=Ron, 49=Vodka, 50=Gin
-- =====================================================

-- Ingresos de insumos (compras a proveedores)
INSERT INTO Movimiento_Inventario (empleado_id, producto_id, insumo_id, movimiento_cantidad, movimiento_tipo, movimiento_proveedor, movimiento_numero_factura, movimiento_observaciones, movimiento_fecha_hora) VALUES
(1, NULL, 1,  20.000, 'INGRESO', 'Carnes del Cauca S.A.S',   'FAC-2026-001', 'Compra semanal carnes',   NOW() - INTERVAL '7 days'),
(1, NULL, 4,  30.000, 'INGRESO', 'Avicola La Merced',         'FAC-2026-002', 'Compra pechuga semana',   NOW() - INTERVAL '7 days'),
(1, NULL, 11, 15.000, 'INGRESO', 'Pescados y Mariscos Ltda',  'FAC-2026-003', 'Mariscos semanales',      NOW() - INTERVAL '7 days'),
(1, NULL, 13, 12.000, 'INGRESO', 'Pescados y Mariscos Ltda',  'FAC-2026-003', 'Salmon fresco',           NOW() - INTERVAL '7 days'),
(1, NULL, 21, 50.000, 'INGRESO', 'Distribuidora El Campo',    'FAC-2026-004', 'Papa francesa',           NOW() - INTERVAL '7 days'),
(1, NULL, 48, 20000,  'INGRESO', 'Licores Macondo Popayan',   'FAC-2026-005', 'Ron para cocteleria',     NOW() - INTERVAL '7 days'),
(1, NULL, 49, 15000,  'INGRESO', 'Licores Macondo Popayan',   'FAC-2026-005', 'Vodka para cocteleria',   NOW() - INTERVAL '7 days'),
(1, NULL, 50, 12000,  'INGRESO', 'Licores Macondo Popayan',   'FAC-2026-005', 'Gin premium',             NOW() - INTERVAL '7 days'),
-- Egresos por consumo en servicio
(7, NULL, 1,   1.500, 'EGRESO',  NULL, NULL, 'Consumo servicio lunes',  NOW() - INTERVAL '4 days'),
(7, NULL, 4,   3.200, 'EGRESO',  NULL, NULL, 'Consumo pechuga semana',  NOW() - INTERVAL '4 days'),
(9, NULL, 48,  2500,  'EGRESO',  NULL, NULL, 'Consumo ron cocteleria',  NOW() - INTERVAL '3 days'),
(7, NULL, 11,  2.000, 'EGRESO',  NULL, NULL, 'Consumo camaron semana',  NOW() - INTERVAL '3 days'),
-- Nuevo ingreso esta semana
(1, NULL, 1,  25.000, 'INGRESO', 'Carnes del Cauca S.A.S',   'FAC-2026-020', 'Compra semanal carnes',   NOW() - INTERVAL '1 day'),
(1, NULL, 13, 10.000, 'INGRESO', 'Pescados y Mariscos Ltda',  'FAC-2026-021', 'Salmon fresco',           NOW() - INTERVAL '1 day');

-- Ingresos de productos de venta directa (cervezas, vinos)
INSERT INTO Movimiento_Inventario (empleado_id, producto_id, insumo_id, movimiento_cantidad, movimiento_tipo, movimiento_proveedor, movimiento_numero_factura, movimiento_observaciones, movimiento_fecha_hora)
SELECT 1, p.producto_id, NULL, v.cantidad, 'INGRESO', v.proveedor, v.factura, v.obs, NOW() - INTERVAL '5 days'
FROM (VALUES
    ('Corona',            24::numeric,  'Distribuidora Bavaria', 'FAC-2026-010', 'Corona x24'),
    ('Coronita',          48,           'Distribuidora Bavaria', 'FAC-2026-010', 'Coronita x48'),
    ('Aguila Light',      48,           'Distribuidora Bavaria', 'FAC-2026-010', 'Aguila Light x48'),
    ('Gato Negro Tinto',  12,           'Vinova SAS',            'FAC-2026-011', 'Gato Negro Tinto'),
    ('Lambrusco Reggiano',10,           'Vinova SAS',            'FAC-2026-011', 'Lambrusco Reggiano')
) AS v(nombre, cantidad, proveedor, factura, obs)
JOIN Producto p ON p.producto_nombre = v.nombre;

