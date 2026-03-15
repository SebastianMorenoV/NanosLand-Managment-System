-- =====================================================
-- NanosLand - Datos iniciales: Servicios y Paquetes
-- Base de datos: nanosland_db (MySQL)
-- =====================================================

-- ─────────────────────────────────────────────────────
-- 1. SERVICIOS
-- ─────────────────────────────────────────────────────
INSERT INTO servicios (nombre, descripcion, precio) VALUES
('Renta de mobiliario básico',   'Mesas y sillas para 20 personas',                      500.00),
('Renta de mobiliario premium',  'Mesas, sillas acolchadas, manteles y centros de mesa',  900.00),
('Brincolín grande',             'Brincolín inflable de 4x4 metros',                      800.00),
('Brincolín pequeño',            'Brincolín inflable de 2x2 metros',                      450.00),
('Piñata',                       'Piñata personalizada con dulces incluidos',              350.00),
('Pastel 30 personas',           'Pastel decorado para 30 personas',                       600.00),
('Pastel 50 personas',           'Pastel decorado para 50 personas',                       950.00),
('DJ y sonido',                  'DJ profesional con equipo de sonido por 4 horas',       1500.00),
('Música en vivo',               'Grupo musical en vivo por 2 horas',                     2500.00),
('Show de payaso',               'Animación con payaso por 2 horas',                       700.00),
('Show de magia',                'Espectáculo de magia interactivo (1 hora)',               800.00),
('Luces neón / UV',              'Iluminación neón y ultravioleta para la pista',           600.00),
('Pista iluminada',              'Pista de baile con luces LED',                           1200.00),
('Máquina de humo',              'Efecto de humo para pista de baile',                     300.00),
('Mesa de dulces',               'Mesa de dulces decorada para 30 personas',               750.00),
('Fotografía profesional',       'Fotógrafo profesional por 3 horas (100 fotos editadas)', 1800.00),
('Cabina de fotos',              'Cabina de fotos con props e impresión instantánea',      1200.00),
('Decoración con globos',        'Arco y bouquets de globos temáticos',                    500.00),
('Decoración temática completa', 'Decoración completa según temática elegida',             1500.00),
('Renta de proyector',           'Proyector y pantalla para videos o karaoke',              400.00);


-- ─────────────────────────────────────────────────────
-- 2. PAQUETES
-- ─────────────────────────────────────────────────────
INSERT INTO paquetes (nombre, descripcion, costoBase) VALUES
('Básico',          '4 Horas • Mobiliario básico • Brincolín pequeño',                              2500.00),
('Fiesta Divertida','5 Horas • Mobiliario básico • Brincolín grande • Piñata • Show de payaso',     4500.00),
('Fiesta Total',    '5 Horas • Mobiliario premium • Piñata • Pastel 30 personas • DJ',              6500.00),
('Mega Neón',       '6 Horas • Mobiliario premium • DJ • Luces neón • Pista iluminada • Humo',      8500.00),
('Premium',         '7 Horas • Todo incluido: Decoración temática • DJ • Fotografía • Mesa dulces',12000.00);


-- ─────────────────────────────────────────────────────
-- 3. PAQUETES_SERVICIOS (relación muchos a muchos)
--    Asume que los IDs se generaron secuencialmente
--    Servicios: 1-20,  Paquetes: 1-5
-- ─────────────────────────────────────────────────────

-- Paquete 1: Básico (costo_base = 2500)
--   Mobiliario básico (500) + Brincolín pequeño (450)
INSERT INTO paquetes_servicios (paquete_id, servicio_id, cantidad, subtotal) VALUES
(1, 1,  1,  500.00),   -- Renta de mobiliario básico
(1, 4,  1,  450.00);   -- Brincolín pequeño

-- Paquete 2: Fiesta Divertida (costo_base = 4500)
--   Mobiliario básico + Brincolín grande + Piñata + Show de payaso
INSERT INTO paquetes_servicios (paquete_id, servicio_id, cantidad, subtotal) VALUES
(2, 1,  1,  500.00),   -- Renta de mobiliario básico
(2, 3,  1,  800.00),   -- Brincolín grande
(2, 5,  1,  350.00),   -- Piñata
(2, 10, 1,  700.00);   -- Show de payaso

-- Paquete 3: Fiesta Total (costo_base = 6500)
--   Mobiliario premium + Piñata + Pastel 30p + DJ
INSERT INTO paquetes_servicios (paquete_id, servicio_id, cantidad, subtotal) VALUES
(3, 2,  1,  900.00),   -- Renta de mobiliario premium
(3, 5,  1,  350.00),   -- Piñata
(3, 6,  1,  600.00),   -- Pastel 30 personas
(3, 8,  1, 1500.00);   -- DJ y sonido

-- Paquete 4: Mega Neón (costo_base = 8500)
--   Mobiliario premium + DJ + Luces neón + Pista iluminada + Máquina de humo
INSERT INTO paquetes_servicios (paquete_id, servicio_id, cantidad, subtotal) VALUES
(4, 2,  1,  900.00),   -- Renta de mobiliario premium
(4, 8,  1, 1500.00),   -- DJ y sonido
(4, 12, 1,  600.00),   -- Luces neón / UV
(4, 13, 1, 1200.00),   -- Pista iluminada
(4, 14, 1,  300.00);   -- Máquina de humo

-- Paquete 5: Premium (costo_base = 12000)
--   Mobiliario premium + Pastel 50p + DJ + Decoración temática + Fotografía + Mesa dulces
INSERT INTO paquetes_servicios (paquete_id, servicio_id, cantidad, subtotal) VALUES
(5, 2,  1,  900.00),   -- Renta de mobiliario premium
(5, 7,  1,  950.00),   -- Pastel 50 personas
(5, 8,  1, 1500.00),   -- DJ y sonido
(5, 16, 1, 1800.00),   -- Fotografía profesional
(5, 15, 1,  750.00),   -- Mesa de dulces
(5, 19, 1, 1500.00);   -- Decoración temática completa


-- ─────────────────────────────────────────────────────
-- 4. CLIENTES (datos de prueba)
-- ─────────────────────────────────────────────────────
INSERT INTO clientes (nombre, telefono, correo) VALUES
('Juan Pérez',       '6441234567', 'juan.perez@email.com'),
('María García',     '6445678901', 'maria.garcia@email.com'),
('Carlos López',     '6449012345', 'carlos.lopez@email.com'),
('Ana Martínez',     '6443456789', 'ana.martinez@email.com'),
('Empresa XYZ S.A.', '6447890123', 'contacto@empresaxyz.com');
