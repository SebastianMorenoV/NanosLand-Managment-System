package com.mycompany.presentacion.controllers;

import com.mycompany.presentacion.context.CotizacionContext;
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
    private ToggleButton btnInicio;
    @FXML
    private ToggleButton btnCotizacion;
    @FXML
    private ToggleButton btnCotizaciones;
    @FXML
    private ToggleButton btnVentas;
    @FXML
    private ToggleButton btnEventos;
    @FXML
    private ToggleButton btnClientes;
    @FXML
    private ToggleButton btnPaquetes;
    @FXML
    private ToggleButton btnServicios;
    @FXML
    private ToggleButton btnDescuentos;
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
        ViewSwitcher.setAlCambiarVista(this::marcarMenu);

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

        // Gestión de usuarios y descuentos: solo el dueño
        boolean esDueno = sesionContext.esDueno();
        if (btnGestionUsuarios != null) {
            btnGestionUsuarios.setVisible(esDueno);
            btnGestionUsuarios.setManaged(esDueno);
        }
        if (btnDescuentos != null) {
            btnDescuentos.setVisible(esDueno);
            btnDescuentos.setManaged(esDueno);
        }

        // Ocultar reportes de adeudos e ingresos para ADMINISTRADOR (solo visible para DUEÑO)
        if (sesionContext.esAdministrador()) {
            if (btnReporteAdeudos != null) {
                btnReporteAdeudos.setVisible(false);
                btnReporteAdeudos.setManaged(false);
            }
            if (btnReporteIngresos != null) {
                btnReporteIngresos.setVisible(false);
                btnReporteIngresos.setManaged(false);
            }
        }

        btnInicio.setSelected(true);
        navInicio();

        navGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) oldVal.setSelected(true);
        });

        iniciarReloj();
    }

    private Timeline reloj;

    private void iniciarReloj() {
        if (lblReloj != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a");
            if (reloj != null) {
                reloj.stop(); // el controlador se reutiliza: no acumular un reloj por cada inicio de sesión
            }
            Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
                lblReloj.setText(LocalDateTime.now().atZone(ZoneId.of("America/Hermosillo")).format(formatter));
            }), new KeyFrame(Duration.seconds(1)));
            clock.setCycleCount(Animation.INDEFINITE);
            clock.play();
            reloj = clock;
        }
    }

    /** Marca en el menú la pantalla abierta, también cuando se llegó desde un botón de otra pantalla. */
    private void marcarMenu(String vista) {
        ToggleButton opcion;
        switch (vista) {
            case "Inicio.fxml": opcion = btnInicio; break;
            case "SeleccionarFecha.fxml": opcion = btnCotizacion; break;
            case "Cotizaciones.fxml": opcion = btnCotizaciones; break;
            case "Cotizacion.fxml":
                opcion = ViewSwitcher.getSpringContext().getBean(CotizacionContext.class).isModoEdicion()
                        ? btnCotizaciones : btnCotizacion;
                break;
            case "Eventos.fxml": opcion = btnEventos; break;
            case "Logistica.fxml": opcion = btnLogistica; break;
            case "Ventas.fxml": opcion = btnVentas; break;
            default: return;
        }
        if (!opcion.isSelected()) {
            opcion.setSelected(true);
        }
    }

    @FXML
    private void navInicio() {
        ViewSwitcher.cargarVista("Inicio.fxml");
    }

    @FXML
    private void navCotizaciones() {
        ViewSwitcher.cargarVista("Cotizaciones.fxml");
    }

    @FXML
    private void navCotizacion() {
        ViewSwitcher.cargarVista("SeleccionarFecha.fxml");
    }

    @FXML
    private void navEventos() {
        ViewSwitcher.cargarVista("Eventos.fxml");
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
    private void navServicios() {
        ViewSwitcher.cargarVista("Servicios.fxml");
    }

    @FXML
    private void navDescuentos() {
        ViewSwitcher.cargarVista("Descuentos.fxml");
    }

    @FXML
    private void navVentas() {
        ViewSwitcher.cargarVista("Ventas.fxml");
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
        // La cotización en edición no debe pasar a la sesión del siguiente usuario.
        ViewSwitcher.getSpringContext().getBean(CotizacionContext.class).limpiar();

        try {
            FXMLLoader loader = new FXMLLoader(getClass()
                    .getResource("/com/mycompany/presentacion/views/Login.fxml"));
            loader.setControllerFactory(ViewSwitcher.getSpringContext()::getBean);
            Parent root = loader.load();

            // Se cambia solo el contenido de la escena existente. Crear una escena nueva de
            // 1280x720 sobre la ventana maximizada la dejaba descuadrada, y además perdía
            // el atajo F11, que está registrado en la escena original.
            rootPane.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            com.mycompany.presentacion.config.RegistroErrores.avisar("No se pudo cerrar la sesión", e);
        }
    }
}