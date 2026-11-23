package com.mycompany.presentacion.utils;

import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

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
        contenido.applyCss();
        contenido.autosize();
        double prefAncho = contenido.prefWidth(-1);
        double prefAlto = contenido.prefHeight(prefAncho);

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
}
