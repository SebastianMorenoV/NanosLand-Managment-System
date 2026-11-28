# NanosLand — Sistema de Gestión

Aplicación de escritorio para administrar el negocio de **NanosLand**, un salón de fiestas y eventos infantiles. Centraliza en un solo lugar lo que normalmente se lleva en libretas y hojas de cálculo: clientes, cotizaciones, agenda de eventos, pagos y reportes.

## ¿Qué hace?

- **Inicio**: resumen de lo que hay hoy, los próximos 7 días y los pendientes (cobrar, confirmar, contactar servicios).
- **Inicio de sesión y usuarios**: acceso con correo y contraseña, con roles de *Dueño* y *Administrador*, y gestión de las cuentas del sistema: alta, edición, desactivar o reactivar, y eliminar definitivamente (las cotizaciones y ventas del usuario se conservan). El sistema nunca se queda sin un dueño activo. Cada usuario puede cambiar su contraseña escribiendo la actual; el dueño además puede cambiar la de los demás sin conocerla (por ejemplo, si la olvidaron).
- **Clientes**: registro y consulta de clientes con sus datos de contacto y dirección.
- **Catálogo de servicios y paquetes**: alta, edición y baja de servicios y de paquetes que agrupan varios servicios. Cada servicio tiene una categoría: *Extra* (se vende aparte), *Incluido en paquete* (solo va dentro de los paquetes) o *Comida*. Un paquete tiene precio fijo (como la renta del local) o una tabla de precios por comida y número de personas (como los paquetes Plus, Full y Neón).
- **Descuentos** (dueño y administrador): rebajas automáticas al precio de los paquetes, por porcentaje o monto fijo, según el día de la semana del evento (p. ej. de lunes a jueves) o una temporada (rango de fechas), para todos los paquetes o solo algunos, con una fecha límite opcional para cotizar. Si a una cotización le tocan varios, se aplica el que más rebaja. Se pueden pausar, borrar o repetir el año siguiente.
- **Cotizaciones**: armado de una cotización para un cliente a partir de un paquete (con su comida y número de personas, si aplica) y servicios extra, con selección de fecha y turno según la disponibilidad de la agenda. El paquete lleva el descuento que toque por la fecha del evento. Cada cotización guarda el precio pactado del paquete y su descuento, y una copia de lo que incluía el paquete (nombre, descripción y servicios incluidos): si después cambia el paquete en el catálogo (precios, servicios, descripción) o los descuentos, las cotizaciones hechas conservan lo que se les vendió en su contrato y su logística, y los cambios aplican a las cotizaciones nuevas.
- **Lista de cotizaciones**: por confirmar, confirmadas, vencidas y canceladas, desde donde se abren para editarlas, cobrar o generar el contrato.
- **Eventos**: eventos confirmados, en curso y realizados, con detalle, cancelación y reprogramación.
- **Logística**: seguimiento del estado de cada servicio contratado para un evento. Lo que incluye el paquete y cada servicio extra van en registros separados, cada uno con su cantidad y sus datos (hora, ubicación, especificaciones), aunque sea el mismo servicio (p. ej. los bollitos del paquete y 100 bollitos extra temáticos).
- **Estado de cuenta**: pagos recibidos, cargos extra y saldo pendiente de cada evento.
- **Ventas extra**: venta de productos aparte de los eventos (por ejemplo, calcetines), que cuenta como ingreso del negocio sin afectar a ningún evento.
- **Reportes**: ingresos, adeudos, eventos y oportunidades de recompra, con exportación a PDF.

## Tecnologías

| Área | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Interfaz gráfica | JavaFX 21 (FXML + CSS) y ControlsFX |
| Lógica de negocio | Spring Boot 3 (inyección de dependencias, casos de uso) |
| Acceso a datos | Spring Data JPA / Hibernate |
| Base de datos | MySQL |
| Reportes | OpenPDF y JFreeChart |
| Utilidades | Lombok |
| Construcción | Maven (proyecto multimódulo) |

## Estructura del proyecto

El sistema sigue una arquitectura por capas, con un módulo de Maven para cada una:

```
├── Common/         DTOs y mappers compartidos entre capas
├── Persistencia/   Entidades JPA, enums y repositorios
├── negocio/        Casos de uso (reglas del negocio)
├── Presentacion/   Aplicación JavaFX: vistas FXML, controladores y estilos
├── sql/            Scripts de datos: ejemplo y catálogo real del local
└── pom.xml         POM padre que agrupa los módulos
```

La capa de presentación arranca el contexto de Spring y obtiene de él los controladores, así que las vistas usan directamente los casos de uso de `negocio`, y estos a su vez los repositorios de `Persistencia`.

## Cómo ejecutarlo

### Requisitos

- JDK 17 o superior (probado con JDK 17 y JDK 21), con `JAVA_HOME` apuntando a él
- Maven
- MySQL 8 en `localhost:3306`

### Pasos

1. Crea la base de datos vacía:

   ```sql
   CREATE DATABASE nanosland_db;
   ```

2. Pon tu usuario y contraseña de MySQL en `negocio/src/main/resources/application.properties`. Si no coinciden con tu MySQL, la aplicación te los pedirá al abrir.

3. Compila todos los módulos desde la raíz del repositorio:

   ```bash
   mvn clean install
   ```

4. Inicia la aplicación, también desde la raíz:

   ```bash
   mvn -pl Presentacion javafx:run
   ```

   Se abre la ventana de inicio de sesión. En este primer arranque la aplicación crea sola las tablas en `nanosland_db`.

5. Carga los datos de ejemplo. Las tablas ya deben existir (paso 4), porque el script solo inserta registros:

   ```bash
   mysql -u root -p --default-character-set=utf8mb4 < sql/datos_iniciales.sql
   ```

   El script está pensado para una base recién creada; ejecútalo una sola vez.

   Para cargar el catálogo real del local (servicios, paquetes y tablas de precios de los volantes vigentes) usa `sql/catalogo_paquetes_servicios.sql`. Ejecútalo después de abrir la aplicación al menos una vez con la versión actual, para que existan las tablas nuevas. Funciona en una base que ya tiene datos y se puede ejecutar más de una vez, porque solo inserta lo que falta. Al final tiene una sección opcional, comentada, para dar de baja el catálogo anterior:

   ```bash
   mysql -u root -p --default-character-set=utf8mb4 < sql/catalogo_paquetes_servicios.sql
   ```

   Los extras de decoración y animación (hoja "EXTRAS": mamparas, alfombra roja, payasitas, pintacaritas, show del Payasito Kiki...) van en un script aparte, `sql/catalogo_extras_decoracion_animacion.sql`. También solo inserta lo que falta y se puede ejecutar más de una vez:

   ```bash
   mysql -u root -p --default-character-set=utf8mb4 < sql/catalogo_extras_decoracion_animacion.sql
   ```

6. Inicia sesión con uno de los usuarios de ejemplo:

   | Rol | Correo | Contraseña |
   |---|---|---|
   | Dueño | `dueno@nanosland.com` | `dueno123` |
   | Administrador | `admin@nanosland.com` | `admin123` |

   Las contraseñas se guardan cifradas (hash BCrypt): la aplicación convierte las del script la siguiente vez que se abre. En una base sin usuarios, al abrir se pide crear la cuenta del dueño.

Si cambias el código, repite el paso 3 antes de volver a iniciar la aplicación.

## Conexión a la base de datos

La aplicación guarda los datos de conexión a MySQL de cada equipo en un archivo propio del usuario (`%APPDATA%\NanosLand\conexion.properties` en Windows), fuera del programa, así que las actualizaciones no los tocan.

- Si al abrir no logra conectarse (por ejemplo, la primera vez en un equipo nuevo), muestra la ventana **Conexión a la base de datos** para capturar servidor, puerto, base de datos, usuario y contraseña.
- También se puede abrir desde la pantalla de inicio de sesión, con el botón **Configurar conexión a la base de datos**.
- Si la base de datos indicada no existe, se crea sola junto con sus tablas.

Mientras ese archivo no exista se usan los valores de `application.properties`.

## Instalador y actualizaciones

El instalador se genera con [jDeploy](https://www.jdeploy.com/) y se publica en GitHub Releases. La aplicación instalada busca versiones nuevas al abrirse y se actualiza sola, sin desinstalar nada.

Para publicar una versión:

```bash
git tag v1.0.0
git push origin v1.0.0
```

El workflow `.github/workflows/jdeploy.yml` compila el proyecto, corre las pruebas y sube los instaladores al release de ese tag. El nombre del tag no puede pasar de 16 caracteres.

Antes de compilar, el workflow verifica el número de versión (`.github/scripts/verificar-version.sh`) y no publica si:

- no tiene el formato `v1.2.3`;
- es menor que la última versión (las versiones siempre van hacia arriba: después de `v1.5.0`, una corrección es `v1.5.1`, nunca `v1.4.1`);
- ya se publicó antes (no se reutilizan versiones: quien ya la instaló no la vuelve a descargar, así que una corrección siempre lleva un número nuevo).

También se puede revisar un número antes de crear el tag: `bash .github/scripts/verificar-version.sh v1.6.0`.

En el equipo donde se instala solo hace falta tener MySQL; el instalador incluye su propio Java.
