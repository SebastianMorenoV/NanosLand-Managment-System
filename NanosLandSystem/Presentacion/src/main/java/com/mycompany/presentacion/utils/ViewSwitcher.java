package com.mycompany.presentacion.utils;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ConfigurableApplicationContext;
import java.io.IOException;

public class ViewSwitcher {

    @Setter
    private static BorderPane contenedorPrincipal;

    @Setter
    @Getter
    private static ConfigurableApplicationContext springContext;

    public static void cargarVista(String fxmlFile) {
        cargarVistaInterna(fxmlFile);
    }

    public static <T> T cargarVistaConController(String fxmlFile) {
        FXMLLoader loader = cargarVistaInterna(fxmlFile);
        return loader != null ? loader.getController() : null;
    }

    /**
     * Carga la vista y la coloca en el centro del contenedor principal envuelta
     * en un ScrollPane. La vista se estira a lo ancho del viewport y, a lo alto,
     * ocupa al menos todo el viewport (respetando los VBox.vgrow/HBox.hgrow
     * existentes) pero puede crecer más allá: cuando el contenido denso no cabe
     * —típico en laptops de 1280x720 o incluso 1920x1080— aparece scroll vertical
     * y las barras de acciones inferiores dejan de cortarse.
     *
     * @return el loader usado (para obtener el controller) o {@code null} si falló.
     */
    private static FXMLLoader cargarVistaInterna(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(ViewSwitcher.class
                    .getResource("/com/mycompany/presentacion/views/" + fxmlFile));
            loader.setControllerFactory(springContext::getBean);
            Parent vista = loader.load();
            contenedorPrincipal.setCenter(envolverEnScroll(vista));
            return loader;
        } catch (IOException e) {
            System.out.println("Error al cargar la vista: " + fxmlFile);
            e.printStackTrace();
            return null;
        }
    }

    private static ScrollPane envolverEnScroll(Parent vista) {
        ScrollPane scroll = new ScrollPane(vista);
        // fitToWidth estira la vista a lo ancho (respeta hgrow). NO usamos
        // fitToHeight porque forzaría la altura del viewport siempre, impidiendo
        // el scroll cuando el contenido es más alto que la pantalla.
        scroll.setFitToWidth(true);
        scroll.setPannable(false);
        scroll.getStyleClass().add("scroll-pane");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // La vista ocupa como mínimo el alto del viewport (para que vgrow siga
        // llenando la pantalla cuando sobra espacio), pero puede crecer más y
        // entonces el ScrollPane muestra scroll en lugar de cortar el contenido.
        if (vista instanceof Region region) {
            region.minHeightProperty().bind(
                    Bindings.selectDouble(scroll.viewportBoundsProperty(), "height"));
        }
        return scroll;
    }

    public static BorderPane getContenedorPrincipal() {
        return contenedorPrincipal;
    }
}
