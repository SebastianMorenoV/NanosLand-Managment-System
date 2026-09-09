package com.mycompany.presentacion.controllers;

import com.mycompany.common.dtos.EventoDTO;
import com.mycompany.persistencia.enums.EstadoEvento;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.time.format.DateTimeFormatter;
import javafx.scene.control.Button;

@Controller
@RequiredArgsConstructor
public class EventoDetallesModalController {

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

    @FXML private Button btnCancelar;
    @FXML private Button btnReprogramar;

    private EventoDTO evento;
    private Runnable onEstadoActualizado;

    private final com.example.negocio.evento.usecase.ActualizarEstadoEventoUseCase actualizarEstadoEventoUseCase;

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
        
        boolean deshabilitarBotones = evento.getEstadoEvento() == EstadoEvento.CANCELADO || 
                                      evento.getEstadoEvento() == EstadoEvento.CANCELADO_TARDIO ||
                                      evento.getEstadoEvento() == EstadoEvento.FINALIZADO;
        if (btnCancelar != null) btnCancelar.setDisable(deshabilitarBotones);
        if (btnReprogramar != null) btnReprogramar.setDisable(deshabilitarBotones);
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
            case CANCELADO_TARDIO:
                lblBadgeEstado.setStyle("-fx-background-color: #fdedec; -fx-text-fill: #e74c3c; -fx-border-color: #e74c3c; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11px;");
                break;
            default:
                lblBadgeEstado.setStyle("-fx-background-color: #eaecee; -fx-text-fill: #7f8c8d; -fx-padding: 3 10 3 10; -fx-background-radius: 12px; -fx-font-weight: bold; -fx-font-size: 11px;");
                break;
        }
    }

    @FXML
    private void cancelarEvento() {
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancelar Evento");
        confirm.setHeaderText("¿Está seguro de que desea cancelar este evento?");
        confirm.setContentText("Esta acción cambiará el estado del evento. Si se cancela 1 día antes o el mismo día, la fecha no se liberará para otras cotizaciones.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                try {
                    actualizarEstadoEventoUseCase.actualizarEstado(evento.getId(), EstadoEvento.CANCELADO);
                    if (onEstadoActualizado != null) onEstadoActualizado.run();
                    cerrarModal();
                } catch (Exception e) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Error al cancelar el evento");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                }
            }
        });
    }

    @FXML
    private void abrirReprogramarModal() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/mycompany/presentacion/views/ReprogramarEventoModal.fxml"));
            loader.setControllerFactory(com.mycompany.presentacion.utils.ViewSwitcher.getSpringContext()::getBean);
            javafx.scene.Parent root = loader.load();

            ReprogramarEventoModalController controller = loader.getController();
            controller.setEvento(evento, () -> {
                if (onEstadoActualizado != null) onEstadoActualizado.run();
                cerrarModal();
            });

            Stage stage = new Stage();
            stage.setTitle("Reprogramar Evento");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(lblFolio.getScene().getWindow());
            stage.setScene(new javafx.scene.Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se pudo abrir la ventana de reprogramación");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void cerrarModal() {
        Stage stage = (Stage) lblFolio.getScene().getWindow();
        stage.close();
    }
}
