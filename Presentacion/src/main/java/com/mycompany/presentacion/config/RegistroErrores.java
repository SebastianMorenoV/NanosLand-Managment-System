package com.mycompany.presentacion.config;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.control.Alert;

/**
 * Registro de errores en disco.
 *
 * La aplicación instalada no tiene consola, así que todo lo que se escribe en
 * la salida estándar y de errores (trazas de excepciones, avisos de Spring e
 * Hibernate) se copia además a un archivo que soporte puede revisar:
 * {@code %APPDATA%\NanosLand\logs\nanosland.log} (junto a conexion.properties).
 */
public final class RegistroErrores {

    private static final long TAMANO_MAXIMO = 5L * 1024 * 1024;
    private static final AtomicBoolean avisoAbierto = new AtomicBoolean(false);

    private RegistroErrores() {
    }

    /** Archivo donde se guarda el registro. */
    public static Path rutaArchivo() {
        return ConfiguracionBD.rutaArchivo().resolveSibling("logs").resolve("nanosland.log");
    }

    /**
     * Empieza a copiar la salida al archivo e instala el manejador de errores
     * no controlados. Debe llamarse al inicio de {@code main}, antes de
     * arrancar JavaFX y Spring. Si el archivo no se puede abrir, la aplicación
     * sigue funcionando sin registro.
     */
    public static void instalar() {
        try {
            Path ruta = rutaArchivo();
            Files.createDirectories(ruta.getParent());
            // Se conserva solo el archivo anterior para que no crezca sin límite.
            if (Files.exists(ruta) && Files.size(ruta) > TAMANO_MAXIMO) {
                Files.move(ruta, ruta.resolveSibling("nanosland.anterior.log"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            OutputStream archivo = new FileOutputStream(ruta.toFile(), true);
            System.setOut(new PrintStream(new Duplicado(System.out, archivo), true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(new Duplicado(System.err, archivo), true, StandardCharsets.UTF_8));
            System.out.println();
            System.out.println("===== Inicio del sistema: " + LocalDateTime.now()
                    + " | versión " + System.getProperty("jdeploy.app.version", "desarrollo") + " =====");
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }

        Thread.setDefaultUncaughtExceptionHandler((hilo, error) -> {
            System.err.println("Error no controlado en el hilo " + hilo.getName() + " (" + LocalDateTime.now() + "):");
            error.printStackTrace();
            avisar("Ocurrió un error inesperado", error);
        });
    }

    /**
     * Muestra un aviso de error al usuario, con la causa y la ubicación del
     * registro. Puede llamarse desde cualquier hilo. Solo se muestra un aviso
     * a la vez, para que un error repetido no llene la pantalla de ventanas.
     */
    public static void avisar(String titulo, Throwable error) {
        if (!avisoAbierto.compareAndSet(false, true)) {
            return;
        }
        Runnable mostrar = () -> {
            try {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(titulo);
                alert.setContentText(causaRaiz(error)
                        + "\n\nEl detalle quedó guardado en:\n" + rutaArchivo());
                alert.showAndWait();
            } finally {
                avisoAbierto.set(false);
            }
        };
        try {
            if (Platform.isFxApplicationThread()) {
                mostrar.run();
            } else {
                Platform.runLater(mostrar);
            }
        } catch (RuntimeException e) {
            // JavaFX todavía no arrancó (o ya terminó): el error queda solo en el archivo.
            avisoAbierto.set(false);
        }
    }

    /** Mensaje de la causa original de un error, sin los envoltorios intermedios. */
    public static String causaRaiz(Throwable error) {
        Throwable causa = error;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        return causa.getMessage() != null ? causa.getMessage() : causa.toString();
    }

    /** Escribe cada byte en la salida original y en el archivo. */
    private static final class Duplicado extends OutputStream {
        private final OutputStream original;
        private final OutputStream archivo;

        Duplicado(OutputStream original, OutputStream archivo) {
            this.original = original;
            this.archivo = archivo;
        }

        @Override
        public void write(int b) throws IOException {
            original.write(b);
            escribirArchivo(() -> archivo.write(b));
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            original.write(b, off, len);
            escribirArchivo(() -> archivo.write(b, off, len));
        }

        @Override
        public void flush() throws IOException {
            original.flush();
            escribirArchivo(archivo::flush);
        }

        private interface Escritura {
            void ejecutar() throws IOException;
        }

        /** Un fallo al escribir el registro (disco lleno, archivo bloqueado) no debe tumbar la aplicación. */
        private void escribirArchivo(Escritura escritura) {
            synchronized (archivo) {
                try {
                    escritura.ejecutar();
                } catch (IOException e) {
                    // Se ignora: la salida original ya recibió el texto.
                }
            }
        }
    }
}
