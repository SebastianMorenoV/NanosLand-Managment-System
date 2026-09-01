package com.mycompany.presentacion.controllers;

import com.mycompany.presentacion.context.SesionContext;
import com.mycompany.presentacion.utils.ViewSwitcher;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MainShellController {

    private final SesionContext sesionContext;

    @FXML
    private BorderPane rootPane;
    @FXML
    private ToggleGroup navGroup;
    @FXML
    private ToggleButton btnCotizacion;
    @FXML
    private ToggleButton btnClientes;
    @FXML
    private ToggleButton btnPaquetes;
    @FXML
    private ToggleButton btnReportes;
    @FXML
    private ToggleButton btnLogistica;
    @FXML
    private ToggleButton btnEstadoCuenta;
    @FXML
    private ToggleButton btnReporteAdeudos;
    @FXML
    private ToggleButton btnReporteIngresos;
    @FXML
    private ToggleButton btnOportunidades;
    @FXML
    private ToggleButton btnGestionUsuarios;
    @FXML
    private Label lblReloj;
    @FXML
    private ImageView imgLogo;
    @FXML
    private Label lblUsuarioActual;
    @FXML
    private Button btnCerrarSesion;

    @FXML
    public void initialize() {
        ViewSwitcher.setContenedorPrincipal(rootPane);

        try {
            imgLogo.setImage(new Image(getClass().getResourceAsStream("/Logo Nanos.png")));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Mostrar información del usuario autenticado
        if (lblUsuarioActual != null && sesionContext.estaAutenticado()) {
            String correo = sesionContext.getUsuarioAutenticado().getCorreo();
            String rol = sesionContext.getUsuarioAutenticado().getRol().name();
            lblUsuarioActual.setText(correo + " (" + rol + ")");
        }

        // Mostrar/ocultar botón de gestión de usuarios según el rol
        if (btnGestionUsuarios != null) {
            boolean esDueno = sesionContext.esDueno();
            btnGestionUsuarios.setVisible(esDueno);
            btnGestionUsuarios.setManaged(esDueno);
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
                lblReloj.setText(LocalDateTime.now().atZone(ZoneId.of("America/Hermosillo")).format(formatter));
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

    @FXML
    private void navReporteAdeudos() {
        ViewSwitcher.cargarVista("ReporteAdeudos.fxml");
    }

    @FXML
    private void navReporteIngresos() {
        ViewSwitcher.cargarVista("ReporteIngresos.fxml");
    }

    @FXML
    private void navOportunidades() {
        ViewSwitcher.cargarVista("ReporteOportunidades.fxml");
    }

    @FXML
    private void navLogistica() {
        ViewSwitcher.cargarVista("Logistica.fxml");
    }

    @FXML
    private void navEstadoCuenta() {
        ViewSwitcher.cargarVista("EstadoCuenta.fxml");
    }

    @FXML
    private void navGestionUsuarios() {
        ViewSwitcher.cargarVista("GestionUsuarios.fxml");
    }

    @FXML
    private void cerrarSesion() {
        sesionContext.cerrarSesion();

        try {
            FXMLLoader loader = new FXMLLoader(getClass()
                    .getResource("/com/mycompany/presentacion/views/Login.fxml"));
            loader.setControllerFactory(ViewSwitcher.getSpringContext()::getBean);
            Parent root = loader.load();

            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}