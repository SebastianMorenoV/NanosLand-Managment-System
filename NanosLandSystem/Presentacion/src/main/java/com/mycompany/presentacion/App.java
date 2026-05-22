package com.mycompany.presentacion;

import com.example.negocio.NegocioApplication;
import com.mycompany.presentacion.utils.ViewSwitcher;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class App extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() throws Exception {
        springContext = new SpringApplicationBuilder()
                .sources(NegocioApplication.class)
                .sources(PresentacionConfig.class)
                .run(new String[]{});
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        ViewSwitcher.setSpringContext(springContext);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/presentacion/views/MainShell.fxml"));
        loader.setControllerFactory(springContext::getBean);
        Parent root = loader.load();
        Scene scene = new Scene(root, 1280, 720);

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

        primaryStage.setMinWidth(1280);
        primaryStage.setMinHeight(720);
    }

    @Override
    public void stop() throws Exception {
        springContext.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}