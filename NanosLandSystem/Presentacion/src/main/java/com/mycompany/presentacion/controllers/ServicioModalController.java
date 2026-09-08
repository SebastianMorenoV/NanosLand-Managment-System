package com.mycompany.presentacion.controllers;

import com.example.negocio.catalogo.usecase.GestionarServicioUseCase;
import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.ServicioDTO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

@Controller
@Scope("prototype")
@RequiredArgsConstructor
public class ServicioModalController {

    private final GestionarServicioUseCase gestionarServicioUseCase;

    @FXML private Label lblTitulo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtPrecio;
    @FXML private TextArea txtDescripcion;

    private ServicioDTO servicioAEditar;

    @Getter
    private boolean guardado = false;

    @FXML
    public void initialize() {
        // Reset state for new modal instance
        servicioAEditar = null;
        guardado = false;
        txtNombre.setText("");
        txtPrecio.setText("");
        txtDescripcion.setText("");
        lblTitulo.setText("Nuevo Servicio");

        // Solo permitir números y punto decimal en precio
        if (txtPrecio != null) {
            txtPrecio.setTextFormatter(new TextFormatter<>(change -> {
                if (change.getControlNewText().matches("\\d*(\\.\\d*)?")) return change;
                return null;
            }));
        }
    }

    public void setServicioAEditar(ServicioDTO servicio) {
        this.servicioAEditar = servicio;
        lblTitulo.setText("Editar Servicio");
        txtNombre.setText(servicio.getNombre());
        txtPrecio.setText(String.valueOf(servicio.getPrecio()));
        txtDescripcion.setText(servicio.getDescripcion() != null ? servicio.getDescripcion() : "");
    }

    @FXML
    private void guardarServicio() {
        String nombre = txtNombre.getText().trim();
        String precioStr = txtPrecio.getText().trim();
        String descripcion = txtDescripcion.getText().trim();

        if (nombre.isEmpty()) {
            mostrarError("El nombre del servicio es obligatorio.");
            return;
        }

        if (precioStr.isEmpty()) {
            mostrarError("El precio es obligatorio.");
            return;
        }

        double precio;
        try {
            precio = Double.parseDouble(precioStr);
            if (precio < 0) {
                mostrarError("El precio no puede ser negativo.");
                return;
            }
        } catch (NumberFormatException e) {
            mostrarError("El precio debe ser un número válido.");
            return;
        }

        try {
            if (servicioAEditar == null) {
                // Modo crear
                gestionarServicioUseCase.crearServicio(nombre, precio, descripcion);
            } else {
                // Modo editar
                gestionarServicioUseCase.actualizarServicio(servicioAEditar.getId(), nombre, precio, descripcion);
            }

            guardado = true;
            cerrarModal();

        } catch (CotizacionException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void cerrarModal() {
        Stage stage = (Stage) txtNombre.getScene().getWindow();
        stage.close();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Validación");
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
