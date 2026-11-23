package com.mycompany.presentacion.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Datos de conexión a MySQL del equipo donde está instalada la aplicación.
 *
 * Se guardan fuera del programa, en un archivo del usuario, para que cada
 * instalación tenga sus propias credenciales y las actualizaciones de la
 * aplicación no las sobrescriban:
 * <ul>
 *   <li>Windows: {@code %APPDATA%\NanosLand\conexion.properties}</li>
 *   <li>Otros sistemas: {@code ~/.nanosland/conexion.properties}</li>
 * </ul>
 * Si el archivo no existe se usan como punto de partida los valores del
 * {@code application.properties} incluido en la aplicación.
 */
public record ConfiguracionBD(String host, int puerto, String baseDatos, String usuario, String contrasena) {

    private static final String ARCHIVO = "conexion.properties";
    private static final Pattern URL_MYSQL = Pattern.compile("jdbc:mysql://([^:/?]+)(?::(\\d+))?/([^?]+).*");

    public static ConfiguracionBD porDefecto() {
        return new ConfiguracionBD("localhost", 3306, "nanosland_db", "root", "");
    }

    /** Ubicación del archivo de configuración de este equipo. */
    public static Path rutaArchivo() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Paths.get(appData, "NanosLand", ARCHIVO);
        }
        return Paths.get(System.getProperty("user.home"), ".nanosland", ARCHIVO);
    }

    /**
     * Carga la configuración guardada en este equipo; si todavía no existe,
     * toma los valores incluidos en la aplicación.
     */
    public static ConfiguracionBD cargar() {
        Path ruta = rutaArchivo();
        if (Files.isRegularFile(ruta)) {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(ruta)) {
                p.load(in);
                ConfiguracionBD def = porDefecto();
                return new ConfiguracionBD(
                        p.getProperty("host", def.host()).trim(),
                        parsePuerto(p.getProperty("puerto"), def.puerto()),
                        p.getProperty("baseDatos", def.baseDatos()).trim(),
                        p.getProperty("usuario", def.usuario()).trim(),
                        p.getProperty("contrasena", def.contrasena()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return desdeAplicacion();
    }

    /** Guarda la configuración en el archivo del usuario. */
    public void guardar() throws IOException {
        Path ruta = rutaArchivo();
        Files.createDirectories(ruta.getParent());
        Properties p = new Properties();
        p.setProperty("host", host);
        p.setProperty("puerto", String.valueOf(puerto));
        p.setProperty("baseDatos", baseDatos);
        p.setProperty("usuario", usuario);
        p.setProperty("contrasena", contrasena);
        try (OutputStream out = Files.newOutputStream(ruta)) {
            p.store(out, "Conexion a MySQL del Sistema de Gestion NanosLand");
        }
    }

    /** URL JDBC; crea la base de datos si todavía no existe en el servidor. */
    public String urlJdbc() {
        return "jdbc:mysql://" + host + ":" + puerto + "/" + baseDatos
                + "?serverTimezone=UTC&createDatabaseIfNotExist=true";
    }

    /** Argumentos con los que se arranca Spring para usar esta conexión. */
    public String[] argumentosSpring() {
        return new String[] {
                "--spring.datasource.url=" + urlJdbc(),
                "--spring.datasource.username=" + usuario,
                "--spring.datasource.password=" + contrasena
        };
    }

    /**
     * Intenta conectarse con estos datos.
     *
     * @return {@code null} si la conexión funcionó, o un mensaje para el
     *         usuario que explica por qué falló.
     */
    public String probarConexion() {
        try (Connection c = DriverManager.getConnection(
                urlJdbc() + "&connectTimeout=5000", usuario, contrasena)) {
            return null;
        } catch (SQLException e) {
            switch (e.getErrorCode()) {
                case 1045:
                    return "Usuario o contraseña incorrectos.";
                case 1044:
                case 1049:
                    return "El usuario no tiene permiso sobre la base de datos \"" + baseDatos
                            + "\" o no puede crearla.";
                default:
                    if (e.getErrorCode() == 0) {
                        return "No se pudo conectar con MySQL en " + host + ":" + puerto
                                + ". Verifica que el servidor esté encendido y que los datos sean correctos.";
                    }
                    return "MySQL rechazó la conexión: " + e.getMessage();
            }
        }
    }

    private static ConfiguracionBD desdeAplicacion() {
        ConfiguracionBD def = porDefecto();
        try (InputStream in = ConfiguracionBD.class.getResourceAsStream("/application.properties")) {
            if (in == null) {
                return def;
            }
            Properties p = new Properties();
            p.load(in);
            Matcher m = URL_MYSQL.matcher(p.getProperty("spring.datasource.url", ""));
            if (!m.matches()) {
                return def;
            }
            return new ConfiguracionBD(
                    m.group(1),
                    parsePuerto(m.group(2), def.puerto()),
                    m.group(3),
                    p.getProperty("spring.datasource.username", def.usuario()),
                    p.getProperty("spring.datasource.password", def.contrasena()));
        } catch (IOException e) {
            return def;
        }
    }

    private static int parsePuerto(String valor, int porDefecto) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (RuntimeException e) {
            return porDefecto;
        }
    }
}
