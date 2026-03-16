package com.mycompany.presentacion.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
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
        try {
            FXMLLoader loader = new FXMLLoader(ViewSwitcher.class
                    .getResource("/com/mycompany/presentacion/views/" + fxmlFile));
            loader.setControllerFactory(springContext::getBean);
            Parent vista = loader.load();
            contenedorPrincipal.setCenter(vista);
        } catch (IOException e) {
            System.out.println("Error al cargar la vista: " + fxmlFile);
            e.printStackTrace();
        }
    }

    public static <T> T cargarVistaConController(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(ViewSwitcher.class
                    .getResource("/com/mycompany/presentacion/views/" + fxmlFile));
            loader.setControllerFactory(springContext::getBean);
            Parent vista = loader.load();
            contenedorPrincipal.setCenter(vista);
            return loader.getController();
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