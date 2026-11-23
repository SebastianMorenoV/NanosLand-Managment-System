# NanosLand — Sistema de Gestión

Aplicación de escritorio para administrar el negocio de **NanosLand**, un salón de fiestas y eventos infantiles. Centraliza en un solo lugar lo que normalmente se lleva en libretas y hojas de cálculo: clientes, cotizaciones, agenda de eventos, pagos y reportes.

## ¿Qué hace?

- **Inicio de sesión y usuarios**: acceso con correo y contraseña, con roles de *Dueño* y *Administrador*, y gestión de las cuentas del sistema.
- **Clientes**: registro y consulta de clientes con sus datos de contacto y dirección.
- **Catálogo de servicios y paquetes**: alta, edición y baja de servicios (shows, brincolines, decoración, fotografía, etc.) y de paquetes que agrupan varios servicios.
- **Cotizaciones**: armado de una cotización para un cliente a partir de paquetes y servicios, con selección de fecha y turno según la disponibilidad de la agenda.
- **Eventos**: agenda de eventos, consulta de detalles, cancelación y reprogramación.
- **Logística**: seguimiento del estado de cada servicio contratado para un evento.
- **Estado de cuenta**: pagos recibidos, cargos extra y saldo pendiente de cada evento.
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
├── sql/            Scripts con datos de ejemplo
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

2. Pon tu usuario y contraseña de MySQL en los dos archivos de configuración (deben coincidir):

   - `negocio/src/main/resources/application.properties`
   - `Persistencia/src/main/resources/application.properties`

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

6. Inicia sesión con uno de los usuarios de ejemplo:

   | Rol | Correo | Contraseña |
   |---|---|---|
   | Dueño | `dueno@nanosland.com` | `dueno123` |
   | Administrador | `admin@nanosland.com` | `admin123` |

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

El workflow `.github/workflows/jdeploy.yml` compila el proyecto y sube los instaladores al release de ese tag. El nombre del tag no puede pasar de 16 caracteres.

En el equipo donde se instala solo hace falta tener MySQL; el instalador incluye su propio Java.
