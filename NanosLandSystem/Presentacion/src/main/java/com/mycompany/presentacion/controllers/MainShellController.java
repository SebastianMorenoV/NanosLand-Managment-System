package com.mycompany.presentacion.controllers;

import com.mycompany.presentacion.utils.ViewSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Controller;

@Controller
public class MainShellController {

    @FXML private BorderPane rootPane;
    @FXML private ToggleGroup navGroup;
    @FXML private ToggleButton btnCotizacion;
    @FXML private ToggleButton btnClientes;
    @FXML private ToggleButton btnPaquetes;
    @FXML private ToggleButton btnReportes;
    @FXML private Label lblReloj;
    @FXML private ImageView imgLogo;

    @FXML
    public void initialize() {
        ViewSwitcher.setContenedorPrincipal(rootPane);
        
        try {
            imgLogo.setImage(new Image(getClass().getResourceAsStream("/Logo Nanos.png")));
        } catch (Exception e) {
            e.printStackTrace();
        }

        btnCotizacion.setSelected(true);
        navCotizacion();

        navGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) oldVal.setSelected(true);
        });

        iniciarReloj();
    }

    private void iniciarReloj() {
        if (lblReloj != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a");
            Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
                lblReloj.setText(LocalDateTime.now().format(formatter));
            }), new KeyFrame(Duration.seconds(1)));
            clock.setCycleCount(Animation.INDEFINITE);
            clock.play();
        }
    }

    @FXML
    private void navCotizacion() {
        ViewSwitcher.cargarVista("SeleccionarFecha.fxml");
    }

    @FXML
    private void navClientes() {
        ViewSwitcher.cargarVista("Usuarios.fxml");
    }

    @FXML
    private void navPaquetes() {
        ViewSwitcher.cargarVista("Paquetes.fxml");
    }

    @FXML
    private void navReportes() {
        ViewSwitcher.cargarVista("Reportes.fxml");
    }
}