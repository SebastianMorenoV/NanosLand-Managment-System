USE nanosland_db;
SET NAMES utf8mb4;

-- =====================================================================
-- CATÁLOGO VIGENTE DE NANOSLAND: SERVICIOS Y PAQUETES (noviembre 2026)
-- =====================================================================
-- Carga los servicios y paquetes de los volantes actuales del local:
-- Renta de local, Paquete Plus, Paquete Full, Paquete Neón y Extras.
--
-- Cómo se acomoda cada volante en el sistema:
--   * Extras            -> un servicio de categoría EXTRA por renglón, con su precio.
--   * Renta de local    -> paquete de precio fijo ($9,800).
--   * Plus, Full y Neón -> un paquete cada uno, con su tabla de precios por comida
--                          (7) y número de personas (60/80/100/120): 28 precios
--                          por paquete.
--   * Precios: se usa el precio más alto del volante (viernes a domingo). El de
--     lunes a jueves se maneja con un descuento desde la pantalla Descuentos.
--   * Las comidas son servicios de categoría COMIDA (precio 0: lo pone la tabla).
--   * La payasita, el pintacaritas y la decoración de mesa son de categoría
--     INCLUIDO: van dentro de los paquetes y no salen en la lista de extras.
--   * Lo que cada paquete agrega a la renta (meseros, payasita, bollitos...) se
--     liga como servicios incluidos, así aparece en su detalle y en la logística.
--
-- Se puede ejecutar más de una vez: solo inserta lo que falta (busca por nombre
-- entre los registros activos) y no toca paquetes ya armados ni editados desde la
-- aplicación. La única corrección que hace sobre datos existentes es pasar a
-- INCLUIDO los servicios incluidos de esta lista que sigan como EXTRA (p. ej. si
-- se cargó la versión anterior de este script). Al final muestra un resumen.
--
-- Uso: primero abre la versión nueva de la aplicación (crea las tablas y columnas
-- nuevas) y después ejecuta:
--   mysql -u root -p --default-character-set=utf8mb4 < sql/catalogo_paquetes_servicios.sql
-- =====================================================================

-- Si alguna de estas consultas falla, la base todavía no tiene las tablas nuevas:
-- abre primero la versión nueva de la aplicación.
SELECT categoria FROM servicios LIMIT 0;
SELECT id FROM paquetes_precios LIMIT 0;

-- MySQL Workbench trae activado el modo "safe updates", que no deja hacer UPDATE
-- sin la llave en el WHERE; se apaga solo durante este script.
SET @safe_updates_previo = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

-- ---------------------------------------------------------------------
-- 1. Servicios
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_servicios;
CREATE TEMPORARY TABLE tmp_servicios (
    orden       INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(255)  NOT NULL,
    descripcion VARCHAR(255)  NOT NULL,
    precio      DECIMAL(10,2) NOT NULL,
    categoria   VARCHAR(20)   NOT NULL
);

INSERT INTO tmp_servicios (nombre, descripcion, precio, categoria) VALUES
-- Extras: personal y artículos para la fiesta
('Mesero(a)',                             'Mesero o mesera durante el evento.',                         500, 'EXTRA'),
('Piñata',                                'Piñata para el evento.',                                     400, 'EXTRA'),
('Caballetes (4)',                        'Juego de 4 caballetes.',                                     700, 'EXTRA'),
('Niñera (4 hrs)',                        'Niñera durante 4 horas.',                                    500, 'EXTRA'),
('Invitación digital',                    'Invitación digital para el evento.',                         150, 'EXTRA'),
('Vela para pastel',                      'Vela para el pastel.',                                        50, 'EXTRA'),
('Sobres para dinero (15) con pluma',     'Paquete de 15 sobres para dinero, con pluma.',                60, 'EXTRA'),
-- Extras: NEÓN, calcetines y tiempo extra
('NEÓN libre (todo el evento)',           'NEÓN durante todo el evento, las veces que quiera.',        1200, 'EXTRA'),
('4 canciones NEÓN',                      'Cuatro canciones de NEÓN.',                                  600, 'EXTRA'),
('Calcetín antiderrapante',               'Par de calcetines antiderrapantes. Precio por par.',          40, 'EXTRA'),
('1/2 hora extra',                        'Media hora adicional de evento.',                           1000, 'EXTRA'),
('1 hora extra',                          'Una hora adicional de evento.',                             1800, 'EXTRA'),
-- Extras: barra de snack
('Barra de snack (60 vasos)',             'Barra de snacks con 60 vasos.',                             2200, 'EXTRA'),
('Barra de snack (80 vasos)',             'Barra de snacks con 80 vasos.',                             2800, 'EXTRA'),
('Barra de snack (100 vasos)',            'Barra de snacks con 100 vasos.',                            3500, 'EXTRA'),
('Barra de snack (120 vasos)',            'Barra de snacks con 120 vasos.',                            4000, 'EXTRA'),
-- Extras: bebidas y desechables
('Vitrolero de agua fresca',              'Vitrolero de agua fresca.',                                  450, 'EXTRA'),
('Refresco de sabor 3L',                  'Refresco de sabor de 3 litros.',                              60, 'EXTRA'),
('Refresco coca 3L',                      'Coca-Cola de 3 litros.',                                      70, 'EXTRA'),
('Garrafón de agua purificada',           'Garrafón de agua purificada.',                                30, 'EXTRA'),
('Vasos desechables (50 pzas)',           'Paquete de 50 vasos desechables.',                            60, 'EXTRA'),
('Charola botanera (10 pzas)',            'Paquete de 10 charolas botaneras.',                           50, 'EXTRA'),
('Vaso para chamoy o salsa (20 pzas)',    'Paquete de 20 vasitos para chamoy o salsa.',                  20, 'EXTRA'),
('Servicio de café (40 vasos)',           'Servicio de café para 40 vasos.',                            450, 'EXTRA'),
-- Extras: comida y pastel
('Tortita y jugo (mínimo 30)',            'Tortita y jugo. Precio por persona; mínimo 30.',              35, 'EXTRA'),
('Hamburguesa mini con papas (mínimo 20)','Hamburguesa mini con papas. Precio por pieza; mínimo 20.',    45, 'EXTRA'),
('Pastel 15 personas',                    'Pastel para 15 personas.',                                   470, 'EXTRA'),
('Bollito',                               'Bollito extra. Precio por pieza.',                            19, 'EXTRA'),
('Bollito relleno',                       'Bollito relleno extra. Precio por pieza.',                    21, 'EXTRA'),
('Cake topper circular 3 cm',             'Cake topper circular de 3 cm. Precio por pieza.',              1, 'EXTRA'),
('Hot dog con papas extra',               'Hot dog con papas adicional. Precio por pieza.',              45, 'EXTRA'),
('Plato de taquiza extra',                'Plato de taquiza adicional.',                                 65, 'EXTRA'),
('Snack extra',                           'Snack adicional.',                                            40, 'EXTRA'),
-- Incluidos en los paquetes (no se venden por separado)
('Payasita (2 horas)',                    'Payasita durante 2 horas. Incluida en los paquetes Plus, Full y Neón.',                 0, 'INCLUIDO'),
('Payasita pintacaritas NEÓN (1 hora)',   'Payasita pintacaritas NEÓN durante 1 hora. Incluida en el paquete Neón.',               0, 'INCLUIDO'),
('Decoración de mesa principal (4 m)',    'Decoración de 4 metros de la mesa principal. Incluida en los paquetes Full y Neón.',    0, 'INCLUIDO'),
-- Comidas de los paquetes Plus, Full y Neón: el precio lo pone la tabla de cada paquete
('Hot dog con papas',                     'Comida de los paquetes Plus, Full y Neón; su precio va en la tabla del paquete.', 0, 'COMIDA'),
('Pizza con espagueti (1 rebanada p/p)',  'Comida de los paquetes Plus, Full y Neón; su precio va en la tabla del paquete.', 0, 'COMIDA'),
('Tacos a vapor con frijol (sandwichón de pollo con frijol)', 'Comida de los paquetes Plus, Full y Neón; su precio va en la tabla del paquete.', 0, 'COMIDA'),
('Taquiza (3 guisos y 1 frijol)',         'Comida de los paquetes Plus, Full y Neón; su precio va en la tabla del paquete.', 0, 'COMIDA'),
('Barbacoa',                              'Comida de los paquetes Plus, Full y Neón; su precio va en la tabla del paquete.', 0, 'COMIDA'),
('Hamburguesa con papas',                 'Comida de los paquetes Plus, Full y Neón; su precio va en la tabla del paquete.', 0, 'COMIDA'),
('Tacos de asada (3)',                    'Comida de los paquetes Plus, Full y Neón; su precio va en la tabla del paquete.', 0, 'COMIDA');

INSERT INTO servicios (nombre, descripcion, precio, imagen, activo, categoria)
SELECT t.nombre, t.descripcion, t.precio, NULL, TRUE, t.categoria
  FROM tmp_servicios t
 WHERE NOT EXISTS (SELECT 1 FROM servicios s WHERE s.activo = TRUE AND s.nombre = t.nombre)
 ORDER BY t.orden;

-- Servicios incluidos de esta lista que ya existían como EXTRA (p. ej. de la versión anterior
-- de este script): pasan a INCLUIDO para que dejen de salir en la lista de extras.
UPDATE servicios
   SET categoria = 'INCLUIDO'
 WHERE activo = TRUE
   AND categoria = 'EXTRA'
   AND nombre IN (SELECT t.nombre FROM tmp_servicios t WHERE t.categoria = 'INCLUIDO');

-- ---------------------------------------------------------------------
-- 2. Paquetes
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_paquetes;
CREATE TEMPORARY TABLE tmp_paquetes (
    orden       INT AUTO_INCREMENT PRIMARY KEY,
    nivel       VARCHAR(20)   NOT NULL,
    nombre      VARCHAR(255)  NOT NULL,
    descripcion VARCHAR(255)  NOT NULL,
    costo       DECIMAL(10,2) NOT NULL
);

-- costo 0 = el precio sale de la tabla de precios (sección 4).
INSERT INTO tmp_paquetes (nivel, nombre, descripcion, costo) VALUES
('RENTA', 'Renta de local', 'Incluye 4 horas de evento, aire acondicionado, 10 mesas de madera con 10 sillas, música ambiental, 1 encargado, 4 staff, 1 garrafón de agua purificada y 1 canción NEÓN. Capacidad: 120 personas incluyendo menores. No incluye meseros, hielo ni desechables.', 9800),
('PLUS',  'Paquete Plus', 'Todo lo de la renta, más: 1 mesero, 1 vitrolero de agua fresca, 10 refrescos de 3L, 1 payasita por 2 horas y 4 canciones NEÓN.', 0),
('FULL',  'Paquete Full', 'Todo lo de la renta, más: 2 meseros, 1 vitrolero de agua fresca, 10 refrescos de 3L, 1 payasita por 2 horas, 4 canciones NEÓN, 80 bollitos y decoración de 4 m de mesa principal.', 0),
('NEON',  'Paquete Neón', 'Todo lo de la renta, más: 2 meseros, vitrolero de agua fresca, 10 refrescos de 3L, payasita 2 horas, NEÓN libre, 80 bollitos, decoración de 4 m de mesa principal, 50 pares de calcetines, pintacaritas NEÓN 1 hora y barra de snacks (100 vasos).', 0);

INSERT INTO paquetes (nombre, descripcion, costo, activo)
SELECT t.nombre, t.descripcion, t.costo, TRUE
  FROM tmp_paquetes t
 WHERE NOT EXISTS (SELECT 1 FROM paquetes p WHERE p.activo = TRUE AND p.nombre = t.nombre)
 ORDER BY t.orden;

-- Id de cada paquete del catálogo (el activo con ese nombre).
DROP TEMPORARY TABLE IF EXISTS tmp_ids_paquetes;
CREATE TEMPORARY TABLE tmp_ids_paquetes (
    nivel      VARCHAR(20) NOT NULL PRIMARY KEY,
    paquete_id BIGINT      NOT NULL,
    costo      DOUBLE      NOT NULL
);

INSERT INTO tmp_ids_paquetes (nivel, paquete_id, costo)
SELECT t.nivel, p.id, p.costo
  FROM tmp_paquetes t
  JOIN (SELECT nombre, MIN(id) AS id FROM paquetes WHERE activo = TRUE GROUP BY nombre) pid ON pid.nombre = t.nombre
  JOIN paquetes p ON p.id = pid.id;

-- ---------------------------------------------------------------------
-- 3. Servicios incluidos de cada paquete (paquetes_servicios)
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_composicion;
CREATE TEMPORARY TABLE tmp_composicion (
    orden    INT AUTO_INCREMENT PRIMARY KEY,
    nivel    VARCHAR(20)  NOT NULL,
    servicio VARCHAR(255) NOT NULL,
    cantidad INT          NOT NULL
);

-- Los volantes dicen "10 refrescos de 3L" sin sabor: se ligan como refresco de sabor.
INSERT INTO tmp_composicion (nivel, servicio, cantidad) VALUES
('PLUS', 'Mesero(a)',                            1),
('PLUS', 'Vitrolero de agua fresca',             1),
('PLUS', 'Refresco de sabor 3L',                10),
('PLUS', 'Payasita (2 horas)',                   1),
('PLUS', '4 canciones NEÓN',                     1),
('FULL', 'Mesero(a)',                            2),
('FULL', 'Vitrolero de agua fresca',             1),
('FULL', 'Refresco de sabor 3L',                10),
('FULL', 'Payasita (2 horas)',                   1),
('FULL', '4 canciones NEÓN',                     1),
('FULL', 'Bollito',                             80),
('FULL', 'Decoración de mesa principal (4 m)',   1),
('NEON', 'Mesero(a)',                            2),
('NEON', 'Vitrolero de agua fresca',             1),
('NEON', 'Refresco de sabor 3L',                10),
('NEON', 'Payasita (2 horas)',                   1),
('NEON', 'NEÓN libre (todo el evento)',          1),
('NEON', 'Bollito',                             80),
('NEON', 'Decoración de mesa principal (4 m)',   1),
('NEON', 'Calcetín antiderrapante',             50),
('NEON', 'Payasita pintacaritas NEÓN (1 hora)',  1),
('NEON', 'Barra de snack (100 vasos)',           1);

-- Solo se arman los paquetes que todavía no tienen servicios incluidos, para no
-- duplicarlos ni tocar los que ya se hayan editado desde la aplicación.
DROP TEMPORARY TABLE IF EXISTS tmp_sin_servicios;
CREATE TEMPORARY TABLE tmp_sin_servicios (
    nivel      VARCHAR(20) NOT NULL PRIMARY KEY,
    paquete_id BIGINT      NOT NULL
);

INSERT INTO tmp_sin_servicios (nivel, paquete_id)
SELECT i.nivel, i.paquete_id
  FROM tmp_ids_paquetes i
 WHERE NOT EXISTS (SELECT 1 FROM paquetes_servicios ps WHERE ps.paquete_id = i.paquete_id);

-- Las comidas nunca van aquí (van en la tabla de precios), solo extras e incluidos.
INSERT INTO paquetes_servicios (paquete_id, servicio_id, cantidad, subtotal)
SELECT a.paquete_id, s.id, c.cantidad, c.cantidad * s.precio
  FROM tmp_sin_servicios a
  JOIN tmp_composicion c ON c.nivel = a.nivel
  JOIN (SELECT nombre, MIN(id) AS id FROM servicios
         WHERE activo = TRUE AND categoria IN ('EXTRA', 'INCLUIDO') GROUP BY nombre) sid
    ON sid.nombre = c.servicio
  JOIN servicios s ON s.id = sid.id
 ORDER BY a.paquete_id, c.orden;

-- ---------------------------------------------------------------------
-- 4. Tablas de precios (paquetes_precios)
-- ---------------------------------------------------------------------

-- Las 7 comidas, con el nombre de su servicio de categoría COMIDA.
DROP TEMPORARY TABLE IF EXISTS tmp_comidas;
CREATE TEMPORARY TABLE tmp_comidas (
    comida   VARCHAR(20)  NOT NULL PRIMARY KEY,
    servicio VARCHAR(255) NOT NULL
);

INSERT INTO tmp_comidas (comida, servicio) VALUES
('HOTDOG',      'Hot dog con papas'),
('PIZZA',       'Pizza con espagueti (1 rebanada p/p)'),
('VAPOR',       'Tacos a vapor con frijol (sandwichón de pollo con frijol)'),
('TAQUIZA',     'Taquiza (3 guisos y 1 frijol)'),
('BARBACOA',    'Barbacoa'),
('HAMBURGUESA', 'Hamburguesa con papas'),
('ASADA',       'Tacos de asada (3)');

-- Precios de los volantes (viernes a domingo).
DROP TEMPORARY TABLE IF EXISTS tmp_precios;
CREATE TEMPORARY TABLE tmp_precios (
    nivel    VARCHAR(20)   NOT NULL,
    comida   VARCHAR(20)   NOT NULL,
    personas INT           NOT NULL,
    precio   DECIMAL(10,2) NOT NULL
);

INSERT INTO tmp_precios (nivel, comida, personas, precio) VALUES
-- Paquete Plus                60 personas                    80 personas                    100 personas                    120 personas
('PLUS', 'HOTDOG',      60, 14700), ('PLUS', 'HOTDOG',      80, 15300), ('PLUS', 'HOTDOG',      100, 15900), ('PLUS', 'HOTDOG',      120, 16700),
('PLUS', 'PIZZA',       60, 15300), ('PLUS', 'PIZZA',       80, 16100), ('PLUS', 'PIZZA',       100, 16900), ('PLUS', 'PIZZA',       120, 17700),
('PLUS', 'VAPOR',       60, 15500), ('PLUS', 'VAPOR',       80, 16500), ('PLUS', 'VAPOR',       100, 17300), ('PLUS', 'VAPOR',       120, 18300),
('PLUS', 'TAQUIZA',     60, 16400), ('PLUS', 'TAQUIZA',     80, 17700), ('PLUS', 'TAQUIZA',     100, 18700), ('PLUS', 'TAQUIZA',     120, 19900),
('PLUS', 'BARBACOA',    60, 16900), ('PLUS', 'BARBACOA',    80, 17900), ('PLUS', 'BARBACOA',    100, 19100), ('PLUS', 'BARBACOA',    120, 20400),
('PLUS', 'HAMBURGUESA', 60, 17600), ('PLUS', 'HAMBURGUESA', 80, 19300), ('PLUS', 'HAMBURGUESA', 100, 20700), ('PLUS', 'HAMBURGUESA', 120, 22300),
('PLUS', 'ASADA',       60, 19800), ('PLUS', 'ASADA',       80, 21800), ('PLUS', 'ASADA',       100, 23700), ('PLUS', 'ASADA',       120, 25900),
-- Paquete Full
('FULL', 'HOTDOG',      60, 19400), ('FULL', 'HOTDOG',      80, 20000), ('FULL', 'HOTDOG',      100, 20600), ('FULL', 'HOTDOG',      120, 21400),
('FULL', 'PIZZA',       60, 20000), ('FULL', 'PIZZA',       80, 20800), ('FULL', 'PIZZA',       100, 21600), ('FULL', 'PIZZA',       120, 22400),
('FULL', 'VAPOR',       60, 20200), ('FULL', 'VAPOR',       80, 21200), ('FULL', 'VAPOR',       100, 22000), ('FULL', 'VAPOR',       120, 23000),
('FULL', 'TAQUIZA',     60, 22100), ('FULL', 'TAQUIZA',     80, 22400), ('FULL', 'TAQUIZA',     100, 23400), ('FULL', 'TAQUIZA',     120, 24600),
('FULL', 'BARBACOA',    60, 21600), ('FULL', 'BARBACOA',    80, 22600), ('FULL', 'BARBACOA',    100, 23800), ('FULL', 'BARBACOA',    120, 25100),
('FULL', 'HAMBURGUESA', 60, 22300), ('FULL', 'HAMBURGUESA', 80, 24000), ('FULL', 'HAMBURGUESA', 100, 25400), ('FULL', 'HAMBURGUESA', 120, 27100),
('FULL', 'ASADA',       60, 24500), ('FULL', 'ASADA',       80, 26500), ('FULL', 'ASADA',       100, 28400), ('FULL', 'ASADA',       120, 30600),
-- Paquete Neón
('NEON', 'HOTDOG',      60, 26200), ('NEON', 'HOTDOG',      80, 26800), ('NEON', 'HOTDOG',      100, 27400), ('NEON', 'HOTDOG',      120, 28200),
('NEON', 'PIZZA',       60, 26800), ('NEON', 'PIZZA',       80, 27600), ('NEON', 'PIZZA',       100, 28400), ('NEON', 'PIZZA',       120, 29200),
('NEON', 'VAPOR',       60, 27000), ('NEON', 'VAPOR',       80, 28000), ('NEON', 'VAPOR',       100, 28800), ('NEON', 'VAPOR',       120, 29800),
('NEON', 'TAQUIZA',     60, 27900), ('NEON', 'TAQUIZA',     80, 29200), ('NEON', 'TAQUIZA',     100, 30200), ('NEON', 'TAQUIZA',     120, 31400),
('NEON', 'BARBACOA',    60, 28400), ('NEON', 'BARBACOA',    80, 29400), ('NEON', 'BARBACOA',    100, 30600), ('NEON', 'BARBACOA',    120, 31900),
('NEON', 'HAMBURGUESA', 60, 29100), ('NEON', 'HAMBURGUESA', 80, 30800), ('NEON', 'HAMBURGUESA', 100, 32200), ('NEON', 'HAMBURGUESA', 120, 33900),
('NEON', 'ASADA',       60, 31300), ('NEON', 'ASADA',       80, 33300), ('NEON', 'ASADA',       100, 35200), ('NEON', 'ASADA',       120, 37400);

-- Solo paquetes del catálogo que aún no tienen tabla y cuyo precio fijo es 0 (los creó
-- este script). Un paquete con el mismo nombre que ya tuviera precio fijo no se convierte.
DROP TEMPORARY TABLE IF EXISTS tmp_sin_precios;
CREATE TEMPORARY TABLE tmp_sin_precios (
    nivel      VARCHAR(20) NOT NULL PRIMARY KEY,
    paquete_id BIGINT      NOT NULL
);

INSERT INTO tmp_sin_precios (nivel, paquete_id)
SELECT i.nivel, i.paquete_id
  FROM tmp_ids_paquetes i
 WHERE i.nivel <> 'RENTA'
   AND i.costo = 0
   AND NOT EXISTS (SELECT 1 FROM paquetes_precios pp WHERE pp.paquete_id = i.paquete_id);

INSERT INTO paquetes_precios (paquete_id, comida_id, personas, precio)
SELECT a.paquete_id, cid.id, p.personas, p.precio
  FROM tmp_precios p
  JOIN tmp_sin_precios a ON a.nivel = p.nivel
  JOIN tmp_comidas c ON c.comida = p.comida
  JOIN (SELECT nombre, MIN(id) AS id FROM servicios
         WHERE activo = TRUE AND categoria = 'COMIDA' GROUP BY nombre) cid
    ON cid.nombre = c.servicio
 ORDER BY a.paquete_id, cid.id, p.personas;

-- ---------------------------------------------------------------------
-- 5. (Opcional) Dar de baja el catálogo anterior
-- ---------------------------------------------------------------------
-- Si la base tiene servicios o paquetes que ya no se ofrecen (los de los datos de
-- ejemplo, o los 84 paquetes "Paquete X - Comida - N personas" y los servicios
-- "Comida: ..." de la versión anterior de este script), quita los comentarios de
-- las líneas de abajo. Es la misma baja lógica (activo = FALSE) que hace el botón
-- Eliminar de la aplicación, con sus mismas reglas: no da de baja paquetes con
-- cotizaciones abiertas ni servicios que sigan en un paquete activo. Nada se borra
-- y las cotizaciones y eventos anteriores no cambian.
--
-- UPDATE paquetes
--    SET activo = FALSE
--  WHERE activo = TRUE
--    AND nombre NOT IN (SELECT t.nombre FROM tmp_paquetes t)
--    AND NOT EXISTS (SELECT 1
--                      FROM cotizaciones c
--                      LEFT JOIN eventos e ON e.cotizacion_id = c.id
--                     WHERE c.paquete_id = paquetes.id
--                       AND c.estado IN ('BORRADOR', 'VIGENTE')
--                       AND (e.id IS NULL OR e.estado <> 'FINALIZADO'));
--
-- UPDATE servicios
--    SET activo = FALSE
--  WHERE activo = TRUE
--    AND nombre NOT IN (SELECT t.nombre FROM tmp_servicios t)
--    AND NOT EXISTS (SELECT 1
--                      FROM paquetes_servicios ps
--                      JOIN paquetes p ON p.id = ps.paquete_id
--                     WHERE ps.servicio_id = servicios.id
--                       AND p.activo = TRUE)
--    AND NOT EXISTS (SELECT 1
--                      FROM paquetes_precios pp
--                      JOIN paquetes p ON p.id = pp.paquete_id
--                     WHERE pp.comida_id = servicios.id
--                       AND p.activo = TRUE);

COMMIT;

SET SQL_SAFE_UPDATES = @safe_updates_previo;

-- ---------------------------------------------------------------------
-- 6. Resumen
-- ---------------------------------------------------------------------
SELECT s.categoria, COUNT(*) AS servicios_del_catalogo
  FROM tmp_servicios t
  JOIN servicios s ON s.nombre = t.nombre AND s.activo = TRUE
 GROUP BY s.categoria
 ORDER BY s.categoria;

-- Se esperan: Renta de local con precio fijo; Plus, Full y Neón con 28 precios cada uno.
SELECT p.nombre,
       p.costo AS precio_fijo,
       (SELECT COUNT(*) FROM paquetes_precios pp WHERE pp.paquete_id = p.id)   AS precios_en_tabla,
       (SELECT COUNT(*) FROM paquetes_servicios ps WHERE ps.paquete_id = p.id) AS servicios_incluidos
  FROM tmp_ids_paquetes i
  JOIN paquetes p ON p.id = i.paquete_id
 ORDER BY p.id;

-- Servicios del catálogo que ya existían con el mismo nombre pero con otro precio o con
-- otra categoría: el script no los cambió (salvo EXTRA -> INCLUIDO). Una comida que ya
-- existía con otra categoría no entra en las tablas de precios: corrígela en Servicios.
SELECT s.id, s.nombre,
       s.categoria AS categoria_en_sistema, t.categoria AS categoria_volante,
       s.precio AS precio_en_sistema, t.precio AS precio_volante
  FROM tmp_servicios t
  JOIN servicios s ON s.nombre = t.nombre AND s.activo = TRUE
 WHERE s.categoria <> t.categoria
    OR (t.categoria <> 'COMIDA' AND s.precio <> t.precio);

DROP TEMPORARY TABLE IF EXISTS tmp_servicios, tmp_paquetes, tmp_ids_paquetes, tmp_composicion,
                               tmp_sin_servicios, tmp_comidas, tmp_precios, tmp_sin_precios;
