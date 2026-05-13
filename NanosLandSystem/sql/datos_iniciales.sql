USE nanosland_db;

-- ==========================================
-- SCRIPT DE INSERCIÓN DE DATOS SEMILLA
-- ==========================================
-- Se recomienda vaciar las tablas antes de ejecutar este script
-- en caso de que ya contengan datos, o usar hibernate ddl-auto=create.

-- 1. Usuarios
INSERT INTO usuarios (correo, contrasena, telefono, rol) VALUES
('dueno@nanosland.com', 'dueno123', '6441112233', 'DUEÑO'),
('admin@nanosland.com', 'admin123', '6442223344', 'ADMINISTRADOR');

-- 2. Clientes (Con los nuevos campos de dirección)
INSERT INTO clientes (nombre, telefono, correo, calle, colonia, ciudad, codigo_postal) VALUES
('Juan Pérez Gómez', '6441234567', 'juan.perez@email.com', 'Calle 5 de Febrero', 'Centro', 'Ciudad Obregón', '85000'),
('María García López', '6449876543', 'maria.gl@email.com', 'Kino 123', 'Las Haciendas', 'Ciudad Obregón', '85050'),
('Carlos Valenzuela', '6445558899', NULL, 'Tabasco 456', 'Campestre', 'Ciudad Obregón', '85160'),
('Ana Sofía Bernal', '6447779900', 'ana.bernal@mail.com', 'Sinaloa 789', 'Miravalle', 'Ciudad Obregón', '85200');

-- 3. Servicios (Catálogo)
INSERT INTO servicios (nombre, descripcion, precio, imagen) VALUES
('Show Payasitas Nanos', 'Show interactivo de 1 hora con 2 payasitas', 1500.00, NULL),
('Brincolin Acuático', 'Alquiler de brincolin inflable con agua por 4 horas', 2000.00, NULL),
('Pintacaritas VIP', 'Maquillaje infantil con diseños especiales de fantasía', 800.00, NULL),
('Mesa de Snacks', 'Mesa de dulces, botanas y papas para 50 personas', 2500.00, NULL),
('Decoración Arco de Globos', 'Arco y centros de mesa básicos', 1200.00, NULL);

-- 4. Paquetes (Con el nuevo campo activo)
INSERT INTO paquetes (nombre, descripcion, costo, activo) VALUES
('Paquete Básico', 'La opción rápida y sencilla. Renta de salón y decoración estándar.', 3000.00, true),
('Paquete NanosLand', 'Nuestro paquete estrella con show en vivo, perfecto para grandes fiestas.', 5500.00, true),
('Paquete Premium Acuático', 'Especial para el verano, trae diversión de agua e snacks.', 8500.00, true),
('Paquete Invierno (Eliminado)', 'Paquete de temporada, ya no está disponible.', 4000.00, false);

-- 5. Paquetes - Servicios (Tabla Intermedia)
-- Paquete Básico (1) -> Decoracion(5) y Pintacaritas(3)
INSERT INTO paquetes_servicios (paquete_id, servicio_id, cantidad, subtotal) VALUES
(1, 5, 1, 1200.00),
(1, 3, 1, 800.00);

-- Paquete NanosLand (2) -> Show(1), Snacks(4), Pintacaritas(3)
INSERT INTO paquetes_servicios (paquete_id, servicio_id, cantidad, subtotal) VALUES
(2, 1, 1, 1500.00),
(2, 4, 1, 2500.00),
(2, 3, 1, 800.00);

-- Paquete Premium Acuático (3) -> Brincolin(2), Snacks(4), Decoracion x2 (5)
INSERT INTO paquetes_servicios (paquete_id, servicio_id, cantidad, subtotal) VALUES
(3, 2, 1, 2000.00),
(3, 4, 1, 2500.00),
(3, 5, 2, 2400.00);

-- 6. Cotizaciones
INSERT INTO cotizaciones (folio, fecha, cliente_id, nombre_festejado, total, notas, estado, turno, usuario_id, paquete_id, tematica) VALUES
('COT-001', '2026-04-01', 1, 'Juanito', 5000.00, 'Cotización inicial pagada', 'VIGENTE', 'MATUTINO', 1, 1, 'Superhéroes'),
('COT-002', '2026-04-05', 2, 'María Jr.', 8800.00, 'Cotización confirmada y pagada', 'VIGENTE', 'VESPERTINO', 2, 2, 'Princesas'),
('COT-003', '2026-04-10', 3, 'Carlitos', 12900.00, 'Evento grande', 'VIGENTE', 'MATUTINO', 1, 3, 'Dinosaurios'),
('COT-004', '2026-04-12', 4, 'Sofía', 3000.00, 'Cliente aún no se decide', 'BORRADOR', 'VESPERTINO', 2, 1, 'Unicornios'),
('COT-005', '2026-04-15', 1, 'Pedro', 8500.00, 'Bautizo cancelado', 'CANCELADA', 'MATUTINO', 1, 3, 'Clásico');

-- 7. Detalles de Cotización (Servicios extra agregados a la cotización por encima del paquete)
INSERT INTO detalles_cotizacion (cotizacion_id, servicio_id, cantidad, precio_unitario, subtotal, hora_sugerida, especificaciones_cliente, desglose_opciones, ubicacion_montaje) VALUES
(2, 5, 1, 1200.00, 1200.00, '16:00:00', 'Decoración extra en la entrada', NULL, 'Entrada principal'),
(3, 1, 1, 1500.00, 1500.00, '12:00:00', 'Payasitas para romper la piñata', NULL, 'Centro del salón');

-- 8. Eventos (Asociados a las cotizaciones VIGENTES)
INSERT INTO eventos (fecha, hora_inicio, hora_fin, turno, notas, cotizacion_id) VALUES
('2026-05-20', '09:00:00', '14:00:00', 'MATUTINO', 'Fiesta infantil con temática de superhéroes. Requiere acceso temprano para decoración.', 1),
('2026-05-25', '15:00:00', '20:00:00', 'VESPERTINO', 'Cumpleaños 10 años, confirmar llegada de mesas a tiempo.', 2),
('2026-06-05', '10:00:00', '15:00:00', 'MATUTINO', 'Evento escolar de fin de curso. Capacidad máxima esperada.', 3);

-- 9. Logística de Servicios (Planeación para el evento)
INSERT INTO logistica_servicios (evento_id, servicio_id, proveedor_asignado, hora_montaje, hora_inicio_servicio, hora_fin_servicio, observaciones_logistica, estado) VALUES
(1, 5, 'Proveedor Decoración SA', '07:30:00', '09:00:00', '14:00:00', 'Arco en la entrada principal', 'CONFIRMADO'),
(2, 1, 'Agencia Payasitas Feliz', '14:30:00', '16:00:00', '17:00:00', 'Tener micrófono listo', 'PROGRAMADO');

-- 10. Cargos Extras (Aplicados durante o al final del evento)
INSERT INTO cargos_extras (evento_id, descripcion, monto, fecha_registro, liquidado) VALUES
(1, 'Tiempo extra de salón (1 hora)', 500.00, '2026-05-20 14:15:00', true),
(2, 'Daño a mantelería', 250.00, '2026-05-25 20:30:00', false);