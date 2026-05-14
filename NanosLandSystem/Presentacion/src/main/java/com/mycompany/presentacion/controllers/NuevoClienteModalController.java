package com.mycompany.presentacion.controllers;

import com.example.negocio.exception.CotizacionException;
import com.example.negocio.cliente.usecase.RegistrarClienteUseCase;
import com.example.negocio.cliente.usecase.ActualizarClienteUseCase;
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
    private final ActualizarClienteUseCase actualizarClienteUseCase;

    private ClienteDTO clienteCreado;
    private ClienteDTO clienteAEditar;

    @FXML private TextField txtNombre;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtCalle;
    @FXML private TextField txtColonia;
    @FXML private TextField txtCiudad;
    @FXML private TextField txtCodigoPostal;

    public ClienteDTO getClienteCreado() {
        return clienteCreado;
    }

    public void setClienteAEditar(ClienteDTO clienteAEditar) {
        this.clienteAEditar = clienteAEditar;
        if (clienteAEditar != null) {
            txtNombre.setText(clienteAEditar.getNombre());
            txtTelefono.setText(clienteAEditar.getTelefono());
            txtCorreo.setText(clienteAEditar.getCorreo());
            txtCalle.setText(clienteAEditar.getCalle());
            txtColonia.setText(clienteAEditar.getColonia());
            txtCiudad.setText(clienteAEditar.getCiudad());
            txtCodigoPostal.setText(clienteAEditar.getCodigoPostal());
        }
    }

    @FXML
    private void guardarCliente() {
        String nombre = txtNombre.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String correo = txtCorreo.getText().trim();
        String calle = txtCalle.getText().trim();
        String colonia = txtColonia.getText().trim();
        String ciudad = txtCiudad.getText().trim();
        String cp = txtCodigoPostal.getText().trim();

        // 1. Validar campos obligatorios
        if (nombre.isEmpty() || telefono.isEmpty()) {
            mostrarAlerta("Campos Incompletos", "Por favor, ingrese al menos el nombre y el teléfono.");
            return;
        }

        try {
            if (clienteAEditar == null) {
                ClienteDTO dto = new ClienteDTO();
                dto.setNombre(nombre);
                dto.setTelefono(telefono);
                dto.setCorreo(correo);
                dto.setCalle(calle);
                dto.setColonia(colonia);
                dto.setCiudad(ciudad);
                dto.setCodigoPostal(cp);
                clienteCreado = registrarClienteUseCase.registrarCliente(dto);
            } else {
                clienteAEditar.setNombre(nombre);
                clienteAEditar.setTelefono(telefono);
                clienteAEditar.setCorreo(correo);
                clienteAEditar.setCalle(calle);
                clienteAEditar.setColonia(colonia);
                clienteAEditar.setCiudad(ciudad);
                clienteAEditar.setCodigoPostal(cp);
                clienteCreado = actualizarClienteUseCase.actualizarCliente(clienteAEditar);
            }
            
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