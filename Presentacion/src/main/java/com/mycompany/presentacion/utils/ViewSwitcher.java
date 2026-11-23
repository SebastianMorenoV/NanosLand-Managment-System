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
     * Carga la vista y la coloca en el centro del contenedor principal.
     * Las vistas deben manejar su propio Scroll (ej. ScrollPane, TableView, ListView)
     * para no romper el comportamiento de vgrow y hgrow.
     *
     * @return el loader usado (para obtener el controller) o {@code null} si falló.
     */
    private static FXMLLoader cargarVistaInterna(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(ViewSwitcher.class
                    .getResource("/com/mycompany/presentacion/views/" + fxmlFile));
            loader.setControllerFactory(springContext::getBean);
            Parent vista = loader.load();
            contenedorPrincipal.setCenter(vista);
            return loader;
        } catch (IOException e) {
            System.out.println("Error al cargar la vista: " + fxmlFile);
            e.printStackTrace();
            return null;
        }
    }

    public static BorderPane getContenedorPrincipal() {
        return contenedorPrincipal;
    }
}
