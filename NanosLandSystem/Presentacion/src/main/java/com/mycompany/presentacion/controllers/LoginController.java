package com.mycompany.presentacion.controllers;

import com.example.negocio.exception.CotizacionException;
import com.example.negocio.sesion.usecase.IniciarSesionUseCase;
import com.mycompany.persistencia.dominio.Usuario;
import com.mycompany.presentacion.context.SesionContext;
import com.mycompany.presentacion.utils.ViewSwitcher;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final IniciarSesionUseCase iniciarSesionUseCase;
    private final SesionContext sesionContext;

    @FXML private TextField txtCorreo;
    @FXML private PasswordField txtContrasena;
    @FXML private ImageView imgLogo;

    @FXML
    public void initialize() {
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

        try {
            Usuario usuario = iniciarSesionUseCase.iniciarSesion(correo, contrasena);
            sesionContext.setUsuarioAutenticado(usuario);

            // Cargar el MainShell reemplazando la escena completa
            FXMLLoader loader = new FXMLLoader(getClass()
                    .getResource("/com/mycompany/presentacion/views/MainShell.fxml"));
            loader.setControllerFactory(ViewSwitcher.getSpringContext()::getBean);
            Parent root = loader.load();

            Stage stage = (Stage) txtCorreo.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
            stage.setMaximized(true);

        } catch (CotizacionException ex) {
            mostrarAlerta("Acceso Denegado", ex.getMessage());
        } catch (Exception ex) {
            mostrarAlerta("Error", "Ocurrió un error inesperado al iniciar sesión.");
            ex.printStackTrace();
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
