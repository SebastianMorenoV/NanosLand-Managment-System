USE nanosland_db;

-- ==========================================
-- SCRIPT DE DATOS: OPORTUNIDADES DE RECOMPRA
-- ==========================================
-- Este script inserta eventos FINALIZADOS en septiembre 2025
-- para que la vista "Oportunidades de Recompra" muestre datos
-- al buscar septiembre del año pasado (por defecto).
--
-- Prerequisito: Ejecutar datos_iniciales.sql primero.
-- Los clientes (IDs 1-19) y paquetes (IDs 1-8) deben existir.

-- Cotizaciones para eventos de septiembre 2025
INSERT INTO cotizaciones (folio, cliente_id, total, notas, estado, usuario_id, paquete_id) VALUES
('COT-R01', 1, 5500.00, 'Cumpleaños Juanito - Sept 2025', 'VIGENTE', 1, 2),
('COT-R02', 2, 8500.00, 'Fiesta acuática - Sept 2025', 'VIGENTE', 2, 3),
('COT-R03', 3, 12000.00, 'Fiesta total Carlos - Sept 2025', 'VIGENTE', 1, 4),
('COT-R04', 5, 3000.00, 'Evento básico Luis - Sept 2025', 'VIGENTE', 3, 1),
('COT-R05', 6, 15000.00, 'XV Años Mónica - Sept 2025', 'VIGENTE', 4, 8),
('COT-R06', 7, 4500.00, 'Show de magia Jorge - Sept 2025', 'VIGENTE', 5, 6),
('COT-R07', 8, 9000.00, 'Kermés Laura - Sept 2025', 'VIGENTE', 6, 7),
('COT-R08', 9, 5500.00, 'Cumpleaños Héctor - Sept 2025', 'VIGENTE', 7, 2),
('COT-R09', 11, 1500.00, 'Mini fiesta Fernando - Sept 2025', 'VIGENTE', 8, 5),
('COT-R10', 12, 8500.00, 'Pool party Valeria - Sept 2025', 'VIGENTE', 1, 3),
('COT-R11', 14, 12000.00, 'Evento grande Elena - Sept 2025', 'VIGENTE', 2, 4),
('COT-R12', 16, 15000.00, 'VIP Samuel - Sept 2025', 'VIGENTE', 3, 8),
('COT-R13', 17, 3000.00, 'Básico Camila - Sept 2025', 'VIGENTE', 4, 1),
('COT-R14', 18, 9000.00, 'Kermés Oscar - Sept 2025', 'VIGENTE', 5, 7),
('COT-R15', 4, 4500.00, 'Show magia Ana Sofía - Sept 2025', 'VIGENTE', 6, 6);

-- Eventos FINALIZADOS en septiembre 2025 vinculados a las cotizaciones anteriores
INSERT INTO eventos (fecha, hora_inicio, hora_fin, turno, estado, nombre_festejado, tematica, notas, cotizacion_id) VALUES
('2025-09-01', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Juanito Jr.', 'Superhéroes', 'Evento exitoso, cliente contento.', (SELECT id FROM cotizaciones WHERE folio = 'COT-R01')),
('2025-09-03', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'María Jr.', 'Sirenita', 'Pool party, todo salió bien.', (SELECT id FROM cotizaciones WHERE folio = 'COT-R02')),
('2025-09-05', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Carlitos', 'Dinosaurios', 'Fiesta grande, 80 invitados.', (SELECT id FROM cotizaciones WHERE folio = 'COT-R03')),
('2025-09-07', '15:00:00', '20:00:00', 'VESPERTINO', 'FINALIZADO', 'Luisito', 'Paw Patrol', 'Evento sencillo.', (SELECT id FROM cotizaciones WHERE folio = 'COT-R04')),
('2025-09-10', '19:00:00', '00:00:00', 'VESPERTINO', 'FINALIZADO', 'Mónica', 'Elegante Rosa', 'XV Años espectacular.', (SELECT id FROM cotizaciones WHERE folio = 'COT-R05')),
('2025-09-12', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Jorgito', 'Harry Potter', 'Show de magia temático.', (SELECT id FROM cotizaciones WHERE folio = 'COT-R06')),
('2025-09-14', '09:00:00', '14:00:00', 'MATUTINO', 'FINALIZADO', 'Colegio Laura', 'Kermés Escolar', 'Kermés para 150 niños.', (SELECT id FROM cotizaciones WHERE folio = 'COT-R07')),
('2025-09-16', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Héctor Jr.', 'Avengers', 'Buena fiesta.', (SELECT id FROM cotizaciones WHERE folio = 'COT-R08')),
('2025-09-18', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Fernandito', 'Mickey Mouse', 'Mini evento, todo bien.', (SELECT id FROM cotizaciones WHERE folio = 'COT-R09')),
('2025-09-20', '14:00:00', '19:00:00', 'VESPERTINO', 'FINALIZADO', 'Valeria', 'Tropical', 'Pool party de 60 personas.', (SELECT id FROM cotizaciones WHERE folio = 'COT-R10')),
('2025-09-22', '10:00:00', '15:00:00', 'MATUTINO', 'FINALIZADO', 'Elena', 'Unicornios', 'Evento grande familiar.', (SELECT id FROM cotizaciones WHERE folio = 'COT-R11')),
('2025-09-24', '18:00:00', '23:00:00', 'VESPERTINO', 'FINALIZADO', 'Samuel Jr.', 'Glow Party', 'Fiesta neón, excelente.', (SELECT id FROM cotizaciones WHERE folio = 'COT-R12')),
('2025-09-26', '15:00:00', '20:00:00', 'VESPERTINO', 'FINALIZADO', 'Camila', 'Frozen', 'Decoración hermosa.', (SELECT id FROM cotizaciones WHERE folio = 'COT-R13')),
('2025-09-28', '09:00:00', '14:00:00', 'MATUTINO', 'FINALIZADO', 'Kermés Oscar', 'Festival', 'Gran kermés.', (SELECT id FROM cotizaciones WHERE folio = 'COT-R14')),
('2025-09-30', '16:00:00', '21:00:00', 'VESPERTINO', 'FINALIZADO', 'Sofía', 'Princesas', 'Show de magia incluido.', (SELECT id FROM cotizaciones WHERE folio = 'COT-R15'));

-- Pagos completos para las cotizaciones (para que estén saldadas)
INSERT INTO pagos (cotizacion_id, cantidad, fecha_hora, tipo, folio_pago) VALUES
((SELECT id FROM cotizaciones WHERE folio = 'COT-R01'), 5500.00, '2025-08-20 10:00:00', 'EFECTIVO', 'PAGO-R01'),
((SELECT id FROM cotizaciones WHERE folio = 'COT-R02'), 8500.00, '2025-08-22 14:00:00', 'TRANSFERENCIA', 'PAGO-R02'),
((SELECT id FROM cotizaciones WHERE folio = 'COT-R03'), 12000.00, '2025-08-25 11:00:00', 'TARJETA', 'PAGO-R03'),
((SELECT id FROM cotizaciones WHERE folio = 'COT-R04'), 3000.00, '2025-08-28 09:00:00', 'EFECTIVO', 'PAGO-R04'),
((SELECT id FROM cotizaciones WHERE folio = 'COT-R05'), 15000.00, '2025-09-01 16:00:00', 'TRANSFERENCIA', 'PAGO-R05'),
((SELECT id FROM cotizaciones WHERE folio = 'COT-R06'), 4500.00, '2025-09-05 10:00:00', 'EFECTIVO', 'PAGO-R06'),
((SELECT id FROM cotizaciones WHERE folio = 'COT-R07'), 9000.00, '2025-09-08 12:00:00', 'TARJETA', 'PAGO-R07'),
((SELECT id FROM cotizaciones WHERE folio = 'COT-R08'), 5500.00, '2025-09-10 15:00:00', 'EFECTIVO', 'PAGO-R08'),
((SELECT id FROM cotizaciones WHERE folio = 'COT-R09'), 1500.00, '2025-09-12 09:00:00', 'EFECTIVO', 'PAGO-R09'),
((SELECT id FROM cotizaciones WHERE folio = 'COT-R10'), 8500.00, '2025-09-15 14:00:00', 'TRANSFERENCIA', 'PAGO-R10'),
((SELECT id FROM cotizaciones WHERE folio = 'COT-R11'), 12000.00, '2025-09-18 11:00:00', 'TARJETA', 'PAGO-R11'),
((SELECT id FROM cotizaciones WHERE folio = 'COT-R12'), 15000.00, '2025-09-20 16:00:00', 'TRANSFERENCIA', 'PAGO-R12'),
((SELECT id FROM cotizaciones WHERE folio = 'COT-R13'), 3000.00, '2025-09-22 10:00:00', 'EFECTIVO', 'PAGO-R13'),
((SELECT id FROM cotizaciones WHERE folio = 'COT-R14'), 9000.00, '2025-09-25 12:00:00', 'TARJETA', 'PAGO-R14'),
((SELECT id FROM cotizaciones WHERE folio = 'COT-R15'), 4500.00, '2025-09-28 15:00:00', 'EFECTIVO', 'PAGO-R15');
