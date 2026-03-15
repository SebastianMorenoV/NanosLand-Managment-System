package com.mycompany.presentacion.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class NuevoClienteModalController {

    // TODO: Inyectar aquí tu UseCase para guardar en la base de datos
    // private final GuardarClienteUseCase guardarClienteUseCase;

    @FXML private TextField txtNombre;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtCorreo;

    @FXML
    private void guardarCliente() {
        String nombre = txtNombre.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String correo = txtCorreo.getText().trim();

        // 1. Validar campos obligatorios
        if (nombre.isEmpty() || telefono.isEmpty()) {
            mostrarAlerta("Campos Incompletos", "Por favor, ingrese al menos el nombre y el teléfono.");
            return;
        }

        // 2. Aquí llamarás a tu UseCase para guardar en MySQL
        // Cliente nuevoCliente = new Cliente();
        // nuevoCliente.setNombre(nombre);
        // nuevoCliente.setTelefono(telefono);
        // guardarClienteUseCase.guardar(nuevoCliente);

        // 3. Cerramos la ventana una vez guardado
        cerrarModal();
    }

    @FXML
    private void cerrarModal() {
        // Obtenemos la ventana actual a partir de cualquier componente visual y la cerramos
        Stage stage = (Stage) txtNombre.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}