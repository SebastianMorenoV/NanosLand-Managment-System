package com.mycompany.presentacion.controllers;

import com.example.negocio.evento.usecase.ReprogramarEventoUseCase;
import com.mycompany.common.dtos.EventoDTO;
import com.mycompany.persistencia.enums.TurnoEvento;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class ReprogramarEventoModalController {

    @FXML private DatePicker dpNuevaFecha;
    @FXML private ComboBox<TurnoEvento> cmbNuevoTurno;

    private EventoDTO evento;
    private Runnable onReprogramado;

    private final ReprogramarEventoUseCase reprogramarEventoUseCase;

    public void setEvento(EventoDTO evento, Runnable onReprogramado) {
        this.evento = evento;
        this.onReprogramado = onReprogramado;
    }

    @FXML
    public void initialize() {
        cmbNuevoTurno.setItems(FXCollections.observableArrayList(TurnoEvento.values()));
        dpNuevaFecha.setEditable(false);
    }

    @FXML
    private void guardarReprogramacion() {
        LocalDate nuevaFecha = dpNuevaFecha.getValue();
        TurnoEvento nuevoTurno = cmbNuevoTurno.getValue();

        if (nuevaFecha == null || nuevoTurno == null) {
            mostrarError("Por favor, seleccione una fecha y un turno válidos.");
            return;
        }

        try {
            reprogramarEventoUseCase.reprogramar(evento.getId(), nuevaFecha, nuevoTurno);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Éxito");
            alert.setHeaderText(null);
            alert.setContentText("El evento ha sido reprogramado correctamente.");
            alert.showAndWait();
            
            if (onReprogramado != null) onReprogramado.run();
            cerrarModal();
        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de Reprogramación");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    private void cerrarModal() {
        Stage stage = (Stage) dpNuevaFecha.getScene().getWindow();
        stage.close();
    }
}
