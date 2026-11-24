package com.mycompany.presentacion.utils;

import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Utilidades para mostrar ventanas modales que se adaptan a la resolución
 * de la pantalla. En laptops con pantallas pequeñas (ej. 1280x720) un modal
 * con tamaño fijo puede quedar más alto/ancho que el área visible y cortarse
 * sin posibilidad de scroll. Este helper limita el tamaño del Stage a un
 * porcentaje del área de trabajo de la pantalla y, cuando el contenido no cabe,
 * lo envuelve en un ScrollPane para que todo siga siendo accesible.
 */
public final class ModalHelper {

    /** Fracción máxima del área visible de la pantalla que puede ocupar un modal. */
    private static final double MAX_FRACCION = 0.9;

    /** Hoja de estilos de la aplicación (la misma que cargan las vistas FXML). */
    public static final String HOJA_ESTILOS = "/com/mycompany/presentacion/styles/styles.css";

    private ModalHelper() {
    }

    /**
     * Muestra el {@code contenido} como modal de aplicación y bloquea hasta que
     * se cierre. El Stage se dimensiona al tamaño preferido del contenido, pero
     * nunca más allá del {@value #MAX_FRACCION} del área visible de la pantalla;
     * si el contenido excede ese límite se añade scroll automáticamente.
     *
     * @param contenido raíz de la vista ya cargada (FXML).
     * @param titulo    título de la ventana.
     * @param stage     Stage a configurar y mostrar (nuevo o reutilizado).
     */
    public static void mostrarModal(Parent contenido, String titulo, Stage stage) {
        Rectangle2D visual = Screen.getPrimary().getVisualBounds();
        double maxAncho = visual.getWidth() * MAX_FRACCION;
        double maxAlto = visual.getHeight() * MAX_FRACCION;

        // Medimos el tamaño preferido del contenido para decidir si necesita scroll.
        // applyCss() no hace nada en un nodo que no está en una escena: sin estilos la
        // medida salía más chica que la real y un modal alto podía quedar más grande que
        // la pantalla (sin scroll y con los botones fuera de la vista). Por eso se mide
        // dentro de una escena provisional.
        Group medicion = null;
        if (contenido.getScene() == null && contenido.getParent() == null) {
            medicion = new Group(contenido);
            new Scene(medicion);
        }
        contenido.applyCss();
        contenido.autosize();
        double prefAncho = contenido.prefWidth(-1);
        double prefAlto = contenido.prefHeight(prefAncho);
        if (medicion != null) {
            medicion.getChildren().clear();
        }

        boolean excedeAncho = prefAncho > maxAncho;
        boolean excedeAlto = prefAlto > maxAlto;

        Scene scene;
        if (excedeAncho || excedeAlto) {
            ScrollPane scroll = new ScrollPane(contenido);
            scroll.setFitToWidth(true);
            // Solo estiramos a lo alto si el contenido cabe; si no cabe dejamos
            // que conserve su alto preferido y el ScrollPane hace scroll.
            scroll.setFitToHeight(!excedeAlto);
            scroll.getStyleClass().add("scroll-pane");
            scroll.setHbarPolicy(excedeAncho
                    ? ScrollPane.ScrollBarPolicy.AS_NEEDED
                    : ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setVbarPolicy(excedeAlto
                    ? ScrollPane.ScrollBarPolicy.AS_NEEDED
                    : ScrollPane.ScrollBarPolicy.NEVER);

            double ancho = Math.min(prefAncho, maxAncho);
            double alto = Math.min(prefAlto, maxAlto);
            scene = new Scene(scroll, ancho, alto);
            // Con scroll el usuario puede querer ampliar la ventana manualmente.
            stage.setResizable(true);
        } else {
            scene = new Scene(contenido);
            stage.setResizable(false);
        }

        stage.setTitle(titulo);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.showAndWait();
    }

    // ─── DIÁLOGOS CONSTRUIDOS EN CÓDIGO ──────────────────────────────────────

    /**
     * Da a un {@link Dialog} armado en código el mismo aspecto que los modales FXML:
     * carga la hoja de estilos de la aplicación (sin ella los controles se ven con el
     * estilo por defecto de JavaFX), fondo blanco, botón principal azul y "Cancelar"
     * como botón de texto a su izquierda.
     * <p>
     * Se llama después de agregar los {@link ButtonType} y antes de mostrar el diálogo.
     *
     * @param dialog      diálogo a estilizar.
     * @param propietario ventana sobre la que se centra el diálogo, o {@code null}.
     */
    public static void aplicarEstilo(Dialog<?> dialog, Window propietario) {
        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(ModalHelper.class.getResource(HOJA_ESTILOS).toExternalForm());
        pane.getStyleClass().add("dialogo-app");
        if (propietario != null) {
            dialog.initOwner(propietario);
        }

        // Mismo orden que en los modales FXML: "Cancelar" a la izquierda del botón principal.
        if (pane.lookup(".button-bar") instanceof ButtonBar barra) {
            barra.setButtonOrder(ButtonBar.BUTTON_ORDER_MAC_OS);
        }
        for (ButtonType tipo : pane.getButtonTypes()) {
            if (pane.lookupButton(tipo) instanceof Button boton) {
                // Cada botón con su ancho natural: "Cancelar" no debe estirarse al del principal.
                ButtonBar.setButtonUniformSize(boton, false);
                boolean cancelar = tipo.getButtonData().isCancelButton();
                boton.getStyleClass().add(cancelar ? "boton-texto-gris" : "boton-azul");
                if (tipo == ButtonType.CANCEL) {
                    boton.setText("Cancelar");
                }
            }
        }
    }

    /** Encabezado de un modal: título grande y una línea de ayuda debajo. */
    public static VBox encabezado(String titulo, String subtitulo) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("titulo-modal");
        Label lblSubtitulo = new Label(subtitulo);
        lblSubtitulo.getStyleClass().add("subtitulo-modal");
        lblSubtitulo.setWrapText(true);
        // Sin esto el Dialog puede recortar el texto cuando ocupa más de una línea.
        lblSubtitulo.setMinHeight(Region.USE_PREF_SIZE);
        return new VBox(4, lblTitulo, lblSubtitulo);
    }

    /**
     * Campo de formulario: etiqueta pequeña en mayúsculas sobre el control, que
     * ocupa todo el ancho disponible.
     */
    public static VBox campo(String etiqueta, Node control) {
        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("label-pequeno");
        if (control instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        return new VBox(5, lbl, control);
    }

    /** Campo de texto con el estilo de la aplicación. */
    public static TextField campoTexto(String promptText) {
        TextField campo = new TextField();
        campo.setPromptText(promptText);
        campo.getStyleClass().add("text-field-moderno");
        return campo;
    }
}
