package com.mycompany.presentacion.controllers;

import com.example.negocio.logistica.usecase.ActualizarEstadoLogisticaUseCase;
import com.example.negocio.logistica.usecase.ConsultarLogisticaUseCase;
import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.EventoDTO;
import com.mycompany.common.dtos.LogisticaDTO;
import com.mycompany.persistencia.enums.EstadoLogistica;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class LogisticaController {

    private final ConsultarLogisticaUseCase consultarLogisticaUseCase;
    private final ActualizarEstadoLogisticaUseCase actualizarEstadoUseCase;

    @FXML private ListView<EventoDTO> listaEventos;
    @FXML private ListView<LogisticaDTO> listaServicios;
    @FXML private Label lblEventoSeleccionado;
    @FXML private Label lblSinServicios;

    private final ObservableList<EventoDTO> eventosData = FXCollections.observableArrayList();
    private final ObservableList<LogisticaDTO> serviciosData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarListaEventos();
        configurarListaServicios();
        cargarEventos();
    }

    private void cargarEventos() {
        List<EventoDTO> eventos = consultarLogisticaUseCase.obtenerEventosProximos();
        eventosData.clear();
        if (eventos != null) {
            eventosData.addAll(eventos);
        }
    }

    private void configurarListaEventos() {
        listaEventos.setItems(eventosData);

        listaEventos.setCellFactory(lv -> new ListCell<EventoDTO>() {
            @Override
            protected void updateItem(EventoDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label lblFecha = new Label(item.getFecha() != null
                        ? item.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
                lblFecha.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");

                Label lblCliente = new Label(item.getClienteNombre() != null ? item.getClienteNombre() : "");
                lblCliente.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

                String turnoStr = item.getTurno() != null ? item.getTurno().name() : "";
                turnoStr = turnoStr.substring(0, 1).toUpperCase() + turnoStr.substring(1).toLowerCase();
                Label lblTurno = new Label("Turno: " + turnoStr);
                lblTurno.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 10px;");

                Label lblPaquete = new Label(item.getPaqueteNombre() != null ? "📦 " + item.getPaqueteNombre() : "");
                lblPaquete.setStyle("-fx-text-fill: #1a82b8; -fx-font-size: 11px;");

                VBox vbox = new VBox(4, lblFecha, lblCliente, lblPaquete, lblTurno);
                vbox.setPadding(new Insets(8));

                setGraphic(vbox);
            }
        });

        listaEventos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                cargarServiciosEvento(newVal);
            } else {
                serviciosData.clear();
                if (lblEventoSeleccionado != null) {
                    lblEventoSeleccionado.setText("Seleccione un evento");
                }
            }
        });
    }

    private void cargarServiciosEvento(EventoDTO evento) {
        if (lblEventoSeleccionado != null) {
            String fechaStr = evento.getFecha() != null
                    ? evento.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
            lblEventoSeleccionado.setText("Logística: " + evento.getClienteNombre() + " — " + fechaStr);
        }

        List<LogisticaDTO> servicios = consultarLogisticaUseCase.obtenerServiciosPorEvento(evento.getId());
        serviciosData.clear();
        if (servicios != null) {
            serviciosData.addAll(servicios);
        }

        if (lblSinServicios != null) {
            boolean vacia = serviciosData.isEmpty();
            lblSinServicios.setVisible(vacia);
            lblSinServicios.setManaged(vacia);
        }
    }

    private void configurarListaServicios() {
        listaServicios.setItems(serviciosData);

        listaServicios.setCellFactory(lv -> new ListCell<LogisticaDTO>() {
            @Override
            protected void updateItem(LogisticaDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                // Nombre del servicio
                Label lblNombre = new Label(item.getNombreServicio() != null ? item.getNombreServicio() : "Servicio");
                lblNombre.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 14px;");

                // Hora
                Label lblHora = new Label("🕐 " + (item.getHoraRequerida() != null
                        ? item.getHoraRequerida().toString() : "Sin hora"));
                lblHora.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

                // Ubicación
                Label lblUbicacion = new Label("📍 " + (item.getUbicacionMontaje() != null
                        ? item.getUbicacionMontaje() : "Sin ubicación"));
                lblUbicacion.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

                // Responsable
                Label lblResponsable = new Label("👤 " + (item.getResponsableTurno() != null
                        ? item.getResponsableTurno() : "Sin asignar"));
                lblResponsable.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

                // Especificaciones
                Label lblEspec = new Label(item.getEspecificaciones() != null
                        ? item.getEspecificaciones() : "");
                lblEspec.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 10px;");
                lblEspec.setWrapText(true);

                VBox infoLeft = new VBox(4, lblNombre, lblHora, lblUbicacion, lblResponsable);
                if (item.getEspecificaciones() != null && !item.getEspecificaciones().isEmpty()) {
                    infoLeft.getChildren().add(lblEspec);
                }

                // ComboBox de estado
                ComboBox<EstadoLogistica> comboEstado = new ComboBox<>(
                        FXCollections.observableArrayList(EstadoLogistica.values()));
                comboEstado.setValue(item.getEstado());
                comboEstado.setPrefWidth(160);
                comboEstado.setConverter(new StringConverter<EstadoLogistica>() {
                    @Override
                    public String toString(EstadoLogistica e) {
                        if (e == null) return "";
                        switch (e) {
                            case POR_CONTACTAR: return "Por Contactar";
                            case ENCARGADO: return "Encargado";
                            case CONFIRMADO: return "Confirmado";
                            case LISTO: return "Listo";
                            default: return e.name();
                        }
                    }
                    @Override
                    public EstadoLogistica fromString(String string) { return null; }
                });

                // Badge de color según estado
                Label lblBadge = new Label();
                lblBadge.setPrefWidth(12);
                lblBadge.setPrefHeight(12);
                lblBadge.setMinWidth(12);
                lblBadge.setMinHeight(12);
                actualizarBadge(lblBadge, item.getEstado());

                comboEstado.setOnAction(e -> {
                    EstadoLogistica nuevoEstado = comboEstado.getValue();
                    if (nuevoEstado != null && nuevoEstado != item.getEstado()) {
                        try {
                            actualizarEstadoUseCase.actualizarEstado(item.getId(), nuevoEstado);
                            item.setEstado(nuevoEstado);
                            actualizarBadge(lblBadge, nuevoEstado);
                        } catch (CotizacionException ex) {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Error");
                            alert.setHeaderText(null);
                            alert.setContentText(ex.getMessage());
                            alert.showAndWait();
                            comboEstado.setValue(item.getEstado());
                        }
                    }
                });

                VBox rightBox = new VBox(8, new HBox(6, lblBadge, comboEstado));
                rightBox.setAlignment(Pos.TOP_RIGHT);

                HBox row = new HBox(16, infoLeft, rightBox);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10));
                row.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8px; -fx-border-color: #e8ecef; -fx-border-radius: 8px;");
                HBox.setHgrow(infoLeft, Priority.ALWAYS);

                setGraphic(row);
                setText(null);
            }
        });
    }

    private void actualizarBadge(Label badge, EstadoLogistica estado) {
        if (estado == null) return;
        String color;
        switch (estado) {
            case POR_CONTACTAR: color = "#e74c3c"; break;
            case ENCARGADO:     color = "#f39c12"; break;
            case CONFIRMADO:    color = "#3498db"; break;
            case LISTO:         color = "#27ae60"; break;
            default:            color = "#bdc3c7"; break;
        }
        badge.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 50; -fx-min-width: 12; -fx-min-height: 12; -fx-max-width: 12; -fx-max-height: 12;");
    }

    @FXML
    private void refrescar() {
        cargarEventos();
        serviciosData.clear();
        if (lblEventoSeleccionado != null) {
            lblEventoSeleccionado.setText("Seleccione un evento");
        }
    }
}
