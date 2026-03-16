USE nanosland_db;

-- ==========================================
-- SCRIPT DE INSERCIÓN DE DATOS DE PRUEBA
-- ==========================================
-- Nota: Asegúrate de vaciar las tablas con TRUNCATE TABLE si 
-- ya tienes datos para evitar que los IDs numéricos se desfasen.

-- 1. Insertamos un par de Clientes
INSERT INTO clientes (nombre, telefono, correo) VALUES
('Juan Pérez Gómez', '6441234567', 'juan.perez@email.com'),
('María García López', '6449876543', 'maria.gl@email.com'),
('Carlos Valenzuela', '6445558899', NULL);

-- 2. Insertamos el catálogo de Servicios 
-- (Pongo imagen a NULL temporalmente)
INSERT INTO servicios (nombre, descripcion, precio, imagen) VALUES
('Show Payasitas Nanos', 'Show interactivo de 1 hora con 2 payasitas', 1500.00, NULL),
('Brincolin Acuático', 'Alquiler de brincolin inflable con agua por 4 horas', 2000.00, NULL),
('Pintacaritas VIP', 'Maquillaje infantil con diseños especiales de fantasía', 800.00, NULL),
('Mesa de Snacks', 'Mesa de dulces, botanas y papas para 50 personas', 2500.00, NULL),
('Decoración Arco de Globos', 'Arco y centros de mesa básicos', 1200.00, NULL);

-- 3. Creamos los Paquetes Base
-- El "costo" define el precio de partida antes de agregar extras.
INSERT INTO paquetes (nombre, descripcion, costo) VALUES
('Paquete Básico', 'La opción rápida y sencilla. Renta de salón y decoración estándar.', 3000.00),
('Paquete NanosLand', 'Nuestro paquete estrella con show en vivo, perfecto para grandes fiestas.', 5500.00),
('Paquete Premium Acuático', 'Especial para el verano, trae diversión de agua e snacks.', 8500.00);

-- 4. Relacionamos Paquetes con Servicios (Tabla Intermedia)
-- Se toma en cuenta que el ID de Paquete 1 es 'Paquete Básico', ID 2 es 'NanosLand', etc.
-- Y el ID de Servicio 1 es 'Show Payasitas', ID 5 es 'Decoración Arco...'

-- El "Paquete Básico" (ID 1) incluye Decoración(5) y Pintacaritas(3)
INSERT INTO paquetes_servicios (paquete_id, servicio_id, cantidad, subtotal) VALUES
(1, 5, 1, 1200.00),
(1, 3, 1, 800.00);

-- El "Paquete NanosLand" (ID 2) incluye Show Payasitas(1), Mesa Snacks(4) y Pintacaritas(3)
INSERT INTO paquetes_servicios (paquete_id, servicio_id, cantidad, subtotal) VALUES
(2, 1, 1, 1500.00),
(2, 4, 1, 2500.00),
(2, 3, 1, 800.00);

-- El "Paquete Premium Acuático" (ID 3) incluye Brincolin(2), Mesa Snacks(4) y Decoracion Doble(5)
INSERT INTO paquetes_servicios (paquete_id, servicio_id, cantidad, subtotal) VALUES
(3, 2, 1, 2000.00),
(3, 4, 1, 2500.00),
(3, 5, 2, 2400.00); -- Aquí ponemos 2 decoraciones y el subtotal es doble.
