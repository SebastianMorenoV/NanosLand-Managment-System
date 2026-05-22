USE nanosland_db;

-- ==========================================
-- SCRIPT DE INSERCIÓN DE DATOS SEMILLA
-- ==========================================
-- Se recomienda vaciar las tablas antes de ejecutar este script
-- en caso de que ya contengan datos, o usar hibernate ddl-auto=create.

-- 1. Usuarios (10)
INSERT INTO usuarios (correo, contrasena, telefono, rol, activo) VALUES
('dueno@nanosland.com', 'dueno123', '6441112233', 'DUEÑO', true),
('admin@nanosland.com', 'admin123', '6442223344', 'ADMINISTRADOR', true),
('martha@nanosland.com', 'martha123', '6443334455', 'ADMINISTRADOR', true),
('ventas1@nanosland.com', 'ventas123', '6444445566', 'ADMINISTRADOR', true),
('ventas2@nanosland.com', 'ventas123', '6445556677', 'ADMINISTRADOR', true),
('coord_eventos@nanosland.com', 'coord123', '6446667788', 'ADMINISTRADOR', true),
('staff1@nanosland.com', 'staff123', '6447778899', 'ADMINISTRADOR', true),
('staff2@nanosland.com', 'staff123', '6448889900', 'ADMINISTRADOR', true),
('inactivo@nanosland.com', 'pass', '6449990011', 'ADMINISTRADOR', false),
('auditor@nanosland.com', 'auditor123', '6440001122', 'DUEÑO', true);

-- 2. Clientes (20)
INSERT INTO clientes (nombre, telefono, correo, calle, colonia, ciudad, codigo_postal, activo) VALUES
('Juan Pérez Gómez', '6441234567', 'juan.perez@email.com', 'Calle 5 de Febrero', 'Centro', 'Ciudad Obregón', '85000', true),
('María García López', '6449876543', 'maria.gl@email.com', 'Kino 123', 'Las Haciendas', 'Ciudad Obregón', '85050', true),
('Carlos Valenzuela', '6445558899', NULL, 'Tabasco 456', 'Campestre', 'Ciudad Obregón', '85160', true),
('Ana Sofía Bernal', '6447779900', 'ana.bernal@mail.com', 'Sinaloa 789', 'Miravalle', 'Ciudad Obregón', '85200', true),
('Luis Fernando Castro', '6441112222', 'luis.castro@mail.com', 'Nainari 1010', 'Del Valle', 'Ciudad Obregón', '85030', true),
('Mónica Ruiz', '6442223333', 'monica.ruiz@mail.com', 'Allende 55', 'Hidalgo', 'Ciudad Obregón', '85010', true),
('Jorge Alcaraz', '6443334444', 'jorge.alc@mail.com', 'Pesqueira 334', 'Norte', 'Ciudad Obregón', '85040', true),
('Laura Quiñones', '6444445555', 'laura.q@mail.com', 'Guerrero 77', 'Sur', 'Ciudad Obregón', '85090', true),
('Héctor Salazar', '6445556666', 'hector.salazar@mail.com', 'Zaragoza 112', 'Centro', 'Ciudad Obregón', '85000', true),
('Diana Mendoza', '6446667777', NULL, 'Obregón 888', 'Villa Itson', 'Ciudad Obregón', '85130', true),
('Fernando Torres', '6447778888', 'fernando.torres@mail.com', 'California 99', 'Bellavista', 'Ciudad Obregón', '85020', true),
('Valeria Rivas', '6448889999', 'valeria.rivas@mail.com', 'Cajeme 200', 'Cajeme', 'Ciudad Obregón', '85060', true),
('Daniel Soto', '6449990000', 'daniel.soto@mail.com', 'Yaqui 50', 'Cortinas', 'Ciudad Obregón', '85080', true),
('Elena Domínguez', '6441231234', 'elena.d@mail.com', 'No Reelección 100', 'Prados', 'Ciudad Obregón', '85110', true),
('Ricardo Morales', '6443214321', NULL, 'Kino 333', 'Las Haciendas', 'Ciudad Obregón', '85050', true),
('Gabriela Fernández', '6444564567', 'gabriela.f@mail.com', 'Michoacán 44', 'Centro', 'Ciudad Obregón', '85000', true),
('Samuel Ortega', '6447897890', 'samuel.o@mail.com', 'Galeana 77', 'Campestre', 'Ciudad Obregón', '85160', true),
('Camila Vargas', '6446543210', 'camila.v@mail.com', 'Hidalgo 120', 'Hidalgo', 'Ciudad Obregón', '85010', true),
('Oscar Silva', '6442468101', 'oscar.s@mail.com', 'Zaragoza 10', 'Norte', 'Ciudad Obregón', '85040', true),
('Patricia Romo (Inactiva)', '6441357924', 'paty.r@mail.com', 'Durango 202', 'Miravalle', 'Ciudad Obregón', '85200', false);

-- 3. Servicios (Catálogo de 15 servicios)
INSERT INTO servicios (nombre, descripcion, precio, imagen, activo) VALUES
('Show Payasitas Nanos', 'Show interactivo de 1 hora con 2 payasitas', 1500.00, NULL, true),
('Brincolin Acuático', 'Alquiler de brincolin inflable con agua por 4 horas', 2000.00, NULL, true),
('Pintacaritas VIP', 'Maquillaje infantil con diseños especiales de fantasía', 800.00, NULL, true),
('Mesa de Snacks', 'Mesa de dulces, botanas y papas para 50 personas', 2500.00, NULL, true),
('Decoración Arco de Globos', 'Arco y centros de mesa básicos', 1200.00, NULL, true),
('Fotografía Profesional', 'Cobertura fotográfica durante 3 horas', 1800.00, NULL, true),
('Piñata Personalizada', 'Piñata grande del personaje deseado', 600.00, NULL, true),
('Show de Magia', 'Mago profesional por 1 hora', 2200.00, NULL, true),
('Carrito de Hot Dogs', 'Hot dogs ilimitados por 2 horas (mínimo 50 personas)', 3000.00, NULL, true),
('Máquina de Palomitas', 'Renta de máquina con insumos por 3 horas', 900.00, NULL, true),
('Animador Botarga', 'Botarga de personaje a elegir por 1 hora', 1000.00, NULL, true),
('Mesa de Postres', 'Cupcakes, galletas decoradas y mini pasteles', 3500.00, NULL, true),
('Toro Mecánico', 'Renta de toro mecánico por 4 horas', 4000.00, NULL, true),
('Iluminación y Sonido', 'Equipo de sonido básico y luces disco', 1500.00, NULL, true),
('Show Navideño (Inactivo)', 'Show especial de Navidad', 2000.00, NULL, false);

-- 4. Paquetes (10 paquetes)
INSERT INTO paquetes (nombre, descripcion, costo, activo) VALUES
('Paquete Básico', 'La opción rápida y sencilla. Renta de salón y decoración estándar.', 3000.00, true),
('Paquete NanosLand', 'Nuestro paquete estrella con show en vivo, perfecto para grandes fiestas.', 5500.00, true),
('Paquete Premium Acuático', 'Especial para el verano, trae diversión de agua e snacks.', 8500.00, true),
('Paquete Fiesta Total', 'Incluye botanas, comida, entretenimiento y decoración', 12000.00, true),
('Paquete Mini', 'Solo renta de espacio y brincolin', 1500.00, true),
('Paquete Magia', 'Enfocado en entretenimiento con show de magia', 4500.00, true),
('Paquete Kermés', 'Ideal para exteriores con varios carritos de comida', 9000.00, true),
('Paquete VIP Nanos', 'Experiencia completa con fotografía, postres y botargas', 15000.00, true),
('Paquete Halloween (Inactivo)', 'Paquete de temporada, ya no está disponible.', 4000.00, false),
('Paquete Invierno (Inactivo)', 'Paquete de temporada, ya no está disponible.', 4000.00, false);

-- 5. Paquetes - Servicios (Tabla Intermedia)
-- Paquete Básico (1)
INSERT INTO paquetes_servicios (paquete_id, servicio_id, cantidad, subtotal) VALUES
(1, 5, 1, 1200.00), (1, 3, 1, 800.00);

-- Paquete NanosLand (2)
INSERT INTO paquetes_servicios (paquete_id, servicio_id, cantidad, subtotal) VALUES
(2, 1, 1, 1500.00), (2, 4, 1, 2500.00), (2, 3, 1, 800.00);

-- Paquete Premium Acuático (3)
INSERT INTO paquetes_servicios (paquete_id, servicio_id, cantidad, subtotal) VALUES
(3, 2, 1, 2000.00), (3, 4, 1, 2500.00), (3, 5, 2, 2400.00);

-- Paquete Fiesta Total (4)
INSERT INTO paquetes_servicios (paquete_id, servicio_id, cantidad, subtotal) VALUES
(4, 1, 1, 1500.00), (4, 4, 1, 2500.00), (4, 9, 1, 3000.00), (4, 14, 1, 1500.00);

-- Paquete Magia (6)
INSERT INTO paquetes_servicios (paquete_id, servicio_id, cantidad, subtotal) VALUES
(6, 8, 1, 2200.00), (6, 5, 1, 1200.00);

-- Paquete VIP Nanos (8)
INSERT INTO paquetes_servicios (paquete_id, servicio_id, cantidad, subtotal) VALUES
(8, 1, 1, 1500.00), (8, 6, 1, 1800.00), (8, 12, 1, 3500.00), (8, 11, 2, 2000.00);

-- 6. Cotizaciones (20) (Sin campos obsoletos de fecha/turno/festejado/tematica)
INSERT INTO cotizaciones (folio, cliente_id, total, notas, estado, usuario_id, paquete_id) VALUES
('COT-001', 1, 5000.00, 'Cotización inicial pagada', 'VIGENTE', 1, 1),
('COT-002', 2, 8800.00, 'Cotización confirmada y pagada', 'VIGENTE', 2, 2),
('COT-003', 3, 12900.00, 'Evento grande', 'VIGENTE', 1, 3),
('COT-004', 4, 3000.00, 'Cliente aún no se decide', 'BORRADOR', 2, 1),
('COT-005', 1, 8500.00, 'Bautizo cancelado', 'CANCELADA', 1, 3),
('COT-006', 5, 12000.00, 'Pago en efectivo completo', 'VIGENTE', 3, 4),
('COT-007', 6, 1500.00, 'Cotización sencilla', 'VIGENTE', 4, 5),
('COT-008', 7, 4500.00, 'Pendiente de anticipo', 'BORRADOR', 5, 6),
('COT-009', 8, 9000.00, 'Kermés escolar', 'VIGENTE', 6, 7),
('COT-010', 9, 15000.00, 'Fiesta empresarial', 'VIGENTE', 7, 8),
('COT-011', 10, 3000.00, 'Cumpleaños 5 años', 'VIGENTE', 8, 1),
('COT-012', 11, 5500.00, 'Fiesta gemelos', 'VIGENTE', 1, 2),
('COT-013', 12, 8500.00, 'Pool party', 'VIGENTE', 2, 3),
('COT-014', 13, 12000.00, 'Graduación kinder', 'VIGENTE', 3, 4),
('COT-015', 14, 4500.00, 'Cotización pendiente de fecha', 'BORRADOR', 4, 6),
('COT-016', 15, 9000.00, 'Cancelada por clima', 'CANCELADA', 5, 7),
('COT-017', 16, 15000.00, 'XV Años', 'VIGENTE', 6, 8),
('COT-018', 17, 3000.00, 'Baby Shower', 'VIGENTE', 7, 1),
('COT-019', 18, 5500.00, 'Primera Comunión', 'VIGENTE', 8, 2),
('COT-020', 19, 8500.00, 'Despedida Soltera', 'VIGENTE', 1, 3);

-- 7. Detalles de Cotización (Servicios extra agregados)
INSERT INTO detalles_cotizacion (cotizacion_id, servicio_id, cantidad, precio_unitario, subtotal, hora_sugerida, especificaciones_cliente, desglose_opciones, ubicacion_montaje) VALUES
(2, 5, 1, 1200.00, 1200.00, '16:00:00', 'Decoración extra en la entrada', NULL, 'Entrada principal'),
(3, 1, 1, 1500.00, 1500.00, '12:00:00', 'Payasitas para romper la piñata', NULL, 'Centro del salón'),
(6, 6, 1, 1800.00, 1800.00, '17:00:00', 'Fotografía en pastel', NULL, 'Mesa principal'),
(10, 13, 1, 4000.00, 4000.00, '18:00:00', 'Toro mecánico para adultos', NULL, 'Patio trasero'),
(14, 10, 2, 900.00, 1800.00, '10:00:00', 'Dos máquinas de palomitas', NULL, 'Entrada'),
(17, 14, 1, 1500.00, 1500.00, '20:00:00', 'Luces disco potentes', NULL, 'Pista de baile'),
(19, 7, 3, 600.00, 1800.00, '15:00:00', '3 Piñatas de cruz', NULL, 'Centro de salón');

-- 8. Eventos (La nueva fuente de verdad para la logística)
INSERT INTO eventos (fecha, hora_inicio, hora_fin, turno, estado, nombre_festejado, tematica, notas, cotizacion_id) VALUES
('2026-05-20', '09:00:00', '14:00:00', 'MATUTINO', 'FINALIZADO', 'Juanito', 'Superhéroes', 'Requiere acceso temprano.', 1),
('2026-05-25', '15:00:00', '20:00:00', 'VESPERTINO', 'FINALIZADO', 'María Jr.', 'Princesas', 'Confirmar mesas a tiempo.', 2),
('2026-06-05', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Carlitos', 'Dinosaurios', 'Capacidad máxima esperada.', 3),
('2026-06-12', '16:00:00', '21:00:00', 'VESPERTINO', 'TENTATIVO', 'Sofía', 'Unicornios', 'Cliente no se decide.', 4),
('2026-06-15', '09:00:00', '14:00:00', 'MATUTINO', 'CANCELADO', 'Pedro', 'Clásico', 'Bautizo cancelado.', 5),
('2026-06-20', '15:00:00', '20:00:00', 'VESPERTINO', 'CONFIRMADO', 'Luisito', 'Transformers', 'Todo pagado.', 6),
('2026-06-25', '10:00:00', '15:00:00', 'MATUTINO', 'CONFIRMADO', 'Ana', 'Paw Patrol', '', 7),
('2026-07-02', '16:00:00', '21:00:00', 'VESPERTINO', 'TENTATIVO', 'Carlos', 'Futbol', 'Falta anticipo.', 8),
('2026-07-10', '09:00:00', '14:00:00', 'MATUTINO', 'CONFIRMADO', 'Kermes Colegio', 'Kermés', 'Evento de 200 personas.', 9),
('2026-07-15', '18:00:00', '23:00:00', 'VESPERTINO', 'CONFIRMADO', 'Empresa SA', 'Elegante', 'No niños.', 10),
('2026-07-20', '15:00:00', '20:00:00', 'VESPERTINO', 'CONFIRMADO', 'Pepe', 'Mickey Mouse', '', 11),
('2026-07-25', '10:00:00', '15:00:00', 'MATUTINO', 'CONFIRMADO', 'Gael y Zoe', 'Toy Story', 'Gemelos.', 12),
('2026-08-05', '14:00:00', '19:00:00', 'VESPERTINO', 'CONFIRMADO', 'Renata', 'Sirenita', 'Cuidado con el agua.', 13),
('2026-08-12', '09:00:00', '14:00:00', 'MATUTINO', 'CONFIRMADO', 'Generación 2026', 'Graduación', 'Mucha música.', 14),
('2026-08-15', '16:00:00', '21:00:00', 'VESPERTINO', 'TENTATIVO', 'Valeria', 'Peppa Pig', 'No hay fecha segura.', 15),
('2026-08-20', '10:00:00', '15:00:00', 'MATUTINO', 'CANCELADO', 'Héctor', 'Batman', 'Canceló por lluvia.', 16),
('2026-08-25', '19:00:00', '00:00:00', 'VESPERTINO', 'CONFIRMADO', 'Camila', 'Glow', 'XV Años, decoración neón.', 17),
('2026-09-02', '15:00:00', '20:00:00', 'VESPERTINO', 'CONFIRMADO', 'Bebé de Elena', 'Ositos', 'Baby shower tranquilo.', 18),
('2026-09-10', '09:00:00', '14:00:00', 'MATUTINO', 'CONFIRMADO', 'Samuel', 'Ángeles', 'Primera comunión.', 19),
('2026-09-15', '18:00:00', '23:00:00', 'VESPERTINO', 'CONFIRMADO', 'Paty', 'Despedida', 'Despedida de soltera.', 20);

-- 9. Logística de Servicios
INSERT INTO logistica_servicios (evento_id, servicio_id, responsable_turno, hora_requerida, especificaciones, desglose_opciones, ubicacion_montaje, estado) VALUES
(1, 5, 'Staff 1', '07:30:00', 'Arco en la entrada principal', NULL, 'Entrada principal', 'LISTO'),
(2, 1, 'Agencia Payasitas', '14:30:00', 'Tener micrófono listo', NULL, 'Centro del salón', 'LISTO'),
(3, 2, 'Brincolines Max', '08:00:00', 'Conectar manguera al fondo', NULL, 'Patio', 'LISTO'),
(6, 9, 'Hot Dogs El Chino', '14:00:00', 'Llevar salsas extra', NULL, 'Jardín', 'CONFIRMADO'),
(10, 13, 'Rodeo Party', '16:00:00', 'Enchufe trifásico necesario', NULL, 'Centro', 'ENCARGADO'),
(12, 1, 'Payasos XYZ', '09:30:00', 'Dos rutinas separadas', NULL, 'Escenario', 'ENCARGADO'),
(17, 14, 'DJ Sonido Master', '17:00:00', 'Probar luces a las 18:00', NULL, 'Pista de baile', 'ENCARGADO');

-- 10. Cargos Extras
INSERT INTO cargos_extras (evento_id, descripcion, cantidad, precio_unitario, subtotal, fecha_hora_cargo) VALUES
(1, 'Tiempo extra de salón (1 hora)', 1, 500.00, 500.00, '2026-05-20 14:15:00'),
(2, 'Daño a mantelería', 1, 250.00, 250.00, '2026-05-25 20:30:00'),
(3, 'Limpieza profunda por brincolin', 1, 300.00, 300.00, '2026-06-05 15:30:00'),
(10, 'Cristalería rota (5 vasos)', 5, 30.00, 150.00, '2026-07-15 23:30:00'),
(17, 'Uso de pirotecnia no permitida (Multa)', 1, 1000.00, 1000.00, '2026-08-25 22:00:00');

-- 11. Pagos (Abonos a las cotizaciones vigentes/finalizadas)
INSERT INTO pagos (cotizacion_id, cantidad, fecha_hora, tipo, folio_pago) VALUES
(1, 2000.00, '2026-04-01 10:00:00', 'EFECTIVO', 'PAGO-001'),
(1, 3000.00, '2026-05-15 12:00:00', 'TRANSFERENCIA', 'PAGO-002'),
(2, 4000.00, '2026-04-05 14:00:00', 'TARJETA', 'PAGO-003'),
(2, 4800.00, '2026-05-20 16:00:00', 'EFECTIVO', 'PAGO-004'),
(3, 5000.00, '2026-04-10 09:00:00', 'TRANSFERENCIA', 'PAGO-005'),
(3, 7900.00, '2026-05-30 11:00:00', 'TRANSFERENCIA', 'PAGO-006'),
(6, 12000.00, '2026-06-01 15:00:00', 'EFECTIVO', 'PAGO-007'),
(7, 1500.00, '2026-06-02 10:30:00', 'EFECTIVO', 'PAGO-008'),
(9, 4500.00, '2026-06-10 12:45:00', 'TARJETA', 'PAGO-009'),
(9, 4500.00, '2026-07-05 17:00:00', 'TRANSFERENCIA', 'PAGO-010'),
(10, 5000.00, '2026-06-15 11:15:00', 'TARJETA', 'PAGO-011'),
(11, 1500.00, '2026-06-20 09:30:00', 'EFECTIVO', 'PAGO-012'),
(12, 2000.00, '2026-06-25 14:20:00', 'EFECTIVO', 'PAGO-013'),
(13, 4000.00, '2026-07-01 16:10:00', 'TRANSFERENCIA', 'PAGO-014'),
(14, 6000.00, '2026-07-05 10:05:00', 'EFECTIVO', 'PAGO-015'),
(17, 5000.00, '2026-07-20 13:40:00', 'TRANSFERENCIA', 'PAGO-016'),
(18, 1500.00, '2026-08-01 15:55:00', 'EFECTIVO', 'PAGO-017'),
(19, 2000.00, '2026-08-05 08:30:00', 'EFECTIVO', 'PAGO-018'),
(20, 4000.00, '2026-08-10 12:00:00', 'TARJETA', 'PAGO-019');-- Masivos Cotizaciones
INSERT INTO cotizaciones (folio, cliente_id, total, notas, estado, usuario_id, paquete_id) VALUES
('COT-021', 13, 1500.00, 'Evento diario', 'VIGENTE', 1, 5),
('COT-022', 8, 12000.00, 'Evento diario', 'VIGENTE', 1, 4),
('COT-023', 14, 9000.00, 'Evento diario', 'VIGENTE', 1, 7),
('COT-024', 12, 15000.00, 'Evento diario', 'VIGENTE', 1, 8),
('COT-025', 4, 5500.00, 'Evento diario', 'VIGENTE', 1, 2),
('COT-026', 10, 9000.00, 'Evento diario', 'VIGENTE', 1, 7),
('COT-027', 3, 5500.00, 'Evento diario', 'VIGENTE', 1, 2),
('COT-028', 11, 15000.00, 'Evento diario', 'VIGENTE', 1, 8),
('COT-029', 18, 15000.00, 'Evento diario', 'VIGENTE', 1, 8),
('COT-030', 6, 9000.00, 'Evento diario', 'VIGENTE', 1, 7),
('COT-031', 8, 12000.00, 'Evento diario', 'VIGENTE', 1, 4),
('COT-032', 4, 1500.00, 'Evento diario', 'VIGENTE', 1, 5),
('COT-033', 8, 5500.00, 'Evento diario', 'VIGENTE', 1, 2),
('COT-034', 5, 12000.00, 'Evento diario', 'VIGENTE', 1, 4),
('COT-035', 15, 8500.00, 'Evento diario', 'VIGENTE', 1, 3),
('COT-036', 19, 12000.00, 'Evento diario', 'VIGENTE', 1, 4),
('COT-037', 7, 9000.00, 'Evento diario', 'VIGENTE', 1, 7),
('COT-038', 8, 1500.00, 'Evento diario', 'VIGENTE', 1, 5),
('COT-039', 14, 3000.00, 'Evento diario', 'VIGENTE', 1, 1),
('COT-040', 7, 3000.00, 'Evento diario', 'VIGENTE', 1, 1),
('COT-041', 16, 1500.00, 'Evento diario', 'VIGENTE', 1, 5),
('COT-042', 1, 5500.00, 'Evento diario', 'VIGENTE', 1, 2),
('COT-043', 4, 1500.00, 'Evento diario', 'VIGENTE', 1, 5),
('COT-044', 9, 12000.00, 'Evento diario', 'VIGENTE', 1, 4),
('COT-045', 4, 5500.00, 'Evento diario', 'VIGENTE', 1, 2),
('COT-046', 18, 1500.00, 'Evento diario', 'VIGENTE', 1, 5),
('COT-047', 7, 3000.00, 'Evento diario', 'VIGENTE', 1, 1),
('COT-048', 18, 1500.00, 'Evento diario', 'VIGENTE', 1, 5),
('COT-049', 11, 9000.00, 'Evento diario', 'VIGENTE', 1, 7),
('COT-050', 10, 1500.00, 'Evento diario', 'VIGENTE', 1, 5),
('COT-051', 19, 1500.00, 'Evento diario', 'VIGENTE', 1, 5),
('COT-052', 8, 5500.00, 'Evento diario', 'VIGENTE', 1, 2),
('COT-053', 12, 4500.00, 'Evento diario', 'VIGENTE', 1, 6),
('COT-054', 19, 9000.00, 'Evento diario', 'VIGENTE', 1, 7),
('COT-055', 13, 5500.00, 'Evento diario', 'VIGENTE', 1, 2),
('COT-056', 2, 9000.00, 'Evento diario', 'VIGENTE', 1, 7),
('COT-057', 7, 3000.00, 'Evento diario', 'VIGENTE', 1, 1),
('COT-058', 3, 8500.00, 'Evento diario', 'VIGENTE', 1, 3),
('COT-059', 2, 15000.00, 'Evento diario', 'VIGENTE', 1, 8),
('COT-060', 13, 3000.00, 'Evento diario', 'VIGENTE', 1, 1),
('COT-061', 18, 1500.00, 'Evento diario', 'VIGENTE', 1, 5),
('COT-062', 10, 8500.00, 'Evento diario', 'VIGENTE', 1, 3),
('COT-063', 6, 9000.00, 'Evento diario', 'VIGENTE', 1, 7),
('COT-064', 8, 5500.00, 'Evento diario', 'VIGENTE', 1, 2),
('COT-065', 14, 4500.00, 'Evento diario', 'VIGENTE', 1, 6),
('COT-066', 9, 4500.00, 'Evento diario', 'VIGENTE', 1, 6),
('COT-067', 10, 1500.00, 'Evento diario', 'VIGENTE', 1, 5),
('COT-068', 7, 15000.00, 'Evento diario', 'VIGENTE', 1, 8),
('COT-069', 3, 4500.00, 'Evento diario', 'VIGENTE', 1, 6),
('COT-070', 15, 4500.00, 'Evento diario', 'VIGENTE', 1, 6),
('COT-071', 10, 4500.00, 'Evento diario', 'VIGENTE', 1, 6),
('COT-072', 18, 3000.00, 'Evento diario', 'VIGENTE', 1, 1),
('COT-073', 10, 9000.00, 'Evento diario', 'VIGENTE', 1, 7),
('COT-074', 7, 4500.00, 'Evento diario', 'VIGENTE', 1, 6),
('COT-075', 19, 8500.00, 'Evento diario', 'VIGENTE', 1, 3),
('COT-076', 10, 9000.00, 'Evento diario', 'VIGENTE', 1, 7),
('COT-077', 13, 1500.00, 'Evento diario', 'VIGENTE', 1, 5),
('COT-078', 8, 1500.00, 'Evento diario', 'VIGENTE', 1, 5),
('COT-079', 2, 8500.00, 'Evento diario', 'VIGENTE', 1, 3),
('COT-080', 17, 9000.00, 'Evento diario', 'VIGENTE', 1, 7),
('COT-081', 19, 1500.00, 'Evento diario', 'VIGENTE', 1, 5),
('COT-082', 11, 3000.00, 'Evento diario', 'VIGENTE', 1, 1),
('COT-083', 9, 12000.00, 'Evento diario', 'VIGENTE', 1, 4),
('COT-084', 15, 5500.00, 'Evento diario', 'VIGENTE', 1, 2),
('COT-085', 13, 1500.00, 'Evento diario', 'VIGENTE', 1, 5),
('COT-086', 8, 15000.00, 'Evento diario', 'VIGENTE', 1, 8),
('COT-087', 6, 15000.00, 'Evento diario', 'VIGENTE', 1, 8),
('COT-088', 4, 4500.00, 'Evento diario', 'VIGENTE', 1, 6),
('COT-089', 1, 4500.00, 'Evento diario', 'VIGENTE', 1, 6),
('COT-090', 11, 5500.00, 'Evento diario', 'VIGENTE', 1, 2),
('COT-091', 16, 1500.00, 'Evento diario', 'VIGENTE', 1, 5),
('COT-092', 18, 8500.00, 'Evento diario', 'VIGENTE', 1, 3);

-- Masivos Eventos
INSERT INTO eventos (fecha, hora_inicio, hora_fin, turno, estado, nombre_festejado, tematica, notas, cotizacion_id) VALUES
('2026-04-01', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Maria', 'Unicornios', 'Autogenerado', 21),
('2026-04-02', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Regina', 'Sirenita', 'Autogenerado', 22),
('2026-04-02', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Leonardo', 'Spiderman', 'Autogenerado', 23),
('2026-04-03', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Emiliano', 'Batman', 'Autogenerado', 24),
('2026-04-04', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Maria', 'Dinosaurios', 'Autogenerado', 25),
('2026-04-04', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Maria', 'Frozen', 'Autogenerado', 26),
('2026-04-05', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Valentina', 'Toy Story', 'Autogenerado', 27),
('2026-04-05', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Emiliano', 'Spiderman', 'Autogenerado', 28),
('2026-04-06', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Valentina', 'Frozen', 'Autogenerado', 29),
('2026-04-06', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Santiago', 'Frozen', 'Autogenerado', 30),
('2026-04-07', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Regina', 'Frozen', 'Autogenerado', 31),
('2026-04-08', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Regina', 'Dinosaurios', 'Autogenerado', 32),
('2026-04-09', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Valentina', 'Batman', 'Autogenerado', 33),
('2026-04-10', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Camila', 'Princesas', 'Autogenerado', 34),
('2026-04-10', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Santiago', 'Toy Story', 'Autogenerado', 35),
('2026-04-11', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Regina', 'Paw Patrol', 'Autogenerado', 36),
('2026-04-11', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Victoria', 'Unicornios', 'Autogenerado', 37),
('2026-04-12', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Maria', 'Toy Story', 'Autogenerado', 38),
('2026-04-12', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Emiliano', 'Frozen', 'Autogenerado', 39),
('2026-04-13', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Camila', 'Batman', 'Autogenerado', 40),
('2026-04-14', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Maria', 'Mickey', 'Autogenerado', 41),
('2026-04-14', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Sebastian', 'Mario Bros', 'Autogenerado', 42),
('2026-04-15', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Emiliano', 'Sirenita', 'Autogenerado', 43),
('2026-04-16', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Santiago', 'Mario Bros', 'Autogenerado', 44),
('2026-04-17', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Maria', 'Mario Bros', 'Autogenerado', 45),
('2026-04-18', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Santiago', 'Frozen', 'Autogenerado', 46),
('2026-04-19', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Emiliano', 'Avengers', 'Autogenerado', 47),
('2026-04-19', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Regina', 'Batman', 'Autogenerado', 48),
('2026-04-20', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Emiliano', 'Mickey', 'Autogenerado', 49),
('2026-04-21', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Emiliano', 'Princesas', 'Autogenerado', 50),
('2026-04-21', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Santiago', 'Unicornios', 'Autogenerado', 51),
('2026-04-22', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Matias', 'Sirenita', 'Autogenerado', 52),
('2026-04-23', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Regina', 'Batman', 'Autogenerado', 53),
('2026-04-24', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Emiliano', 'Mario Bros', 'Autogenerado', 54),
('2026-04-24', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Santiago', 'Avengers', 'Autogenerado', 55),
('2026-04-25', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Sebastian', 'Mickey', 'Autogenerado', 56),
('2026-04-26', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Valentina', 'Batman', 'Autogenerado', 57),
('2026-04-27', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Ximena', 'Sirenita', 'Autogenerado', 58),
('2026-04-28', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Victoria', 'Unicornios', 'Autogenerado', 59),
('2026-04-28', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Santiago', 'Mickey', 'Autogenerado', 60),
('2026-04-29', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Victoria', 'Unicornios', 'Autogenerado', 61),
('2026-04-29', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Mateo', 'Sirenita', 'Autogenerado', 62),
('2026-04-30', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Maria', 'Mario Bros', 'Autogenerado', 63),
('2026-04-30', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Santiago', 'Princesas', 'Autogenerado', 64),
('2026-05-01', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Santiago', 'Frozen', 'Autogenerado', 65),
('2026-05-02', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Santiago', 'Princesas', 'Autogenerado', 66),
('2026-05-02', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Matias', 'Sirenita', 'Autogenerado', 67),
('2026-05-03', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Matias', 'Mickey', 'Autogenerado', 68),
('2026-05-04', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Camila', 'Dinosaurios', 'Autogenerado', 69),
('2026-05-05', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Matias', 'Toy Story', 'Autogenerado', 70),
('2026-05-06', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Valentina', 'Unicornios', 'Autogenerado', 71),
('2026-05-07', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Leonardo', 'Avengers', 'Autogenerado', 72),
('2026-05-08', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Camila', 'Princesas', 'Autogenerado', 73),
('2026-05-08', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Sebastian', 'Paw Patrol', 'Autogenerado', 74),
('2026-05-09', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Camila', 'Princesas', 'Autogenerado', 75),
('2026-05-10', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Camila', 'Sirenita', 'Autogenerado', 76),
('2026-05-11', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Emiliano', 'Avengers', 'Autogenerado', 77),
('2026-05-12', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Leonardo', 'Toy Story', 'Autogenerado', 78),
('2026-05-13', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Mateo', 'Unicornios', 'Autogenerado', 79),
('2026-05-14', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Camila', 'Unicornios', 'Autogenerado', 80),
('2026-05-14', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Sebastian', 'Dinosaurios', 'Autogenerado', 81),
('2026-05-15', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Camila', 'Princesas', 'Autogenerado', 82),
('2026-05-15', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Valentina', 'Princesas', 'Autogenerado', 83),
('2026-05-16', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Santiago', 'Dinosaurios', 'Autogenerado', 84),
('2026-05-17', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Valentina', 'Princesas', 'Autogenerado', 85),
('2026-05-18', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Mateo', 'Toy Story', 'Autogenerado', 86),
('2026-05-19', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Emiliano', 'Sirenita', 'Autogenerado', 87),
('2026-05-19', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Maria', 'Spiderman', 'Autogenerado', 88),
('2026-05-20', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Emiliano', 'Batman', 'Autogenerado', 89),
('2026-05-20', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Mateo', 'Toy Story', 'Autogenerado', 90),
('2026-05-21', '16:00:00', '21:00:00', 'VESPERTINO', 'CONFIRMADO', 'Ximena', 'Batman', 'Autogenerado', 91),
('2026-05-21', '10:00:00', '15:00:00', 'MATUTINO', 'CONFIRMADO', 'Ximena', 'Princesas', 'Autogenerado', 92);

-- Masivos Pagos
INSERT INTO pagos (cotizacion_id, cantidad, fecha_hora, tipo, folio_pago) VALUES
(21, 1500.00, '2026-04-01 10:00:00', 'EFECTIVO', 'PAGO-020'),
(22, 12000.00, '2026-04-02 10:00:00', 'EFECTIVO', 'PAGO-021'),
(23, 9000.00, '2026-04-02 10:00:00', 'EFECTIVO', 'PAGO-022'),
(24, 15000.00, '2026-04-03 10:00:00', 'EFECTIVO', 'PAGO-023'),
(25, 5500.00, '2026-04-04 10:00:00', 'EFECTIVO', 'PAGO-024'),
(26, 9000.00, '2026-04-04 10:00:00', 'EFECTIVO', 'PAGO-025'),
(27, 5500.00, '2026-04-05 10:00:00', 'EFECTIVO', 'PAGO-026'),
(28, 15000.00, '2026-04-05 10:00:00', 'EFECTIVO', 'PAGO-027'),
(29, 15000.00, '2026-04-06 10:00:00', 'EFECTIVO', 'PAGO-028'),
(30, 9000.00, '2026-04-06 10:00:00', 'EFECTIVO', 'PAGO-029'),
(31, 12000.00, '2026-04-07 10:00:00', 'EFECTIVO', 'PAGO-030'),
(32, 1500.00, '2026-04-08 10:00:00', 'EFECTIVO', 'PAGO-031'),
(33, 5500.00, '2026-04-09 10:00:00', 'EFECTIVO', 'PAGO-032'),
(34, 12000.00, '2026-04-10 10:00:00', 'EFECTIVO', 'PAGO-033'),
(35, 8500.00, '2026-04-10 10:00:00', 'EFECTIVO', 'PAGO-034'),
(36, 12000.00, '2026-04-11 10:00:00', 'EFECTIVO', 'PAGO-035'),
(37, 9000.00, '2026-04-11 10:00:00', 'EFECTIVO', 'PAGO-036'),
(38, 1500.00, '2026-04-12 10:00:00', 'EFECTIVO', 'PAGO-037'),
(39, 3000.00, '2026-04-12 10:00:00', 'EFECTIVO', 'PAGO-038'),
(40, 3000.00, '2026-04-13 10:00:00', 'EFECTIVO', 'PAGO-039'),
(41, 1500.00, '2026-04-14 10:00:00', 'EFECTIVO', 'PAGO-040'),
(42, 5500.00, '2026-04-14 10:00:00', 'EFECTIVO', 'PAGO-041'),
(43, 1500.00, '2026-04-15 10:00:00', 'EFECTIVO', 'PAGO-042'),
(44, 12000.00, '2026-04-16 10:00:00', 'EFECTIVO', 'PAGO-043'),
(45, 5500.00, '2026-04-17 10:00:00', 'EFECTIVO', 'PAGO-044'),
(46, 1500.00, '2026-04-18 10:00:00', 'EFECTIVO', 'PAGO-045'),
(47, 3000.00, '2026-04-19 10:00:00', 'EFECTIVO', 'PAGO-046'),
(48, 1500.00, '2026-04-19 10:00:00', 'EFECTIVO', 'PAGO-047'),
(49, 9000.00, '2026-04-20 10:00:00', 'EFECTIVO', 'PAGO-048'),
(50, 1500.00, '2026-04-21 10:00:00', 'EFECTIVO', 'PAGO-049'),
(51, 1500.00, '2026-04-21 10:00:00', 'EFECTIVO', 'PAGO-050'),
(52, 5500.00, '2026-04-22 10:00:00', 'EFECTIVO', 'PAGO-051'),
(53, 4500.00, '2026-04-23 10:00:00', 'EFECTIVO', 'PAGO-052'),
(54, 9000.00, '2026-04-24 10:00:00', 'EFECTIVO', 'PAGO-053'),
(55, 5500.00, '2026-04-24 10:00:00', 'EFECTIVO', 'PAGO-054'),
(56, 9000.00, '2026-04-25 10:00:00', 'EFECTIVO', 'PAGO-055'),
(57, 3000.00, '2026-04-26 10:00:00', 'EFECTIVO', 'PAGO-056'),
(58, 8500.00, '2026-04-27 10:00:00', 'EFECTIVO', 'PAGO-057'),
(59, 15000.00, '2026-04-28 10:00:00', 'EFECTIVO', 'PAGO-058'),
(60, 3000.00, '2026-04-28 10:00:00', 'EFECTIVO', 'PAGO-059'),
(61, 1500.00, '2026-04-29 10:00:00', 'EFECTIVO', 'PAGO-060'),
(62, 8500.00, '2026-04-29 10:00:00', 'EFECTIVO', 'PAGO-061'),
(63, 9000.00, '2026-04-30 10:00:00', 'EFECTIVO', 'PAGO-062'),
(64, 5500.00, '2026-04-30 10:00:00', 'EFECTIVO', 'PAGO-063'),
(65, 4500.00, '2026-05-01 10:00:00', 'EFECTIVO', 'PAGO-064'),
(66, 4500.00, '2026-05-02 10:00:00', 'EFECTIVO', 'PAGO-065'),
(67, 1500.00, '2026-05-02 10:00:00', 'EFECTIVO', 'PAGO-066'),
(68, 15000.00, '2026-05-03 10:00:00', 'EFECTIVO', 'PAGO-067'),
(69, 4500.00, '2026-05-04 10:00:00', 'EFECTIVO', 'PAGO-068'),
(70, 4500.00, '2026-05-05 10:00:00', 'EFECTIVO', 'PAGO-069'),
(71, 4500.00, '2026-05-06 10:00:00', 'EFECTIVO', 'PAGO-070'),
(72, 3000.00, '2026-05-07 10:00:00', 'EFECTIVO', 'PAGO-071'),
(73, 9000.00, '2026-05-08 10:00:00', 'EFECTIVO', 'PAGO-072'),
(74, 4500.00, '2026-05-08 10:00:00', 'EFECTIVO', 'PAGO-073'),
(75, 8500.00, '2026-05-09 10:00:00', 'EFECTIVO', 'PAGO-074'),
(76, 9000.00, '2026-05-10 10:00:00', 'EFECTIVO', 'PAGO-075'),
(77, 1500.00, '2026-05-11 10:00:00', 'EFECTIVO', 'PAGO-076'),
(78, 1500.00, '2026-05-12 10:00:00', 'EFECTIVO', 'PAGO-077'),
(79, 8500.00, '2026-05-13 10:00:00', 'EFECTIVO', 'PAGO-078'),
(80, 9000.00, '2026-05-14 10:00:00', 'EFECTIVO', 'PAGO-079'),
(81, 1500.00, '2026-05-14 10:00:00', 'EFECTIVO', 'PAGO-080'),
(82, 3000.00, '2026-05-15 10:00:00', 'EFECTIVO', 'PAGO-081'),
(83, 12000.00, '2026-05-15 10:00:00', 'EFECTIVO', 'PAGO-082'),
(84, 5500.00, '2026-05-16 10:00:00', 'EFECTIVO', 'PAGO-083'),
(85, 1500.00, '2026-05-17 10:00:00', 'EFECTIVO', 'PAGO-084'),
(86, 15000.00, '2026-05-18 10:00:00', 'EFECTIVO', 'PAGO-085'),
(87, 15000.00, '2026-05-19 10:00:00', 'EFECTIVO', 'PAGO-086'),
(88, 4500.00, '2026-05-19 10:00:00', 'EFECTIVO', 'PAGO-087'),
(89, 4500.00, '2026-05-20 10:00:00', 'EFECTIVO', 'PAGO-088'),
(90, 5500.00, '2026-05-20 10:00:00', 'EFECTIVO', 'PAGO-089'),
(91, 1500.00, '2026-05-21 10:00:00', 'EFECTIVO', 'PAGO-090'),
(92, 8500.00, '2026-05-21 10:00:00', 'EFECTIVO', 'PAGO-091');

