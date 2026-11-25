USE nanosland_db;
SET NAMES utf8mb4;

-- =====================================================================
-- EXTRAS DE DECORACIÓN Y ANIMACIÓN (volante "EXTRAS", noviembre 2026)
-- =====================================================================
-- Complementa a catalogo_paquetes_servicios.sql con los extras de la hoja de
-- decoración y animación, que faltaban: 17 servicios de categoría EXTRA (se
-- venden aparte y aparecen en la lista de extras de la cotización).
--
--   * Decoración: mesa principal, decoración maxi, alfombra roja, centro de
--     mesa y lo que se "podría agregar además" (mamparas, guía de globos e
--     imagen de coroplast).
--   * Animación (no incluye premios): payasitas ARCOÍRIS, pintacaritas NEÓN
--     MIAU / CHIPI / ARCOÍRIS y el show del PAYASITO KIKI.
--
-- No toca nada de lo que ya existe. En particular, los servicios INCLUIDOS en
-- los paquetes ("Decoración de mesa principal (4 m)", "Payasita (2 horas)" y
-- "Payasita pintacaritas NEÓN (1 hora)") se quedan como están: son lo que trae
-- cada paquete, y estos son los que se venden aparte.
--
-- Se puede ejecutar más de una vez: solo inserta los que faltan (busca por
-- nombre entre los servicios activos). Al final muestra un resumen y los
-- servicios activos con nombres parecidos, por si alguno ya se había dado de
-- alta a mano desde la aplicación con otro nombre (en ese caso, dé de baja el
-- que sobre desde la pantalla Servicios).
--
-- Uso:
--   mysql -u root -p --default-character-set=utf8mb4 < sql/catalogo_extras_decoracion_animacion.sql
-- =====================================================================

-- Si esta consulta falla, la base todavía no tiene la columna de categoría:
-- abre primero la versión nueva de la aplicación.
SELECT categoria FROM servicios LIMIT 0;

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_extras;
CREATE TEMPORARY TABLE tmp_extras (
    orden       INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(255)  NOT NULL,
    descripcion VARCHAR(255)  NOT NULL,
    precio      DECIMAL(10,2) NOT NULL
);

INSERT INTO tmp_extras (nombre, descripcion, precio) VALUES
-- Decoración
('Decoración mesa principal (2 mamparas, 4 m)',     'Decoración de la mesa principal con 2 mamparas, 4 metros.',                                              2900),
('Decoración maxi (3 a 5 mamparas, 6 m)',           'Decoración maxi de 3 a 5 mamparas, 6 metros.',                                                           4800),
('Alfombra roja (5 m, bastones y lona 1.8 m)',      'Alfombra roja de 5 metros, bastones y lona de 1.8 metros.',                                              2000),
('Centro de mesa (globos y base de acrílico)',      'Centro de mesa con 1 globo metalizado, 3 de látex y base de acrílico. Sujeto a disponibilidad. Precio por pieza.', 110),
-- Decoración: se podría agregar además
('Mampara en puerta de entrada',                    'Mampara personalizada en el exterior de la puerta de entrada, con 2 medias guías.',                      1800),
('Mampara de recepción (recibidor)',                'Mampara personalizada en la recepción como recibidor, con guía de globos.',                             1200),
('Mampara de repisas con media guía de globos',     'Mampara de repisas y guía media de globos.',                                                              900),
('Guía de globos',                                  'Guía de globos.',                                                                                         500),
('Renta de imagen de coroplast',                    'Renta de imagen de coroplast. Sujeto a disponibilidad.',                                                  400),
-- Animación (no incluye premios)
('Payasita ARCOÍRIS (2 horas)',                     '1 payasita ARCOÍRIS durante 2 horas: juegos y actividades. No incluye premios.',                          850),
('Payasita ARCOÍRIS (3 horas) con pintacaritas',    '1 payasita ARCOÍRIS durante 3 horas: 1 hora de pintacaritas y 2 horas de juegos y actividades. No incluye premios.', 1200),
('2 payasitas ARCOÍRIS (2 horas)',                  '2 payasitas ARCOÍRIS durante 2 horas: juegos y actividades. No incluye premios.',                        1700),
('2 payasitas ARCOÍRIS (3 horas) con pintacaritas', '2 payasitas ARCOÍRIS durante 3 horas: 1 hora de pintacaritas y 2 horas de juegos y actividades. No incluye premios.', 2100),
('Pintacaritas NEÓN MIAU (1 hora)',                 'Pintacaritas NEÓN MIAU durante 1 hora.',                                                                 1500),
('Pintacaritas NEÓN CHIPI (1 hora)',                'Pintacaritas NEÓN CHIPI durante 1 hora.',                                                                1300),
('Pintacaritas NEÓN ARCOÍRIS (2 horas)',            'Pintacaritas NEÓN ARCOÍRIS durante 2 horas.',                                                            1600),
('Show PAYASITO KIKI',                              'Show de malabares, globoflexia, juegos y regalos con el PAYASITO KIKI.',                                 4000);

-- Cuáles ya estaban antes de este script (para el resumen).
DROP TEMPORARY TABLE IF EXISTS tmp_ya_existian;
CREATE TEMPORARY TABLE tmp_ya_existian (nombre VARCHAR(255) NOT NULL PRIMARY KEY);
INSERT INTO tmp_ya_existian (nombre)
SELECT DISTINCT t.nombre
  FROM tmp_extras t
  JOIN servicios s ON s.nombre = t.nombre AND s.activo = TRUE;

INSERT INTO servicios (nombre, descripcion, precio, imagen, activo, categoria)
SELECT t.nombre, t.descripcion, t.precio, NULL, TRUE, 'EXTRA'
  FROM tmp_extras t
 WHERE NOT EXISTS (SELECT 1 FROM servicios s WHERE s.activo = TRUE AND s.nombre = t.nombre)
 ORDER BY t.orden;

COMMIT;

-- ---------------------------------------------------------------------
-- Resumen
-- ---------------------------------------------------------------------
-- Cada extra de la hoja y qué pasó con él. Se esperan 17 renglones.
SELECT t.orden,
       t.nombre,
       t.precio AS precio_volante,
       s.precio AS precio_en_sistema,
       s.categoria,
       CASE WHEN y.nombre IS NULL THEN 'Agregado' ELSE 'Ya existía (no se tocó)' END AS resultado
  FROM tmp_extras t
  JOIN servicios s ON s.nombre = t.nombre AND s.activo = TRUE
  LEFT JOIN tmp_ya_existian y ON y.nombre = t.nombre
 ORDER BY t.orden;

-- Otros servicios activos con nombres parecidos (pueden ser los incluidos en los
-- paquetes, que está bien, o un extra dado de alta a mano que ahora sobra).
SELECT s.id, s.nombre, s.categoria, s.precio
  FROM servicios s
 WHERE s.activo = TRUE
   AND s.nombre NOT IN (SELECT t.nombre FROM tmp_extras t)
   AND (s.nombre LIKE '%decoraci%' OR s.nombre LIKE '%mampara%' OR s.nombre LIKE '%alfombra%'
     OR s.nombre LIKE '%centro de mesa%' OR s.nombre LIKE '%globo%' OR s.nombre LIKE '%coroplast%'
     OR s.nombre LIKE '%payasit%' OR s.nombre LIKE '%pintacaritas%' OR s.nombre LIKE '%kiki%')
 ORDER BY s.nombre;

DROP TEMPORARY TABLE IF EXISTS tmp_extras, tmp_ya_existian;
