package com.mycompany.presentacion.config;

import com.mycompany.presentacion.utils.ModalHelper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Ventana que se muestra mientras el sistema arranca (conexión a MySQL y Spring
 * tardan varios segundos). Sin ella no aparece nada en pantalla durante ese
 * tiempo y el usuario intenta abrir la aplicación otra vez.
 */
public final class PantallaCarga {

    private final Stage stage = new Stage(StageStyle.TRANSPARENT);
    private final Label lblEstado = new Label("Iniciando…");

    public PantallaCarga() {
        ImageView logo = new ImageView();
        Image imagen = null;
        try {
            // Se reduce al cargarla: el original es de 2000x2000.
            imagen = new Image(PantallaCarga.class.getResourceAsStream("/Logo Nanos.png"), 360, 360, true, true);
            logo.setImage(imagen);
        } catch (RuntimeException e) {
            // Sin logo la pantalla sigue cumpliendo su función.
        }
        logo.setFitWidth(170);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);

        Label lblSistema = new Label("SISTEMA DE GESTIÓN");
        lblSistema.getStyleClass().add("carga-sistema");
        Label lblNombre = new Label("NanosLand");
        lblNombre.getStyleClass().add("carga-nombre");
        Label lblDescripcion = new Label("Cotizaciones, eventos, pagos y reportes");
        lblDescripcion.getStyleClass().add("carga-descripcion");
        VBox textos = new VBox(2, lblSistema, lblNombre, lblDescripcion);
        textos.setAlignment(Pos.CENTER_LEFT);

        HBox cuerpo = new HBox(28, logo, textos);
        cuerpo.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(cuerpo, Priority.ALWAYS);

        lblEstado.getStyleClass().add("carga-estado");
        String version = System.getProperty("jdeploy.app.version");
        Label lblVersion = new Label(version != null ? "v" + version : "desarrollo");
        lblVersion.getStyleClass().add("carga-version");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox pie = new HBox(lblEstado, espacio, lblVersion);
        pie.setAlignment(Pos.CENTER_LEFT);

        ProgressBar barra = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
        barra.setMaxWidth(Double.MAX_VALUE);
        barra.getStyleClass().add("carga-barra");

        VBox tarjeta = new VBox(18, cuerpo, new VBox(8, pie, barra));
        tarjeta.getStyleClass().add("carga-tarjeta");
        tarjeta.setPrefSize(580, 300);

        // Margen transparente alrededor para que se vea la sombra de la tarjeta.
        StackPane raiz = new StackPane(tarjeta);
        raiz.setPadding(new Insets(24));
        raiz.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(raiz);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(PantallaCarga.class.getResource(ModalHelper.HOJA_ESTILOS).toExternalForm());
        stage.setScene(scene);
        stage.setTitle("NanosLand");
        if (imagen != null) {
            stage.getIcons().add(imagen);
        }
    }

    public void mostrar() {
        stage.centerOnScreen();
        stage.show();
    }

    /** Cambia el texto de lo que se está haciendo. Puede llamarse desde cualquier hilo. */
    public void estado(String texto) {
        if (Platform.isFxApplicationThread()) {
            lblEstado.setText(texto);
        } else {
            Platform.runLater(() -> lblEstado.setText(texto));
        }
    }

    /** Ventana sobre la que se centran los diálogos que aparezcan durante el arranque. */
    public Window ventana() {
        return stage;
    }

    public void cerrar() {
        stage.close();
    }
}
