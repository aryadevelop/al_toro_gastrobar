-- =====================================================
-- SCRIPT DE INSERCIÓN BASE DE DATOS
-- PostgreSQL 12+
-- =====================================================

SET client_encoding = 'UTF8';
SET search_path TO restaurante, public;

-- =====================================================
-- 1. CATEGORIA CARTA
-- =====================================================
INSERT INTO CategoriaCarta (categoria_nombre, orden, activo) VALUES
('ENTRADAS',             1,  TRUE),
('PARA COMPARTIR',       2,  TRUE),
('TIERRA',               3,  TRUE),
('MAR',                  4,  TRUE),
('MAR Y TIERRA',         5,  TRUE),
('PASTA',                6,  TRUE),
('RAPIDAS',              7,  TRUE),
('MENU INFANTIL',        8,  TRUE),
('ACOMPANANTES',         9,  TRUE),
('COCTELES DE LA CASA', 10,  TRUE),
('CLASICOS',            11,  TRUE),
('GIN TONIC',           12,  TRUE),
('TIKIS',               13,  TRUE),
('SANGRIA',             14,  TRUE),
('MICHELADAS',          15,  TRUE),
('TEQUILA',             16,  TRUE),
('WHISKY',              17,  TRUE),
('RON',                 18,  TRUE),
('VINO TINTO',          19,  TRUE),
('VINO ESPUMOSO',       20,  TRUE),
('VINO DULCE',          21,  TRUE),
('OTRAS BEBIDAS',       22,  TRUE),
('JUGOS NATURALES',     23,  TRUE),
('LIMONADAS',           24,  TRUE),
('SODAS ITALIANAS',     25,  TRUE),
('CERVEZA',             26,  TRUE),
('ADICIONES MICHELADA', 27,  TRUE);

-- =====================================================
-- 2. ZONA
-- =====================================================
INSERT INTO Zona (zona_nombre, zona_capacidad_personas, zona_imagen_url) VALUES
('Salón Principal',  60, 'https://picsum.photos/seed/zona-salon/360/220'),
('Terraza',          30, 'https://picsum.photos/seed/zona-terraza/360/220'),
('VIP',              16, 'https://picsum.photos/seed/zona-vip/360/220'),
('Barra',            10, 'https://picsum.photos/seed/zona-barra/360/220'),
('Jardín',           20, 'https://picsum.photos/seed/zona-jardin/360/220'),
('Sala Privada',     12, 'https://picsum.photos/seed/zona-privada/360/220'),
('Zona Romántica',    6, 'https://picsum.photos/seed/zona-romantica/360/220');

-- =====================================================
-- 3. DECORACION
-- =====================================================
INSERT INTO Decoracion (decoracion_nombre, decoracion_estado, decoracion_costo_adicional, decoracion_imagen_url) VALUES
('Velas Románticas',        'ACTIVO',   25000.00, 'https://picsum.photos/seed/decor-velas/360/220'),
('Cumpleaños Premium',      'ACTIVO',   0,        'https://picsum.photos/seed/decor-cumple/360/220'),
('Decoración Empresarial',  'ACTIVO',   0,        'https://picsum.photos/seed/decor-empresa/360/220'),
('Mesa Temática Toro',      'ACTIVO',   0,        'https://picsum.photos/seed/decor-toro/360/220'),
('Minimalista Elegante',    'ACTIVO',   0,        'https://picsum.photos/seed/decor-minimal/360/220'),
('Globos y Flores',         'ACTIVO',   0,        'https://picsum.photos/seed/decor-globos/360/220'),
('Rústica Vintage',         'INACTIVO', 0,        'https://picsum.photos/seed/decor-rustica/360/220'),
('Test Zona Fija',          'ACTIVO',   0,        NULL);

-- =====================================================
-- 4. DECORACION_ZONA
-- =====================================================
INSERT INTO Decoracion_Zona (decoracion_id, zona_id) VALUES
(1, 1),(1, 2),(1, 3),
(2, 3),(2, 6),
(3, 1),(3, 2),(3, 3),(3, 6),
(4, 3),(4, 6),
(5, 1),(5, 2),
(8, 4);

-- =====================================================
-- 5. INSUMO
-- =====================================================
INSERT INTO Insumo (insumo_nombre, insumo_unidad, insumo_stock_actual, insumo_estado) VALUES
-- Carnes
('Punta de Anca',        'KG',     18.500, 'ACTIVO'),
('Lomo Fino de Res',     'KG',     12.000, 'ACTIVO'),
('Churrasco',            'KG',     20.000, 'ACTIVO'),
('Pechuga de Pollo',     'KG',     25.000, 'ACTIVO'),
('Lomo de Cerdo',        'KG',     15.000, 'ACTIVO'),
('Costilla de Cerdo',    'KG',     22.000, 'ACTIVO'),
('Carne de Hamburguesa', 'KG',     30.000, 'ACTIVO'),
('Tocineta',             'KG',      8.000, 'ACTIVO'),
('Chorizo',              'KG',     10.000, 'ACTIVO'),
('Salchicha Americana',  'KG',      5.000, 'ACTIVO'),
-- Mariscos
('Camaron',              'KG',     12.000, 'ACTIVO'),
('Calamar Anillo',       'KG',      8.000, 'ACTIVO'),
('Salmon',               'KG',     10.000, 'ACTIVO'),
('Langostino',           'KG',      6.000, 'ACTIVO'),
('Pulpo',                'KG',      4.000, 'ACTIVO'),
('Almejas',              'KG',      3.000, 'ACTIVO'),
('Mejillones',           'KG',      3.500, 'ACTIVO'),
('Palmito de Cangrejo',  'KG',      2.000, 'ACTIVO'),
('Pescado Blanco',       'KG',      8.000, 'ACTIVO'),
-- Vegetales y frescos
('Platano Verde',        'KG',     20.000, 'ACTIVO'),
('Papa Francesa',        'KG',     40.000, 'ACTIVO'),
('Papa Criolla',         'KG',     25.000, 'ACTIVO'),
('Esparragos',           'KG',      5.000, 'ACTIVO'),
('Aguacate',             'UNIDAD',  50,    'ACTIVO'),
('Tomate Cherry',        'KG',      8.000, 'ACTIVO'),
('Lechuga',              'KG',      6.000, 'ACTIVO'),
('Cebolla Morada',       'KG',      5.000, 'ACTIVO'),
('Maiz Tierno',          'KG',     10.000, 'ACTIVO'),
('Jalapenos',            'KG',      2.000, 'ACTIVO'),
('Frijol Refrito',       'KG',      5.000, 'ACTIVO'),
-- Pasta y arroces
('Fettuccine',           'KG',      8.000, 'ACTIVO'),
('Spaghetti',            'KG',      8.000, 'ACTIVO'),
('Arroz de Risotto',     'KG',      5.000, 'ACTIVO'),
-- Lácteos y salsas
('Queso Doble Crema',    'KG',     10.000, 'ACTIVO'),
('Queso Parmesano',      'KG',      3.000, 'ACTIVO'),
('Crema de Coco',        'L',       4.000, 'ACTIVO'),
('Guacamole',            'KG',      6.000, 'ACTIVO'),
('Salsa Criolla',        'KG',      8.000, 'ACTIVO'),
('Salsa BBQ',            'KG',      5.000, 'ACTIVO'),
('Salsa Champiñones',    'KG',      4.000, 'ACTIVO'),
('Salsa Pomodoro',       'KG',      6.000, 'ACTIVO'),
('Salsa Alfredo',        'KG',      3.000, 'ACTIVO'),
('Salsa Hawaiana',       'KG',      3.000, 'ACTIVO'),
('Chimichurri',          'KG',      4.000, 'ACTIVO'),
('Salsa Strogonoff',     'KG',      3.000, 'ACTIVO'),
('Salsa Demiglace',      'KG',      2.000, 'ACTIVO'),
('Salsa Caribeña',       'KG',      4.000, 'ACTIVO'),
-- Licores (insumos para coctelería)
('Ron',                  'ML',  15000,     'ACTIVO'),
('Vodka',                'ML',  12000,     'ACTIVO'),
('Gin',                  'ML',  10000,     'ACTIVO'),
('Tequila',              'ML',   8000,     'ACTIVO'),
('Triple Sec',           'ML',   5000,     'ACTIVO'),
('Aguardiente',          'ML',   6000,     'ACTIVO'),
('Campari',              'ML',   3000,     'ACTIVO'),
('Vermouth',             'ML',   3000,     'ACTIVO'),
('Curaçao',              'ML',   4000,     'ACTIVO'),
('Brandy',               'ML',   2000,     'ACTIVO'),
('Granadina',            'ML',   3000,     'ACTIVO'),
('Ginger Beer',          'ML',   8000,     'ACTIVO'),
('Agua Tonica',          'ML',   8000,     'ACTIVO'),
('Sirup de Maracuya',    'ML',   3000,     'ACTIVO'),
('Sirup Frutos Rojos',   'ML',   2000,     'ACTIVO'),
('Sirup Manzana',        'ML',   2000,     'ACTIVO'),
-- Frutas para cócteles y jugos
('Limon',                'KG',      8.000, 'ACTIVO'),
('Hierbabuena',          'KG',      1.000, 'ACTIVO'),
('Lulo',                 'KG',      4.000, 'ACTIVO'),
('Fresa',                'KG',      3.000, 'ACTIVO'),
('Maracuya',             'KG',      4.000, 'ACTIVO'),
('Piña',                 'KG',      6.000, 'ACTIVO'),
('Mango',                'KG',      4.000, 'ACTIVO'),
-- Otros
('Tajin',                'KG',      1.000, 'ACTIVO'),
('Nachos',               'KG',      5.000, 'ACTIVO'),
('Pan',                  'UNIDAD',  80,    'ACTIVO'),
('Vino Blanco Cocina',   'ML',   5000,     'ACTIVO'),
('Leche Tigre',          'ML',   2000,     'ACTIVO'),
('Suero Costeño',        'KG',      3.000, 'ACTIVO'),
('Aceitunas',            'KG',      1.500, 'ACTIVO');

-- =====================================================
-- 6. PRODUCTO
-- =====================================================

-- ENTRADAS (cat 1) → IDs 1-6
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, menu_especial) VALUES
(1, 'Don Toston',          'ACTIVO', 16000, 'PREPARACION', 'PLATO', FALSE),
(1, 'Toston Loco',         'ACTIVO', 16000, 'PREPARACION', 'PLATO', FALSE),
(1, 'Toston Marino',       'ACTIVO', 27000, 'PREPARACION', 'PLATO', FALSE),
(1, 'Coctel de Camarones', 'ACTIVO', 26000, 'PREPARACION', 'PLATO', FALSE),
(1, 'Caribe Crunch',       'ACTIVO', 12000, 'PREPARACION', 'PLATO', FALSE),
(1, 'Apanado Caribeno',    'ACTIVO', 29000, 'PREPARACION', 'PLATO', FALSE);

-- PARA COMPARTIR (cat 2) → IDs 7-9
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, menu_especial) VALUES
(2, 'Picada Gran Toro',     'ACTIVO', 70000, 'PREPARACION', 'PLATO', FALSE),
(2, 'Meros Nachos',         'ACTIVO', 32000, 'PREPARACION', 'PLATO', FALSE),
(2, 'Pinchos Mar y Tierra', 'ACTIVO', 52000, 'PREPARACION', 'PLATO', FALSE);

-- TIERRA (cat 3) → IDs 10-23
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, menu_especial) VALUES
(3, 'Picanha',              'ACTIVO', 42000, 'PREPARACION', 'PLATO', FALSE),
(3, 'Tomahawk',             'ACTIVO', 90000, 'PREPARACION', 'PLATO', FALSE),
(3, 'T-Bone Steak',         'ACTIVO', 48000, 'PREPARACION', 'PLATO', FALSE),
(3, 'Lomo Fino Fajon',      'ACTIVO', 45000, 'PREPARACION', 'PLATO', FALSE),
(3, 'Costillas BBQ',        'ACTIVO', 28000, 'PREPARACION', 'PLATO', FALSE),
(3, 'Rodeo Tropical',       'ACTIVO', 35000, 'PREPARACION', 'PLATO', FALSE),
(3, 'Rodeo',                'ACTIVO', 33000, 'PREPARACION', 'PLATO', FALSE),
(3, 'Pechuga a la Plancha', 'ACTIVO', 31000, 'PREPARACION', 'PLATO', FALSE),
(3, 'La Taurina',           'ACTIVO', 46000, 'PREPARACION', 'PLATO', FALSE),
(3, 'Taurina Black',        'ACTIVO', 47000, 'PREPARACION', 'PLATO', FALSE),
(3, 'Filet Mignon',         'ACTIVO', 42000, 'PREPARACION', 'PLATO', FALSE),
(3, 'Churrasco',            'ACTIVO', 45000, 'PREPARACION', 'PLATO', FALSE),
(3, 'Lomo de Cerdo',        'ACTIVO', 30000, 'PREPARACION', 'PLATO', FALSE),
(3, 'Lomo BBQ',             'ACTIVO', 32000, 'PREPARACION', 'PLATO', FALSE);

-- MAR (cat 4) → IDs 24-27
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, menu_especial) VALUES
(4, 'Cazuela de Mariscos',  'ACTIVO', 41000, 'PREPARACION', 'PLATO', FALSE),
(4, 'Salmon a la Plancha',  'ACTIVO', 49000, 'PREPARACION', 'PLATO', FALSE),
(4, 'Salmon a la Marinera', 'ACTIVO', 57000, 'PREPARACION', 'PLATO', FALSE),
(4, 'Ceviche de Pescado',   'ACTIVO', 33000, 'PREPARACION', 'PLATO', FALSE);

-- MAR Y TIERRA (cat 5) → IDs 28-31
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, menu_especial) VALUES
(5, 'Arroz de Mariscos', 'ACTIVO', 32000, 'PREPARACION', 'PLATO', FALSE),
(5, 'Rodeo Marino',      'ACTIVO', 37000, 'PREPARACION', 'PLATO', FALSE),
(5, 'Risotto de Salmon', 'ACTIVO', 38000, 'PREPARACION', 'PLATO', FALSE),
(5, 'Filet Marino',      'ACTIVO', 48000, 'PREPARACION', 'PLATO', FALSE);

-- PASTA (cat 6) → IDs 32-35
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, menu_especial) VALUES
(6, 'Fettuccine de la Casa',        'ACTIVO', 38000, 'PREPARACION', 'PLATO', FALSE),
(6, 'Spaghetti al Pomodoro',        'ACTIVO', 24000, 'PREPARACION', 'PLATO', FALSE),
(6, 'Fettuccine con Salmon',        'ACTIVO', 39000, 'PREPARACION', 'PLATO', FALSE),
(6, 'Fettuccine en Salsa Alfredo',  'ACTIVO', 38000, 'PREPARACION', 'PLATO', FALSE);

-- RAPIDAS (cat 7) → IDs 36-44
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, menu_especial) VALUES
(7, 'Hamburguesa Al Toro',      'ACTIVO', 25000, 'PREPARACION', 'PLATO', FALSE),
(7, 'Hamburguesa Tropitoro',    'ACTIVO', 29000, 'PREPARACION', 'PLATO', FALSE),
(7, 'Hamburguesa Gran Toro',    'ACTIVO', 31000, 'PREPARACION', 'PLATO', FALSE),
(7, 'Hamburguesa Toro Burguer', 'ACTIVO', 31000, 'PREPARACION', 'PLATO', FALSE),
(7, 'Hamburguesa Toro Chilli',  'ACTIVO', 29000, 'PREPARACION', 'PLATO', FALSE),
(7, 'Hamburguesa Master',       'ACTIVO', 36000, 'PREPARACION', 'PLATO', FALSE),
(7, 'Perro Caliente',           'ACTIVO', 20000, 'PREPARACION', 'PLATO', FALSE),
(7, 'Mazorcada Toro',           'ACTIVO', 32000, 'PREPARACION', 'PLATO', FALSE),
(7, 'Mazorcada',                'ACTIVO', 26000, 'PREPARACION', 'PLATO', FALSE);

-- MENU INFANTIL (cat 8) → IDs 45-47
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, menu_especial) VALUES
(8, 'Nuggets de Pollo',      'ACTIVO', 15000, 'PREPARACION', 'PLATO', FALSE),
(8, 'Choripapa',             'ACTIVO', 13000, 'PREPARACION', 'PLATO', FALSE),
(8, 'Hamburguesa Ternerita', 'ACTIVO', 18000, 'PREPARACION', 'PLATO', FALSE);

-- ACOMPANANTES (cat 9) → IDs 48-54
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, menu_especial) VALUES
(9, 'Aros de Cebolla x4',  'ACTIVO',  5000, 'PREPARACION', 'PLATO',  FALSE),
(9, '180gr Papa Francesa',  'ACTIVO',  6000, 'PREPARACION', 'PLATO', FALSE),
(9, 'Papa Criolla',         'ACTIVO',  5000, 'PREPARACION', 'PLATO', FALSE),
(9, 'Ensalada Salada',      'ACTIVO',  5000, 'PREPARACION', 'PLATO',  FALSE),
(9, 'Ensalada Dulce',       'ACTIVO',  5000, 'PREPARACION', 'PLATO',  FALSE),
(9, 'Arroz Perla',          'ACTIVO',  4000, 'PREPARACION', 'PLATO',  FALSE),
(9, 'Chips de Platano',     'ACTIVO',  5000, 'PREPARACION', 'PLATO',  FALSE);

-- COCTELES DE LA CASA (cat 10) → IDs 55-57
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, menu_especial) VALUES
(10, 'Bambuco',    'ACTIVO', 22000, 'PREPARACION', 'BEBIDA', FALSE),
(10, 'Gnomo',      'ACTIVO', 20000, 'PREPARACION', 'BEBIDA', FALSE),
(10, 'Cumpleanos', 'ACTIVO', 30000, 'PREPARACION', 'BEBIDA', FALSE);

-- CLASICOS (cat 11) → IDs 58-69
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, menu_especial) VALUES
(11, 'Mojito Clasico',      'ACTIVO', 22000, 'PREPARACION', 'BEBIDA', FALSE),
(11, 'Mojito Lulo',         'ACTIVO', 22000, 'PREPARACION', 'BEBIDA', FALSE),
(11, 'Margarita Clasica',   'ACTIVO', 22000, 'PREPARACION', 'BEBIDA', FALSE),
(11, 'Moscow Mule',         'ACTIVO', 24000, 'PREPARACION', 'BEBIDA', FALSE),
(11, 'Tom Collins',         'ACTIVO', 22000, 'PREPARACION', 'BEBIDA', FALSE),
(11, 'Long Island Ice Tea', 'ACTIVO', 25000, 'PREPARACION', 'BEBIDA', FALSE),
(11, 'Negroni',             'ACTIVO', 27000, 'PREPARACION', 'BEBIDA', FALSE),
(11, 'Laguna Azul',         'ACTIVO', 22000, 'PREPARACION', 'BEBIDA', FALSE),
(11, 'Cuba Libre',          'ACTIVO', 20000, 'PREPARACION', 'BEBIDA', FALSE),
(11, 'Orgasmo',             'ACTIVO', 22000, 'PREPARACION', 'BEBIDA', FALSE),
(11, 'Dry Martini',         'ACTIVO', 28000, 'PREPARACION', 'BEBIDA', FALSE),
(11, 'Caipiroska',          'ACTIVO', 20000, 'PREPARACION', 'BEBIDA', FALSE);

-- GIN TONIC (cat 12) → IDs 70-71
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, menu_especial) VALUES
(12, 'Gin Tonic Clasico', 'ACTIVO', 22000, 'PREPARACION', 'BEBIDA', FALSE),
(12, 'Gin Tonic Love',    'ACTIVO', 22000, 'PREPARACION', 'BEBIDA', FALSE);

-- TIKIS (cat 13) → IDs 72-74
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, menu_especial) VALUES
(13, 'Piña Colada', 'ACTIVO', 22000, 'PREPARACION', 'BEBIDA', FALSE),
(13, 'Daiquiri',    'ACTIVO', 20000, 'PREPARACION', 'BEBIDA', FALSE),
(13, 'Blue Hawaii', 'ACTIVO', 20000, 'PREPARACION', 'BEBIDA', FALSE);

-- SANGRIA (cat 14) → IDs 75-76
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, menu_especial) VALUES
(14, 'Sangria Love',     'ACTIVO', 80000, 'PREPARACION', 'BEBIDA', FALSE),
(14, 'Sangria Lancelot', 'ACTIVO', 80000, 'PREPARACION', 'BEBIDA', FALSE);

-- MICHELADAS (cat 15) → IDs 77-79
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, menu_especial) VALUES
(15, 'Michelada Luna Azul', 'ACTIVO', 13000, 'PREPARACION', 'BEBIDA', FALSE),
(15, 'Michelada Monarca',   'ACTIVO', 12000, 'PREPARACION', 'BEBIDA', FALSE),
(15, 'Michelada Toro',      'ACTIVO', 16000, 'PREPARACION', 'BEBIDA', FALSE);

-- TEQUILA (cat 16) → IDs 80-86  [VENTA_DIRECTA con stock]
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, stock_actual) VALUES
(16, 'Herradura Reposado', 'ACTIVO', 380000, 'VENTA_DIRECTA', 'BEBIDA', 5),
(16, '1800 Reposado',      'ACTIVO', 350000, 'VENTA_DIRECTA', 'BEBIDA', 4),
(16, '1800 Anejo',         'ACTIVO', 380000, 'VENTA_DIRECTA', 'BEBIDA', 3),
(16, '1800 Cristalino',    'ACTIVO', 450000, 'VENTA_DIRECTA', 'BEBIDA', 2),
(16, 'Jose Cuervo',        'ACTIVO', 140000, 'VENTA_DIRECTA', 'BEBIDA', 8),
(16, 'Patron Reposado',    'ACTIVO', 480000, 'VENTA_DIRECTA', 'BEBIDA', 3),
(16, 'Don Julio 70',       'ACTIVO', 350000, 'VENTA_DIRECTA', 'BEBIDA', 4);

-- WHISKY (cat 17) → IDs 87-90  [VENTA_DIRECTA con stock]
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, stock_actual) VALUES
(17, 'Duggans Dew',      'ACTIVO',  90000, 'VENTA_DIRECTA', 'BEBIDA', 6),
(17, 'Old Parr 12 Anos', 'ACTIVO', 250000, 'VENTA_DIRECTA', 'BEBIDA', 4),
(17, 'Red Label Rojo',   'ACTIVO', 130000, 'VENTA_DIRECTA', 'BEBIDA', 7),
(17, 'Chivas Regal 12',  'ACTIVO', 230000, 'VENTA_DIRECTA', 'BEBIDA', 3);

-- RON (cat 18) → IDs 91-93  [VENTA_DIRECTA con stock]
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, stock_actual) VALUES
(18, 'Viejo Caldas 8 Anos',  'ACTIVO', 160000, 'VENTA_DIRECTA', 'BEBIDA', 5),
(18, 'Viejo Caldas 15 Anos', 'ACTIVO', 220000, 'VENTA_DIRECTA', 'BEBIDA', 3),
(18, 'Zacapa',               'ACTIVO', 220000, 'VENTA_DIRECTA', 'BEBIDA', 4);

-- VINO TINTO (cat 19) → IDs 94-99  [VENTA_DIRECTA con stock]
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, stock_actual) VALUES
(19, 'Rosaleda',                 'ACTIVO',  40000, 'VENTA_DIRECTA', 'BEBIDA', 10),
(19, 'Gato Negro Tinto',         'ACTIVO',  65000, 'VENTA_DIRECTA', 'BEBIDA',  8),
(19, 'Finca Las Moras Malbec',   'ACTIVO',  88000, 'VENTA_DIRECTA', 'BEBIDA',  6),
(19, 'Sangue de Boi',            'ACTIVO',  55000, 'VENTA_DIRECTA', 'BEBIDA',  7),
(19, 'Frontera',                 'ACTIVO',  88000, 'VENTA_DIRECTA', 'BEBIDA',  5),
(19, 'Tocornal',                 'ACTIVO',  70000, 'VENTA_DIRECTA', 'BEBIDA',  6);

-- VINO ESPUMOSO (cat 20) → ID 100  [VENTA_DIRECTA con stock]
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, stock_actual) VALUES
(20, 'Lambrusco Reggiano', 'ACTIVO', 65000, 'VENTA_DIRECTA', 'BEBIDA', 8);

-- VINO DULCE (cat 21) → IDs 101-103  [VENTA_DIRECTA con stock]
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, stock_actual) VALUES
(21, 'Cafe',                  'ACTIVO', 40000, 'VENTA_DIRECTA', 'BEBIDA', 6),
(21, 'Avocado Casa Grajales', 'ACTIVO', 55000, 'VENTA_DIRECTA', 'BEBIDA', 5),
(21, 'Gato Negro Semi Sweet', 'ACTIVO', 60000, 'VENTA_DIRECTA', 'BEBIDA', 4);

-- OTRAS BEBIDAS (cat 22) → IDs 104-106  [VENTA_DIRECTA con stock]
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, stock_actual) VALUES
(22, 'Coca-Cola',       'ACTIVO', 6000, 'VENTA_DIRECTA', 'BEBIDA', 48),
(22, 'Botella de Agua', 'ACTIVO', 4000, 'VENTA_DIRECTA', 'BEBIDA', 60),
(22, 'Bretana',         'ACTIVO', 5000, 'VENTA_DIRECTA', 'BEBIDA', 36);

-- JUGOS NATURALES (cat 23) → IDs 107-112
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, menu_especial) VALUES
(23, 'Jugo de Maracuya con Agua',  'ACTIVO',  7000, 'PREPARACION', 'BEBIDA', FALSE),
(23, 'Jugo de Maracuya con Leche', 'ACTIVO',  8000, 'PREPARACION', 'BEBIDA', FALSE),
(23, 'Jugo de Mango con Agua',     'ACTIVO',  7000, 'PREPARACION', 'BEBIDA', FALSE),
(23, 'Jugo de Mango con Leche',    'ACTIVO',  8000, 'PREPARACION', 'BEBIDA', FALSE),
(23, 'Jugo de Fresa con Agua',     'ACTIVO',  7000, 'PREPARACION', 'BEBIDA', FALSE),
(23, 'Jugo de Fresa con Leche',    'ACTIVO',  8000, 'PREPARACION', 'BEBIDA', FALSE);

-- LIMONADAS (cat 24) → IDs 113-116
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, menu_especial) VALUES
(24, 'Limonada Maracumango', 'ACTIVO', 10000, 'PREPARACION', 'BEBIDA', FALSE),
(24, 'Limonada de Coco',     'ACTIVO', 12000, 'PREPARACION', 'BEBIDA', FALSE),
(24, 'Limonada de Cereza',   'ACTIVO', 10000, 'PREPARACION', 'BEBIDA', FALSE),
(24, 'Limonada Natural',     'ACTIVO',  8000, 'PREPARACION', 'BEBIDA', FALSE);

-- SODAS ITALIANAS (cat 25) → IDs 117-121
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, menu_especial) VALUES
(25, 'Soda Temple',   'ACTIVO', 9000, 'PREPARACION', 'BEBIDA', FALSE),
(25, 'Soda Tropical', 'ACTIVO', 9000, 'PREPARACION', 'BEBIDA', FALSE),
(25, 'Soda Saru',     'ACTIVO', 9000, 'PREPARACION', 'BEBIDA', FALSE),
(25, 'Soda Be',       'ACTIVO', 9000, 'PREPARACION', 'BEBIDA', FALSE),
(25, 'Soda Citrus',   'ACTIVO', 9000, 'PREPARACION', 'BEBIDA', FALSE);

-- CERVEZA (cat 26) → IDs 122-128  [VENTA_DIRECTA con stock]
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, stock_actual) VALUES
(26, 'Corona',          'ACTIVO', 10000, 'VENTA_DIRECTA', 'BEBIDA', 24),
(26, 'Coronita',        'ACTIVO',  6000, 'VENTA_DIRECTA', 'BEBIDA', 36),
(26, 'Stella Artois',   'ACTIVO', 12000, 'VENTA_DIRECTA', 'BEBIDA', 18),
(26, 'Aguila Light',    'ACTIVO',  5000, 'VENTA_DIRECTA', 'BEBIDA', 48),
(26, 'Aguila Original', 'ACTIVO',  5000, 'VENTA_DIRECTA', 'BEBIDA', 48),
(26, 'Club Colombia',   'ACTIVO',  6000, 'VENTA_DIRECTA', 'BEBIDA', 30),
(26, 'Smirnoff',        'ACTIVO', 12000, 'VENTA_DIRECTA', 'BEBIDA', 20);

-- ADICIONES MICHELADA (cat 27) → IDs 129-131  [VENTA_DIRECTA con stock]
INSERT INTO Producto (categoriacarta_id, producto_nombre, producto_estado, producto_precio, producto_tipo, producto_categoria, stock_actual) VALUES
(27, 'Adicion Michelada', 'ACTIVO', 3000, 'VENTA_DIRECTA', 'BEBIDA', 50),
(27, 'Adicion Chelada',   'ACTIVO', 2000, 'VENTA_DIRECTA', 'BEBIDA', 50),
(27, 'Zumo de Limon',     'ACTIVO', 1000, 'VENTA_DIRECTA', 'BEBIDA', 50);

-- =====================================================
-- 7. RECETA
-- Se usa JOIN por nombre para evitar dependencia en IDs
-- autogenerados (BIGSERIAL).  El JOIN es INNER: si algún
-- nombre no coincide exactamente la fila se omite sin error.
-- Verificar filas insertadas en log post-deploy con:
--   SELECT COUNT(*) FROM Receta;  -- debe ser 80
-- =====================================================
INSERT INTO Receta (insumo_id, producto_id, receta_cantidad)
SELECT i.insumo_id, p.producto_id, r.cantidad
FROM (VALUES
    -- Picanha
    ('Punta de Anca'::text,     'Picanha'::text,              0.340::numeric(12,3)),
    ('Chimichurri',              'Picanha',                    0.050),
    ('Papa Criolla',             'Picanha',                    0.150),
    ('Aguacate',                 'Picanha',                    0.500),
    ('Tomate Cherry',            'Picanha',                    0.030),
    -- Lomo Fino Fajon
    ('Lomo Fino de Res',         'Lomo Fino Fajon',            0.200),
    ('Salsa Strogonoff',         'Lomo Fino Fajon',            0.080),
    ('Papa Criolla',             'Lomo Fino Fajon',            0.150),
    -- Costillas BBQ
    ('Costilla de Cerdo',        'Costillas BBQ',              0.350),
    ('Salsa BBQ',                'Costillas BBQ',              0.080),
    ('Papa Francesa',            'Costillas BBQ',              0.160),
    -- Rodeo
    ('Pechuga de Pollo',         'Rodeo',                      0.280),
    ('Salsa Champiñones',        'Rodeo',                      0.080),
    ('Papa Francesa',            'Rodeo',                      0.180),
    -- Pechuga a la Plancha
    ('Pechuga de Pollo',         'Pechuga a la Plancha',       0.280),
    ('Papa Francesa',            'Pechuga a la Plancha',       0.180),
    -- La Taurina
    ('Churrasco',                'La Taurina',                 0.400),
    ('Tocineta',                 'La Taurina',                 0.050),
    ('Salsa Champiñones',        'La Taurina',                 0.080),
    ('Papa Criolla',             'La Taurina',                 0.150),
    -- Taurina Black
    ('Churrasco',                'Taurina Black',              0.400),
    ('Tocineta',                 'Taurina Black',              0.060),
    ('Salsa Demiglace',          'Taurina Black',              0.080),
    ('Papa Criolla',             'Taurina Black',              0.150),
    -- Filet Mignon
    ('Lomo Fino de Res',         'Filet Mignon',               0.220),
    ('Salsa Demiglace',          'Filet Mignon',               0.060),
    ('Esparragos',               'Filet Mignon',               0.080),
    -- Lomo BBQ
    ('Lomo de Cerdo',            'Lomo BBQ',                   0.280),
    ('Salsa BBQ',                'Lomo BBQ',                   0.080),
    ('Papa Francesa',            'Lomo BBQ',                   0.160),
    -- Cazuela de Mariscos
    ('Camaron',                  'Cazuela de Mariscos',        0.100),
    ('Calamar Anillo',           'Cazuela de Mariscos',        0.080),
    ('Pulpo',                    'Cazuela de Mariscos',        0.060),
    ('Almejas',                  'Cazuela de Mariscos',        0.060),
    ('Mejillones',               'Cazuela de Mariscos',        0.060),
    ('Langostino',               'Cazuela de Mariscos',        0.060),
    -- Salmon a la Plancha
    ('Salmon',                   'Salmon a la Plancha',        0.250),
    ('Papa Criolla',             'Salmon a la Plancha',        0.120),
    -- Salmon a la Marinera
    ('Salmon',                   'Salmon a la Marinera',       0.250),
    ('Camaron',                  'Salmon a la Marinera',       0.080),
    ('Salsa Caribeña',           'Salmon a la Marinera',       0.100),
    -- Ceviche de Pescado
    ('Pescado Blanco',           'Ceviche de Pescado',         0.300),
    ('Camaron',                  'Ceviche de Pescado',         0.100),
    ('Limon',                    'Ceviche de Pescado',         0.050),
    ('Cebolla Morada',           'Ceviche de Pescado',         0.030),
    -- Arroz de Mariscos
    ('Camaron',                  'Arroz de Mariscos',          0.120),
    ('Calamar Anillo',           'Arroz de Mariscos',          0.100),
    ('Arroz de Risotto',         'Arroz de Mariscos',          0.200),
    -- Risotto de Salmon
    ('Salmon',                   'Risotto de Salmon',          0.200),
    ('Arroz de Risotto',         'Risotto de Salmon',          0.200),
    ('Queso Parmesano',          'Risotto de Salmon',          0.030),
    -- Fettuccine de la Casa
    ('Fettuccine',               'Fettuccine de la Casa',      0.170),
    ('Lomo Fino de Res',         'Fettuccine de la Casa',      0.180),
    ('Salsa Pomodoro',           'Fettuccine de la Casa',      0.100),
    ('Tomate Cherry',            'Fettuccine de la Casa',      0.030),
    -- Spaghetti al Pomodoro
    ('Spaghetti',                'Spaghetti al Pomodoro',      0.170),
    ('Salsa Pomodoro',           'Spaghetti al Pomodoro',      0.120),
    ('Queso Parmesano',          'Spaghetti al Pomodoro',      0.030),
    -- Fettuccine con Salmon
    ('Fettuccine',               'Fettuccine con Salmon',      0.170),
    ('Salmon',                   'Fettuccine con Salmon',      0.180),
    ('Salsa Alfredo',            'Fettuccine con Salmon',      0.100),
    -- Fettuccine en Salsa Alfredo
    ('Fettuccine',               'Fettuccine en Salsa Alfredo',0.170),
    ('Salsa Alfredo',            'Fettuccine en Salsa Alfredo',0.120),
    ('Queso Parmesano',          'Fettuccine en Salsa Alfredo',0.030),
    -- Hamburguesa Al Toro
    ('Carne de Hamburguesa',     'Hamburguesa Al Toro',        0.200),
    ('Queso Doble Crema',        'Hamburguesa Al Toro',        0.040),
    ('Chorizo',                  'Hamburguesa Al Toro',        0.050),
    ('Tocineta',                 'Hamburguesa Al Toro',        0.030),
    -- Hamburguesa Gran Toro
    ('Carne de Hamburguesa',     'Hamburguesa Gran Toro',      0.250),
    ('Queso Doble Crema',        'Hamburguesa Gran Toro',      0.060),
    ('Tocineta',                 'Hamburguesa Gran Toro',      0.050),
    -- Bambuco (coctelería)
    ('Aguardiente',              'Bambuco',                   60.000),
    ('Lulo',                     'Bambuco',                   60.000),
    ('Limon',                    'Bambuco',                   20.000),
    -- Mojito Clasico  (cantidades en ML para licores, KG para frutas)
    ('Ron',                      'Mojito Clasico',            60.000),
    ('Limon',                    'Mojito Clasico',             0.030),
    ('Hierbabuena',              'Mojito Clasico',             0.005),
    -- Mojito Lulo
    ('Ron',                      'Mojito Lulo',               60.000),
    ('Lulo',                     'Mojito Lulo',                0.060),
    ('Limon',                    'Mojito Lulo',                0.020),
    ('Hierbabuena',              'Mojito Lulo',                0.005),
    -- Margarita Clasica
    ('Tequila',                  'Margarita Clasica',         60.000),
    ('Triple Sec',               'Margarita Clasica',         20.000),
    ('Limon',                    'Margarita Clasica',          0.030),
    -- Moscow Mule
    ('Vodka',                    'Moscow Mule',               60.000),
    ('Ginger Beer',              'Moscow Mule',              120.000),
    ('Limon',                    'Moscow Mule',                0.020),
    -- Negroni
    ('Gin',                      'Negroni',                   30.000),
    ('Campari',                  'Negroni',                   30.000),
    ('Vermouth',                 'Negroni',                   30.000),
    -- Cuba Libre
    ('Ron',                      'Cuba Libre',                60.000),
    ('Limon',                    'Cuba Libre',                 0.015),
    -- Piña Colada
    ('Ron',                      'Piña Colada',               60.000),
    ('Crema de Coco',            'Piña Colada',               60.000),
    ('Piña',                     'Piña Colada',                0.100),
    -- Jugo de Maracuya con Agua
    ('Maracuya',                 'Jugo de Maracuya con Agua',  0.120),
    -- Jugo de Mango con Agua
    ('Mango',                    'Jugo de Mango con Agua',     0.150),
    -- Jugo de Fresa con Agua
    ('Fresa',                    'Jugo de Fresa con Agua',     0.150),
    -- Limonada Natural
    ('Limon',                    'Limonada Natural',            0.080),
    -- Limonada de Coco
    ('Limon',                    'Limonada de Coco',            0.060),
    ('Crema de Coco',            'Limonada de Coco',           60.000),
    -- Soda Temple
    ('Sirup Frutos Rojos',       'Soda Temple',               30.000),
    ('Limon',                    'Soda Temple',                0.020),
    -- Soda Tropical
    ('Sirup de Maracuya',        'Soda Tropical',             30.000),
    ('Limon',                    'Soda Tropical',              0.020)
) AS r(insumo_nombre, producto_nombre, cantidad)
JOIN Insumo   i ON i.insumo_nombre   = r.insumo_nombre
JOIN Producto p ON p.producto_nombre = r.producto_nombre;