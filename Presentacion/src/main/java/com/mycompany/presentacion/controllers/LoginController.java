package com.mycompany.presentacion.controllers;

import com.example.negocio.exception.CotizacionException;
import com.example.negocio.sesion.usecase.IniciarSesionUseCase;
import com.mycompany.persistencia.dominio.Usuario;
import com.mycompany.presentacion.App;
import com.mycompany.presentacion.context.SesionContext;
import com.mycompany.presentacion.utils.Espera;
import com.mycompany.presentacion.utils.ViewSwitcher;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.shape.SVGPath;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final IniciarSesionUseCase iniciarSesionUseCase;
    private final SesionContext sesionContext;

    /** Ícono de ojo abierto (mostrar) y tachado (ocultar), de Material Icons. */
    private static final String OJO = "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z";
    private static final String OJO_TACHADO = "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z";

    @FXML private TextField txtCorreo;
    @FXML private PasswordField txtContrasena;
    @FXML private TextField txtContrasenaVisible;
    @FXML private ToggleButton btnVerContrasena;
    @FXML private SVGPath iconoOjo;
    @FXML private ImageView imgLogo;
    @FXML private Label lblVersion;

    @FXML
    public void initialize() {
        // Los dos campos de contraseña muestran el mismo texto; deja espacio a la derecha para el ojo.
        txtContrasenaVisible.textProperty().bindBidirectional(txtContrasena.textProperty());
        txtContrasena.getStyleClass().add("campo-contrasena");
        txtContrasenaVisible.getStyleClass().add("campo-contrasena");
        mostrarContrasena(false);

        // jDeploy indica la versión instalada; al ejecutar desde el IDE no existe.
        String version = System.getProperty("jdeploy.app.version");
        lblVersion.setText("Sistema de Gestión NanosLand "
                + (version != null ? "v" + version : "(desarrollo)"));
        try {
            imgLogo.setImage(new Image(getClass().getResourceAsStream("/Logo Nanos.png")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void iniciarSesion() {
        String correo = txtCorreo.getText();
        String contrasena = txtContrasena.getText();

        // La verificación de la contraseña (BCrypt) y la primera consulta pueden tardar.
        Espera.ejecutar(txtCorreo, "Iniciando sesión…",
                () -> iniciarSesionUseCase.iniciarSesion(correo, contrasena),
                this::abrirSistema,
                error -> {
                    if (error instanceof CotizacionException) {
                        mostrarAlerta("Acceso Denegado", error.getMessage());
                    } else {
                        mostrarAlerta("Error", "Ocurrió un error inesperado al iniciar sesión.");
                    }
                });
    }

    private void abrirSistema(Usuario usuario) {
        try {
            sesionContext.setUsuarioAutenticado(usuario);

            // Cargar el MainShell reemplazando la escena completa
            FXMLLoader loader = new FXMLLoader(getClass()
                    .getResource("/com/mycompany/presentacion/views/MainShell.fxml"));
            loader.setControllerFactory(ViewSwitcher.getSpringContext()::getBean);
            Parent root = loader.load();

            Stage stage = (Stage) txtCorreo.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setMaximized(true);
        } catch (Exception ex) {
            mostrarAlerta("Error", "Ocurrió un error inesperado al iniciar sesión.");
            ex.printStackTrace();
        }
    }

    @FXML
    private void alternarContrasena() {
        mostrarContrasena(btnVerContrasena.isSelected());
    }

    /** Muestra la contraseña en claro o con puntos, y deja el cursor al final del campo visible. */
    private void mostrarContrasena(boolean visible) {
        txtContrasenaVisible.setVisible(visible);
        txtContrasena.setVisible(!visible);
        iconoOjo.setContent(visible ? OJO_TACHADO : OJO);
        btnVerContrasena.setTooltip(new Tooltip(visible ? "Ocultar contraseña" : "Mostrar contraseña"));
        TextField activo = visible ? txtContrasenaVisible : txtContrasena;
        if (btnVerContrasena.isFocused() || txtContrasena.isFocused() || txtContrasenaVisible.isFocused()) {
            activo.requestFocus();
            activo.positionCaret(activo.getText() == null ? 0 : activo.getText().length());
        }
    }

    @FXML
    private void configurarConexion() {
        App.configurarConexion();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
