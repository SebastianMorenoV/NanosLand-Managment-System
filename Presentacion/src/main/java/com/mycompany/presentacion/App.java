package com.mycompany.presentacion;

import com.example.negocio.NegocioApplication;
import com.example.negocio.evento.usecase.ActualizarEstadosPorFechaUseCase;
import com.mycompany.presentacion.config.ConfiguracionBD;
import com.mycompany.presentacion.config.ConfiguracionBDDialog;
import com.mycompany.presentacion.config.PrimerUsuarioDialog;
import com.mycompany.presentacion.config.RegistroErrores;
import com.mycompany.presentacion.utils.ViewSwitcher;
import java.io.IOException;
import java.util.Optional;
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

    @Override
    public void start(Stage primaryStage) throws Exception {
        instancia = this;
        this.primaryStage = primaryStage;

        // Antes de arrancar Spring se confirma que hay conexión a MySQL. Si no
        // la hay (primera vez en este equipo, contraseña distinta, servidor
        // apagado) se piden los datos en lugar de fallar al iniciar. Lo mismo
        // si la conexión funciona pero el sistema no logra iniciar con esa base.
        ConfiguracionBD config = ConfiguracionBD.cargar();
        String error = config.probarConexion();
        boolean configuracionNueva = false;
        ConfigurableApplicationContext contexto = null;
        while (contexto == null) {
            if (error != null) {
                Optional<ConfiguracionBD> nueva = ConfiguracionBDDialog.mostrar(null, config, error);
                if (nueva.isEmpty()) {
                    Platform.exit();
                    return;
                }
                config = nueva.get();
                configuracionNueva = true;
            }
            try {
                contexto = arrancarSpring(config);
            } catch (Exception e) {
                e.printStackTrace();
                error = "La conexión funciona, pero el sistema no pudo iniciar con esta base de datos: "
                        + RegistroErrores.causaRaiz(e);
            }
        }
        usarContexto(contexto);
        if (configuracionNueva) {
            guardarConfiguracion(config);
        }

        PrimerUsuarioDialog.mostrarSiNoHayUsuarios(null, springContext);

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
        ConfigurableApplicationContext contextoNuevo;
        try {
            contextoNuevo = arrancarSpring(nueva.get());
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("No se pudo aplicar la nueva conexión",
                    "La conexión funciona, pero el sistema no pudo iniciar con esa base de datos:\n"
                    + RegistroErrores.causaRaiz(e) + "\n\nSe conserva la conexión anterior.");
            return;
        }
        ConfigurableApplicationContext contextoAnterior = app.springContext;
        app.usarContexto(contextoNuevo);
        if (contextoAnterior != null) {
            contextoAnterior.close();
        }
        guardarConfiguracion(nueva.get());
        try {
            PrimerUsuarioDialog.mostrarSiNoHayUsuarios(app.primaryStage, app.springContext);
            app.primaryStage.getScene().setRoot(app.cargarLogin());
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
        try {
            // Contraseñas que sigan en texto plano (usuarios antiguos o cargados por script).
            contexto.getBean(com.example.negocio.sesion.usecase.Contrasenas.class).migrarTextoPlano();
            // Eventos confirmados cuya fecha ya llegó o pasó: en curso / finalizado.
            contexto.getBean(ActualizarEstadosPorFechaUseCase.class).actualizar();
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
