package com.mycompany.presentacion.controllers;

import com.example.negocio.evento.usecase.CambiarHorarioEventoUseCase;
import com.mycompany.common.dtos.EventoDTO;
import com.mycompany.common.dtos.ReporteLogisticaSemanalDTO;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.presentacion.utils.ComboHoras;
import com.mycompany.presentacion.utils.ModalHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.time.LocalTime;
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
    @FXML private Button btnFinalizar;
    @FXML private Button btnEditarHorario;

    private EventoDTO evento;
    private Runnable onEstadoActualizado;

    private final com.example.negocio.evento.usecase.ActualizarEstadoEventoUseCase actualizarEstadoEventoUseCase;
    private final CambiarHorarioEventoUseCase cambiarHorarioEventoUseCase;

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

        mostrarHorario();

        lblPaquete.setText(evento.getPaqueteDetalle() != null && !evento.getPaqueteDetalle().isBlank() ? evento.getPaqueteDetalle() : "Personalizado");
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
        // Un evento en curso (es hoy) ya no se reprograma: si no se realiza, se cancela.
        if (btnReprogramar != null) btnReprogramar.setDisable(deshabilitarBotones
                || evento.getEstadoEvento() == EstadoEvento.EN_CURSO);
        // El horario se puede corregir aunque el evento ya se haya realizado; no si se canceló.
        boolean cancelado = evento.getEstadoEvento() == EstadoEvento.CANCELADO
                || evento.getEstadoEvento() == EstadoEvento.CANCELADO_TARDIO;
        if (btnEditarHorario != null) btnEditarHorario.setDisable(cancelado);

        // Finalizar a mano: solo un evento confirmado o en curso cuya fecha ya llegó.
        boolean sePuedeFinalizar = (evento.getEstadoEvento() == EstadoEvento.CONFIRMADO
                || evento.getEstadoEvento() == EstadoEvento.EN_CURSO)
                && evento.getFecha() != null && !evento.getFecha().isAfter(java.time.LocalDate.now());
        if (btnFinalizar != null) btnFinalizar.setDisable(!sePuedeFinalizar);
    }

    private void mostrarHorario() {
        if (evento.getHoraInicio() == null || evento.getHoraFin() == null) {
            lblHorario.setText("Sin horario");
            return;
        }
        lblHorario.setText(ReporteLogisticaSemanalDTO.horaTexto(evento.getHoraInicio())
                + " a " + ReporteLogisticaSemanalDTO.horaTexto(evento.getHoraFin()));
    }

    /** Corrige la hora de inicio y de fin reales del evento (las que salen en el contrato). */
    @FXML
    private void editarHorario() {
        if (evento == null || evento.getId() == null) return;

        ComboBox<LocalTime> comboInicio = new ComboBox<>();
        ComboBox<LocalTime> comboFin = new ComboBox<>();
        ComboHoras.configurarInicio(comboInicio);
        ComboHoras.configurarFin(comboFin);
        comboInicio.setValue(evento.getHoraInicio());
        comboFin.setValue(evento.getHoraFin());

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Editar horario");
        ButtonType guardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, guardar);

        HBox horas = new HBox(10, ModalHelper.campo("HORA DE INICIO", comboInicio),
                ModalHelper.campo("HORA DE FIN", comboFin));
        horas.getChildren().forEach(c -> HBox.setHgrow(c, Priority.ALWAYS));
        VBox contenido = new VBox(18,
                ModalHelper.encabezado("Editar horario",
                        "Hora real de inicio y de fin del evento. Es la que sale en el contrato."),
                horas);
        contenido.setPrefWidth(400);
        dialogo.getDialogPane().setContent(contenido);
        ModalHelper.aplicarEstilo(dialogo, lblFolio.getScene().getWindow());

        // Se guarda al pulsar "Guardar"; si hay un error, el diálogo sigue abierto para corregirlo.
        dialogo.getDialogPane().lookupButton(guardar).addEventFilter(ActionEvent.ACTION, e -> {
            try {
                cambiarHorarioEventoUseCase.cambiarHorario(evento.getId(), comboInicio.getValue(), comboFin.getValue());
            } catch (Exception ex) {
                e.consume();
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.initOwner(dialogo.getDialogPane().getScene().getWindow());
                alert.setTitle("Editar horario");
                alert.setHeaderText("No se pudo guardar el horario");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        });

        if (dialogo.showAndWait().orElse(ButtonType.CANCEL) == guardar) {
            evento.setHoraInicio(comboInicio.getValue());
            evento.setHoraFin(comboFin.getValue());
            mostrarHorario();
            if (onEstadoActualizado != null) onEstadoActualizado.run();
        }
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
        confirm.setContentText("El evento y su cotización quedarán cancelados y la fecha quedará libre. "
                + "Si el evento ya estaba confirmado y se cancela el mismo día o un día antes, se registra como cancelación tardía.");
        
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
    private void finalizarEvento() {
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Finalizar Evento");
        confirm.setHeaderText("¿Marcar este evento como finalizado?");
        confirm.setContentText("Un evento finalizado ya no se puede cancelar ni reprogramar.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                try {
                    actualizarEstadoEventoUseCase.actualizarEstado(evento.getId(), EstadoEvento.FINALIZADO);
                    if (onEstadoActualizado != null) onEstadoActualizado.run();
                    cerrarModal();
                } catch (Exception e) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Error al finalizar el evento");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                }
            }
        });
    }

    /** Los pagos y el contrato se manejan desde la cotización del evento. */
    @FXML
    private void abrirCotizacion() {
        if (evento == null || evento.getCotizacionId() == null) {
            return;
        }
        var contexto = com.mycompany.presentacion.utils.ViewSwitcher.getSpringContext()
                .getBean(com.mycompany.presentacion.context.CotizacionContext.class);
        contexto.abrir(evento.getCotizacionId(), "Eventos.fxml");
        cerrarModal();
        com.mycompany.presentacion.utils.ViewSwitcher.cargarVista("Cotizacion.fxml");
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
