package com.mycompany.presentacion;

import com.example.negocio.NegocioApplication;
import com.example.negocio.evento.usecase.ActualizarEstadosPorFechaUseCase;
import com.mycompany.presentacion.config.ConfiguracionBD;
import com.mycompany.presentacion.config.ConfiguracionBDDialog;
import com.mycompany.presentacion.config.PantallaCarga;
import com.mycompany.presentacion.config.PrimerUsuarioDialog;
import com.mycompany.presentacion.config.RegistroErrores;
import com.mycompany.presentacion.utils.Espera;
import com.mycompany.presentacion.utils.ViewSwitcher;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class App extends Application {

    private static App instancia;

    private ConfigurableApplicationContext springContext;
    private Stage primaryStage;
    private PantallaCarga pantallaCarga;

    @Override
    public void start(Stage primaryStage) {
        instancia = this;
        this.primaryStage = primaryStage;

        // La conexión y Spring tardan varios segundos: se hacen en segundo plano
        // para que la pantalla de carga aparezca de inmediato y no se congele.
        pantallaCarga = new PantallaCarga();
        pantallaCarga.mostrar();
        Thread hilo = new Thread(this::arrancar, "hilo-arranque");
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Arranque del sistema. Corre fuera del hilo de JavaFX; las ventanas se abren con {@link #enHiloFx}. */
    private void arrancar() {
        try {
            // Antes de arrancar Spring se confirma que hay conexión a MySQL. Si no
            // la hay (primera vez en este equipo, contraseña distinta, servidor
            // apagado) se piden los datos en lugar de fallar al iniciar. Lo mismo
            // si la conexión funciona pero el sistema no logra iniciar con esa base.
            pantallaCarga.estado("Conectando a la base de datos…");
            ConfiguracionBD config = ConfiguracionBD.cargar();
            String error = config.probarConexion();
            boolean configuracionNueva = false;
            ConfigurableApplicationContext contexto = null;
            while (contexto == null) {
                if (error != null) {
                    pantallaCarga.estado("Esperando los datos de conexión…");
                    ConfiguracionBD actual = config;
                    String aviso = error;
                    Optional<ConfiguracionBD> nueva = enHiloFx(() ->
                            ConfiguracionBDDialog.mostrar(pantallaCarga.ventana(), actual, aviso));
                    if (nueva.isEmpty()) {
                        Platform.exit();
                        return;
                    }
                    config = nueva.get();
                    configuracionNueva = true;
                }
                pantallaCarga.estado("Iniciando el sistema…");
                try {
                    contexto = arrancarSpring(config);
                } catch (Exception e) {
                    e.printStackTrace();
                    error = "La conexión funciona, pero el sistema no pudo iniciar con esta base de datos: "
                            + RegistroErrores.causaRaiz(e);
                }
            }
            pantallaCarga.estado("Preparando la información…");
            usarContexto(contexto);

            ConfiguracionBD configFinal = config;
            boolean guardar = configuracionNueva;
            enHiloFx(() -> {
                if (guardar) {
                    guardarConfiguracion(configFinal);
                }
                PrimerUsuarioDialog.mostrarSiNoHayUsuarios(pantallaCarga.ventana(), springContext);
                mostrarVentanaPrincipal();
                return null;
            });
        } catch (Throwable e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                pantallaCarga.cerrar();
                mostrarError("No se pudo iniciar el sistema", RegistroErrores.causaRaiz(e)
                        + "\n\nEl detalle quedó guardado en:\n" + RegistroErrores.rutaArchivo());
                Platform.exit();
            });
        }
    }

    /** Ejecuta {@code accion} en el hilo de JavaFX y espera su resultado. */
    private static <T> T enHiloFx(Callable<T> accion) throws Exception {
        FutureTask<T> tarea = new FutureTask<>(accion);
        Platform.runLater(tarea);
        try {
            return tarea.get();
        } catch (ExecutionException e) {
            throw e.getCause() instanceof Exception causa ? causa : e;
        }
    }

    private void mostrarVentanaPrincipal() throws IOException {
        Scene scene = new Scene(cargarLogin(), 1280, 720);

        primaryStage.setTitle("Sistema de Gestión NanosLand");
        try {
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/Logo Nanos.png")));
        } catch (RuntimeException e) {
            // Sin ícono la aplicación sigue funcionando.
        }
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.setFullScreenExitHint("Presiona F11 o ESC para salir de pantalla completa");

        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F11) {
                primaryStage.setFullScreen(!primaryStage.isFullScreen());
            }
        });

        primaryStage.show();
        // Si mientras cargaba el usuario pasó a otra ventana, Windows deja la nueva
        // detrás; se trae al frente para que se note que el sistema ya abrió.
        primaryStage.setAlwaysOnTop(true);
        primaryStage.setAlwaysOnTop(false);
        primaryStage.toFront();
        // Se cierra después de mostrar la principal: así nunca quedan cero ventanas
        // abiertas (JavaFX terminaría la aplicación).
        pantallaCarga.cerrar();

        // Mínimos por debajo de 1280x720 para que la ventana pueda reducirse en
        // laptops pequeñas sin quedar bloqueada. El contenido sigue siendo
        // accesible gracias al scroll que añade ViewSwitcher a cada vista.
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(620);
    }

    /**
     * Abre la ventana de conexión a la base de datos desde la pantalla de
     * inicio de sesión. Si el usuario indica datos nuevos, el sistema arranca
     * con esa conexión y vuelve a mostrar el inicio de sesión.
     *
     * El sistema nuevo se arranca ANTES de cerrar el actual y de guardar la
     * configuración: si no logra iniciar, todo queda como estaba.
     */
    public static void configurarConexion() {
        App app = instancia;
        if (app == null) {
            return;
        }
        Optional<ConfiguracionBD> nueva = ConfiguracionBDDialog.mostrar(
                app.primaryStage, ConfiguracionBD.cargar(), null);
        if (nueva.isEmpty()) {
            return;
        }
        // Arrancar el sistema con la nueva base tarda varios segundos.
        Espera.ejecutar(app.primaryStage.getScene().getRoot(), "Conectando con la nueva base de datos…",
                () -> arrancarSpring(nueva.get()),
                contextoNuevo -> app.aplicarConexion(contextoNuevo, nueva.get()),
                error -> mostrarError("No se pudo aplicar la nueva conexión",
                        "La conexión funciona, pero el sistema no pudo iniciar con esa base de datos:\n"
                        + RegistroErrores.causaRaiz(error) + "\n\nSe conserva la conexión anterior."));
    }

    /** Cambia al sistema ya arrancado con la nueva conexión y vuelve al inicio de sesión. */
    private void aplicarConexion(ConfigurableApplicationContext contextoNuevo, ConfiguracionBD config) {
        ConfigurableApplicationContext contextoAnterior = springContext;
        usarContexto(contextoNuevo);
        if (contextoAnterior != null) {
            contextoAnterior.close();
        }
        guardarConfiguracion(config);
        try {
            PrimerUsuarioDialog.mostrarSiNoHayUsuarios(primaryStage, springContext);
            primaryStage.getScene().setRoot(cargarLogin());
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("No se pudo mostrar el inicio de sesión",
                    RegistroErrores.causaRaiz(e) + "\n\nCierra el sistema y vuelve a abrirlo.");
        }
    }

    private static ConfigurableApplicationContext arrancarSpring(ConfiguracionBD config) {
        return new SpringApplicationBuilder()
                .sources(NegocioApplication.class)
                .sources(PresentacionConfig.class)
                // Spring Boot activa por defecto el modo "sin pantalla" de AWT, con el que
                // java.awt.Desktop no puede abrir los PDF generados (contrato, comprobante).
                .headless(false)
                .run(config.argumentosSpring());
    }

    private void usarContexto(ConfigurableApplicationContext contexto) {
        springContext = contexto;
        ViewSwitcher.setSpringContext(contexto);
        // Cada corrección va por separado: si una falla, las demás se siguen aplicando.
        // Contraseñas que sigan en texto plano (usuarios antiguos o cargados por script).
        correccionAlArrancar(() -> contexto.getBean(com.example.negocio.sesion.usecase.Contrasenas.class).migrarTextoPlano());
        // Ventas sin autor (usuario eliminado): la columna era obligatoria en bases anteriores.
        correccionAlArrancar(() -> contexto.getBean(
                com.example.negocio.esquema.CorregirEsquemaUseCase.class).permitirVentasSinUsuario());
        // Eventos confirmados cuya fecha ya llegó o pasó: en curso / finalizado.
        correccionAlArrancar(() -> contexto.getBean(ActualizarEstadosPorFechaUseCase.class).actualizar());
        // Cotizaciones de antes del precio pactado del paquete.
        correccionAlArrancar(() -> contexto.getBean(
                com.example.negocio.cotizacion.usecase.CompletarPrecioPaqueteUseCase.class).completar());
        // Logística de antes: lo del paquete y los extras de un mismo servicio iban en un solo registro.
        correccionAlArrancar(() -> contexto.getBean(
                com.example.negocio.logistica.usecase.SepararLogisticaExtrasUseCase.class).separar());
    }

    private static void correccionAlArrancar(Runnable correccion) {
        try {
            correccion.run();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private static void guardarConfiguracion(ConfiguracionBD config) {
        try {
            config.guardar();
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("No se pudo guardar la configuración",
                    "El sistema funciona con la conexión indicada, pero no se pudo guardar en "
                    + ConfiguracionBD.rutaArchivo() + ":\n" + e.getMessage()
                    + "\n\nLa próxima vez habrá que capturarla de nuevo.");
        }
    }

    private Parent cargarLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/presentacion/views/Login.fxml"));
        loader.setControllerFactory(springContext::getBean);
        return loader.load();
    }

    private static void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(titulo);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @Override
    public void stop() throws Exception {
        if (springContext != null) {
            springContext.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
