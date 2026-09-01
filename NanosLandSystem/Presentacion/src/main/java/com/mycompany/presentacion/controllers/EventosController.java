package com.mycompany.presentacion.controllers;

import com.example.negocio.evento.usecase.ConsultarEventosUseCase;
import com.mycompany.common.dtos.EventoDTO;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.presentacion.utils.ViewSwitcher;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class EventosController {

    private final ConsultarEventosUseCase consultarEventosUseCase;

    // Tarjetas KPI
    @FXML private Label lblKpiTotal;
    @FXML private Label lblKpiTentativos;
    @FXML private Label lblKpiConfirmados;
    @FXML private Label lblKpiEnCurso;
    @FXML private Label lblKpiFinalizados;
    @FXML private Label lblKpiCancelados;

    // Filtros
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<EstadoEvento> cmbEstado;
    @FXML private ComboBox<TurnoEvento> cmbTurno;
    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFin;

    // Tabla y Paginación
    @FXML private TableView<EventoDTO> tablaEventos;
    @FXML private TableColumn<EventoDTO, String> colFolio;
    @FXML private TableColumn<EventoDTO, LocalDate> colFecha;
    @FXML private TableColumn<EventoDTO, TurnoEvento> colTurno;
    @FXML private TableColumn<EventoDTO, String> colCliente;
    @FXML private TableColumn<EventoDTO, String> colFestejado;
    @FXML private TableColumn<EventoDTO, String> colPaquete;
    @FXML private TableColumn<EventoDTO, Double> colTotal;
    @FXML private TableColumn<EventoDTO, EstadoEvento> colEstado;
    @FXML private TableColumn<EventoDTO, Void> colAcciones;
    @FXML private Pagination paginacion;

    private static final int ITEMS_POR_PAGINA = 15;
    private final ObservableList<EventoDTO> masterData = FXCollections.observableArrayList();
    private final ObservableList<EventoDTO> filteredData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltros();
        recargarEventos();
    }

    private void configurarTabla() {
        if (tablaEventos != null) {
            tablaEventos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

            tablaEventos.setRowFactory(tv -> {
                TableRow<EventoDTO> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && (!row.isEmpty())) {
                        abrirModalDetalles(row.getItem());
                    }
                });
                return row;
            });
        }

        colFolio.setCellValueFactory(new PropertyValueFactory<>("folioCotizacion"));
        colFolio.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #1a82b8; -fx-alignment: CENTER;");
                }
            }
        });

        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        colFecha.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(dtf.format(item));
                    setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
                }
            }
        });

        colTurno.setCellValueFactory(new PropertyValueFactory<>("turno"));
        colTurno.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(TurnoEvento item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String str = item.name();
                    setText(str.charAt(0) + str.substring(1).toLowerCase());
                    setStyle("-fx-alignment: CENTER; -fx-text-fill: #7f8c8d;");
                }
            }
        });

        colCliente.setCellValueFactory(new PropertyValueFactory<>("clienteNombre"));

        colFestejado.setCellValueFactory(cellData -> {
            EventoDTO dto = cellData.getValue();
            String fest = dto.getNombreFestejado() != null ? dto.getNombreFestejado() : "";
            String tema = (dto.getTematica() != null && !dto.getTematica().isBlank()) ? " (" + dto.getTematica() + ")" : "";
            return new javafx.beans.property.SimpleStringProperty(fest + tema);
        });

        colPaquete.setCellValueFactory(new PropertyValueFactory<>("paqueteNombre"));

        colTotal.setCellValueFactory(cellData -> {
            EventoDTO dto = cellData.getValue();
            double total = dto.getTotalCotizacion() + dto.getTotalCargosExtras();
            return new javafx.beans.property.SimpleObjectProperty<>(total);
        });
        colTotal.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("$%,.2f", item));
                    setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
                }
            }
        });

        // Columna Estado con Badge estilizado
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoEvento"));
        colEstado.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(EstadoEvento item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item.name());
                    badge.setAlignment(Pos.CENTER);
                    badge.setPrefWidth(105);

                    switch (item) {
                        case TENTATIVO:
                            badge.setStyle("-fx-background-color: #fef5e7; -fx-text-fill: #f39c12; -fx-border-color: #f39c12; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 3 6 3 6; -fx-font-size: 11px; -fx-font-weight: bold;");
                            break;
                        case CONFIRMADO:
                            badge.setStyle("-fx-background-color: #ebf5fb; -fx-text-fill: #1a82b8; -fx-border-color: #1a82b8; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 3 6 3 6; -fx-font-size: 11px; -fx-font-weight: bold;");
                            break;
                        case EN_CURSO:
                            badge.setStyle("-fx-background-color: #f4ecf7; -fx-text-fill: #8e44ad; -fx-border-color: #8e44ad; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 3 6 3 6; -fx-font-size: 11px; -fx-font-weight: bold;");
                            break;
                        case FINALIZADO:
                            badge.setStyle("-fx-background-color: #eafaf1; -fx-text-fill: #27ae60; -fx-border-color: #27ae60; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 3 6 3 6; -fx-font-size: 11px; -fx-font-weight: bold;");
                            break;
                        case CANCELADO:
                            badge.setStyle("-fx-background-color: #fdedec; -fx-text-fill: #e74c3c; -fx-border-color: #e74c3c; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 3 6 3 6; -fx-font-size: 11px; -fx-font-weight: bold;");
                            break;
                        default:
                            badge.setStyle("-fx-background-color: #eaecee; -fx-text-fill: #7f8c8d; -fx-padding: 3 6 3 6; -fx-background-radius: 10px; -fx-font-size: 11px; -fx-font-weight: bold;");
                            break;
                    }

                    HBox container = new HBox(badge);
                    container.setAlignment(Pos.CENTER);
                    setGraphic(container);
                    setText(null);
                }
            }
        });

        // Columna de Acciones
        colAcciones.setCellFactory(column -> new TableCell<>() {
            private final Button btnDetalle = new Button("Detalle");

            {
                btnDetalle.getStyleClass().add("boton-azul-claro");
                btnDetalle.setStyle("-fx-font-size: 11px; -fx-padding: 4 10 4 10; -fx-cursor: hand;");
                btnDetalle.setOnAction(event -> {
                    EventoDTO evento = getTableView().getItems().get(getIndex());
                    if (evento != null) {
                        abrirModalDetalles(evento);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox container = new HBox(btnDetalle);
                    container.setAlignment(Pos.CENTER);
                    setGraphic(container);
                }
            }
        });

        paginacion.setPageCount(1);
        paginacion.setPageFactory(this::crearPagina);
    }

    private void configurarFiltros() {
        cmbEstado.setItems(FXCollections.observableArrayList(EstadoEvento.values()));
        cmbTurno.setItems(FXCollections.observableArrayList(TurnoEvento.values()));

        if (dpInicio != null) dpInicio.setEditable(false);
        if (dpFin != null) dpFin.setEditable(false);

        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());
        cmbEstado.valueProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());
        cmbTurno.valueProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());
        dpInicio.valueProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());
        dpFin.valueProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());
    }

    private Node crearPagina(int pageIndex) {
        int fromIndex = pageIndex * ITEMS_POR_PAGINA;
        int toIndex = Math.min(fromIndex + ITEMS_POR_PAGINA, filteredData.size());

        if (fromIndex < filteredData.size() && fromIndex <= toIndex) {
            tablaEventos.setItems(FXCollections.observableArrayList(filteredData.subList(fromIndex, toIndex)));
        } else {
            tablaEventos.setItems(FXCollections.observableArrayList());
        }
        return tablaEventos;
    }

    @FXML
    public void recargarEventos() {
        try {
            List<EventoDTO> eventos = consultarEventosUseCase.listarTodosEventos();
            masterData.clear();
            if (eventos != null) {
                masterData.addAll(eventos);
            }
            actualizarKpis(masterData);
            aplicarFiltros();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error al cargar eventos");
            alert.setContentText("No se pudieron consultar los eventos desde la base de datos.");
            alert.showAndWait();
        }
    }

    private void actualizarKpis(List<EventoDTO> eventos) {
        int total = eventos.size();
        long tentativos = eventos.stream().filter(e -> e.getEstadoEvento() == EstadoEvento.TENTATIVO).count();
        long confirmados = eventos.stream().filter(e -> e.getEstadoEvento() == EstadoEvento.CONFIRMADO).count();
        long enCurso = eventos.stream().filter(e -> e.getEstadoEvento() == EstadoEvento.EN_CURSO).count();
        long finalizados = eventos.stream().filter(e -> e.getEstadoEvento() == EstadoEvento.FINALIZADO).count();
        long cancelados = eventos.stream().filter(e -> e.getEstadoEvento() == EstadoEvento.CANCELADO).count();

        lblKpiTotal.setText(String.valueOf(total));
        lblKpiTentativos.setText(String.valueOf(tentativos));
        lblKpiConfirmados.setText(String.valueOf(confirmados));
        lblKpiEnCurso.setText(String.valueOf(enCurso));
        lblKpiFinalizados.setText(String.valueOf(finalizados));
        lblKpiCancelados.setText(String.valueOf(cancelados));
    }

    private void aplicarFiltros() {
        String texto = txtBuscar.getText() != null ? txtBuscar.getText().trim().toLowerCase() : "";
        EstadoEvento estado = cmbEstado.getValue();
        TurnoEvento turno = cmbTurno.getValue();
        LocalDate inicio = dpInicio.getValue();
        LocalDate fin = dpFin.getValue();

        List<EventoDTO> resultado = masterData.stream().filter(e -> {
            // Filtro de texto
            if (!texto.isEmpty()) {
                boolean coincideFolio = e.getFolioCotizacion() != null && e.getFolioCotizacion().toLowerCase().contains(texto);
                boolean coincideCliente = e.getClienteNombre() != null && e.getClienteNombre().toLowerCase().contains(texto);
                boolean coincideFestejado = e.getNombreFestejado() != null && e.getNombreFestejado().toLowerCase().contains(texto);
                boolean coincideTematica = e.getTematica() != null && e.getTematica().toLowerCase().contains(texto);
                boolean coincidePaquete = e.getPaqueteNombre() != null && e.getPaqueteNombre().toLowerCase().contains(texto);

                if (!coincideFolio && !coincideCliente && !coincideFestejado && !coincideTematica && !coincidePaquete) {
                    return false;
                }
            }

            // Filtro de estado
            if (estado != null && e.getEstadoEvento() != estado) {
                return false;
            }

            // Filtro de turno
            if (turno != null && e.getTurno() != turno) {
                return false;
            }

            // Filtro de rango de fechas
            if (inicio != null && e.getFecha() != null && e.getFecha().isBefore(inicio)) {
                return false;
            }
            if (fin != null && e.getFecha() != null && e.getFecha().isAfter(fin)) {
                return false;
            }

            return true;
        }).collect(Collectors.toList());

        filteredData.clear();
        filteredData.addAll(resultado);

        int pageCount = (int) Math.ceil((double) filteredData.size() / ITEMS_POR_PAGINA);
        paginacion.setPageCount(pageCount == 0 ? 1 : pageCount);
        paginacion.setCurrentPageIndex(0);
        crearPagina(0);
    }

    @FXML
    private void limpiarFiltros() {
        txtBuscar.clear();
        cmbEstado.setValue(null);
        cmbTurno.setValue(null);
        dpInicio.setValue(null);
        dpFin.setValue(null);
    }

    private void abrirModalDetalles(EventoDTO evento) {
        if (evento == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/presentacion/views/EventoDetallesModal.fxml"));
            loader.setControllerFactory(ViewSwitcher.getSpringContext()::getBean);
            Parent root = loader.load();

            EventoDetallesModalController controller = loader.getController();
            controller.setEvento(evento, this::recargarEventos);

            Stage stage = new Stage();
            stage.setTitle("Detalles del Evento: " + (evento.getFolioCotizacion() != null ? evento.getFolioCotizacion() : ""));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(tablaEventos.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se pudo abrir el detalle del evento");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
