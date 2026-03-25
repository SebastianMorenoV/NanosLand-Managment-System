package com.mycompany.presentacion.controllers;

import com.example.negocio.exception.CotizacionException;
import com.example.negocio.cliente.usecase.RegistrarClienteUseCase;
import com.mycompany.common.dtos.ClienteDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class NuevoClienteModalController {

    private final RegistrarClienteUseCase registrarClienteUseCase;

    private ClienteDTO clienteCreado;

    @FXML private TextField txtNombre;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtCorreo;

    public ClienteDTO getClienteCreado() {
        return clienteCreado;
    }

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

        try {
            clienteCreado = registrarClienteUseCase.registrarCliente(nombre, telefono, correo);
            // Cerramos la ventana una vez guardado
            cerrarModal();
        } catch (CotizacionException ex) {
            mostrarAlerta("Error", ex.getMessage());
        } catch (Exception ex) {
            mostrarAlerta("Error", "Ocurrió un error inesperado al guardar el cliente. Intente de nuevo.");
        }
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