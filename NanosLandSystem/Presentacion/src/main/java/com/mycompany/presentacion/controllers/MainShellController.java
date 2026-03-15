package com.mycompany.presentacion.controllers;

import com.mycompany.presentacion.utils.ViewSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import org.springframework.stereotype.Controller;

@Controller
public class MainShellController {

    @FXML private BorderPane rootPane;
    @FXML private ToggleGroup navGroup;
    @FXML private ToggleButton btnCotizacion;
    @FXML private ToggleButton btnClientes;

    @FXML
    public void initialize() {
        ViewSwitcher.setContenedorPrincipal(rootPane);
        btnCotizacion.setSelected(true);
        navCotizacion();

        // Evitar que se deseleccione al hacer click en el ya seleccionado
        navGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) oldVal.setSelected(true);
        });
    }

    @FXML
    private void navCotizacion() {
        ViewSwitcher.cargarVista("Cotizacion.fxml");
    }

    @FXML
    private void navClientes() {
        ViewSwitcher.cargarVista("Usuarios.fxml");
    }
}