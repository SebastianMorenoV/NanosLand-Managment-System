package com.mycompany.presentacion.controllers;

import com.example.negocio.evento.usecase.ActualizarEstadoEventoUseCase;
import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.EventoDTO;
import com.mycompany.persistencia.enums.EstadoEvento;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.time.format.DateTimeFormatter;

@Controller
@RequiredArgsConstructor
public class EventoDetallesModalController {

    private final ActualizarEstadoEventoUseCase actualizarEstadoEventoUseCase;

    @FXML private Label lblFolio;
    @FXML private Label lblBadgeEstado;
    @FXML private Label lblCliente;
    @FXML private Label lblFestejado;
    @FXML private Label lblTematica;
    @FXML private Label lblFechaTurno;
    @FXML private Label lblHorario;
    @FXML private Label lblPaquete;
    @FXML private Label lblNotas;
    @FXML private Label lblTotalCotizacion;
    @FXML private Label lblCargosExtras;
    @FXML private Label lblGranTotal;
    @FXML private ComboBox<EstadoEvento> cmbNuevoEstado;

    private EventoDTO evento;
    private Runnable onEstadoActualizado;

    @FXML
    public void initialize() {
        cmbNuevoEstado.setItems(FXCollections.observableArrayList(EstadoEvento.values()));
    }

    public void setEvento(EventoDTO evento, Runnable onEstadoActualizado) {
        this.evento = evento;
        this.onEstadoActualizado = onEstadoActualizado;

        if (evento == null) return;

        lblFolio.setText(evento.getFolioCotizacion() != null ? evento.getFolioCotizacion() : "SIN FOLIO");
        lblCliente.setText("Cliente: " + (evento.getClienteNombre() != null ? evento.getClienteNombre() : "No especificado"));
        lblFestejado.setText(evento.getNombreFestejado() != null ? evento.getNombreFestejado() : "-");
        lblTematica.setText(evento.getTematica() != null ? evento.getTematica() : "-");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String fechaStr = evento.getFecha() != null ? evento.getFecha().format(dtf) : "Sin fecha";
        String turnoStr = evento.getTurno() != null ? evento.getTurno().name() : "-";
        lblFechaTurno.setText(fechaStr + " (" + turnoStr + ")");

        String horaInicio = evento.getHoraInicio() != null ? evento.getHoraInicio().toString() : "--:--";
        String horaFin = evento.getHoraFin() != null ? evento.getHoraFin().toString() : "--:--";
        lblHorario.setText(horaInicio + " a " + horaFin);

        lblPaquete.setText(evento.getPaqueteNombre() != null ? evento.getPaqueteNombre() : "Personalizado");
        lblNotas.setText((evento.getNotas() != null && !evento.getNotas().isBlank()) ? evento.getNotas() : "Sin notas adicionales.");

        double totalCot = evento.getTotalCotizacion();
        double totalExtras = evento.getTotalCargosExtras();
        double granTotal = totalCot + totalExtras;

        lblTotalCotizacion.setText(String.format("$%,.2f", totalCot));
        lblCargosExtras.setText(String.format("$%,.2f", totalExtras));
        lblGranTotal.setText(String.format("$%,.2f", granTotal));

        actualizarBadgeEstado(evento.getEstadoEvento());
        cmbNuevoEstado.setValue(evento.getEstadoEvento());
    }

    private void actualizarBadgeEstado(EstadoEvento estado) {
        if (estado == null) {
            lblBadgeEstado.setText("SIN ESTADO");
            lblBadgeEstado.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white;");
            return;
        }

        lblBadgeEstado.setText(estado.name());
        switch (estado) {
            case TENTATIVO:
                lblBadgeEstado.setStyle("-fx-background-color: #fef5e7; -fx-text-fill: #f39c12; -fx-border-color: #f39c12; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11px;");
                break;
            case CONFIRMADO:
                lblBadgeEstado.setStyle("-fx-background-color: #ebf5fb; -fx-text-fill: #1a82b8; -fx-border-color: #1a82b8; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11px;");
                break;
            case EN_CURSO:
                lblBadgeEstado.setStyle("-fx-background-color: #f4ecf7; -fx-text-fill: #8e44ad; -fx-border-color: #8e44ad; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11px;");
                break;
            case FINALIZADO:
                lblBadgeEstado.setStyle("-fx-background-color: #eafaf1; -fx-text-fill: #27ae60; -fx-border-color: #27ae60; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11px;");
                break;
            case CANCELADO:
                lblBadgeEstado.setStyle("-fx-background-color: #fdedec; -fx-text-fill: #e74c3c; -fx-border-color: #e74c3c; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11px;");
                break;
            default:
                lblBadgeEstado.setStyle("-fx-background-color: #eaecee; -fx-text-fill: #7f8c8d; -fx-padding: 3 10 3 10; -fx-background-radius: 12px; -fx-font-weight: bold; -fx-font-size: 11px;");
                break;
        }
    }

    @FXML
    private void guardarEstado() {
        EstadoEvento nuevoEstado = cmbNuevoEstado.getValue();
        if (nuevoEstado == null || evento == null) return;

        if (nuevoEstado == evento.getEstadoEvento()) {
            cerrarModal();
            return;
        }

        try {
            actualizarEstadoEventoUseCase.actualizarEstado(evento.getId(), nuevoEstado);
            evento.setEstadoEvento(nuevoEstado);
            actualizarBadgeEstado(nuevoEstado);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Estado Actualizado");
            alert.setHeaderText(null);
            alert.setContentText("El estado del evento ha sido cambiado a: " + nuevoEstado.name());
            alert.showAndWait();

            if (onEstadoActualizado != null) {
                onEstadoActualizado.run();
            }

            cerrarModal();
        } catch (CotizacionException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se pudo actualizar el estado");
            alert.setContentText("Ocurrió un error inesperado al actualizar el estado.");
            alert.showAndWait();
        }
    }

    @FXML
    private void cerrarModal() {
        Stage stage = (Stage) lblFolio.getScene().getWindow();
        stage.close();
    }
}
