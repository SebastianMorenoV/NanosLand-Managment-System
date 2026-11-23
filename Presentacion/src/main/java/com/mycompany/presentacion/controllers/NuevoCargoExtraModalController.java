package com.mycompany.presentacion.controllers;

import com.example.negocio.estadoCuenta.usecase.RegistrarCargoExtraUseCase;
import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.CargoExtraDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class NuevoCargoExtraModalController {

    private final RegistrarCargoExtraUseCase registrarCargoExtraUseCase;

    @FXML private TextField txtDescripcion;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtPrecioUnitario;

    private Long eventoId;
    private boolean cargoRegistrado = false;

    @FXML
    public void initialize() {
        txtCantidad.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            if (change.getControlNewText().matches("\\d*")) return change;
            return null;
        }));
        
        txtPrecioUnitario.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            if (change.getControlNewText().matches("\\d*(\\.\\d*)?")) return change;
            return null;
        }));
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    public boolean isCargoRegistrado() {
        return cargoRegistrado;
    }

    @FXML
    private void guardarCargo() {
        String descripcion = txtDescripcion.getText().trim();
        String cantidadStr = txtCantidad.getText().trim();
        String precioStr = txtPrecioUnitario.getText().trim();

        if (descripcion.isEmpty() || cantidadStr.isEmpty() || precioStr.isEmpty()) {
            mostrarAlerta("Datos Incompletos", "Debe llenar todos los campos.");
            return;
        }

        try {
            int cantidad = Integer.parseInt(cantidadStr);
            double precio = Double.parseDouble(precioStr);

            CargoExtraDTO dto = new CargoExtraDTO();
            dto.setEventoId(eventoId);
            dto.setDescripcion(descripcion);
            dto.setCantidad(cantidad);
            dto.setPrecioUnitario(precio);

            registrarCargoExtraUseCase.registrarCargo(dto);
            cargoRegistrado = true;
            cerrarModal();
        } catch (NumberFormatException ex) {
            mostrarAlerta("Error de Formato", "Asegúrese de ingresar números válidos en cantidad y precio.");
        } catch (CotizacionException ex) {
            mostrarAlerta("Error", ex.getMessage());
        } catch (Exception ex) {
            mostrarAlerta("Error", "Ocurrió un error inesperado al registrar el cargo extra.");
        }
    }

    @FXML
    private void cerrarModal() {
        Stage stage = (Stage) txtDescripcion.getScene().getWindow();
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
