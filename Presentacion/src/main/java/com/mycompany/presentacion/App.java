package com.mycompany.presentacion;

import com.example.negocio.NegocioApplication;
import com.mycompany.presentacion.config.ConfiguracionBD;
import com.mycompany.presentacion.config.ConfiguracionBDDialog;
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
        // apagado) se piden los datos en lugar de fallar al iniciar.
        ConfiguracionBD config = ConfiguracionBD.cargar();
        String error = config.probarConexion();
        while (error != null) {
            Optional<ConfiguracionBD> nueva = ConfiguracionBDDialog.mostrar(null, config, error);
            if (nueva.isEmpty()) {
                Platform.exit();
                return;
            }
            config = nueva.get();
            error = config.probarConexion();
        }

        try {
            iniciarSpring(config);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("No se pudo iniciar el sistema", causaRaiz(e));
            Platform.exit();
            return;
        }

        Scene scene = new Scene(cargarLogin(), 1280, 720);

        primaryStage.setTitle("Sistema de Gestión NanosLand");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/Logo Nanos.png")));
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
     * inicio de sesión. Si el usuario guarda datos nuevos, el sistema se
     * reinicia con esa conexión y vuelve a mostrar el inicio de sesión.
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
        try {
            if (app.springContext != null) {
                app.springContext.close();
            }
            app.iniciarSpring(nueva.get());
            app.primaryStage.getScene().setRoot(app.cargarLogin());
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("No se pudo aplicar la nueva conexión",
                    causaRaiz(e) + "\n\nCierra el sistema y vuelve a abrirlo.");
        }
    }

    private void iniciarSpring(ConfiguracionBD config) {
        springContext = new SpringApplicationBuilder()
                .sources(NegocioApplication.class)
                .sources(PresentacionConfig.class)
                .run(config.argumentosSpring());
        ViewSwitcher.setSpringContext(springContext);
    }

    private Parent cargarLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/presentacion/views/Login.fxml"));
        loader.setControllerFactory(springContext::getBean);
        return loader.load();
    }

    private static String causaRaiz(Throwable e) {
        Throwable causa = e;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        return causa.getMessage() != null ? causa.getMessage() : causa.toString();
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
