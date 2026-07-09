package com.mycompany.presentacion.controllers;

import com.example.negocio.estadoCuenta.usecase.ConsultarEstadoCuentaUseCase;
import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.CargoExtraDTO;
import com.mycompany.common.dtos.EstadoCuentaDTO;
import com.mycompany.common.dtos.PagoDTO;
import com.mycompany.common.dtos.ServicioExtraDTO;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.presentacion.utils.ViewSwitcher;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class EstadoCuentaController {

    private final ConsultarEstadoCuentaUseCase consultarEstadoCuentaUseCase;

    @FXML private TextField txtBuscar;
    @FXML private ListView<Evento> listaEventos;
    @FXML private VBox panelDetalle;

    // Etiquetas Resumen
    @FXML private Label lblFolio;
    @FXML private Label lblCliente;
    @FXML private Label lblFecha;
    @FXML private Label lblEstado;

    // Listas
    @FXML private ListView<String> listaPresupuesto;
    @FXML private ListView<String> listaCargos;
    @FXML private ListView<String> listaPagos;

    // Totales
    @FXML private Label lblGranTotal;
    @FXML private Label lblPagado;
    @FXML private Label lblSaldoPendiente;

    private final ObservableList<Evento> eventosMaster = FXCollections.observableArrayList();
    private EstadoCuentaDTO estadoCuentaActual;

    @FXML
    public void initialize() {
        panelDetalle.setVisible(false);
        configurarListaEventos();
        cargarEventos();
    }

    private void cargarEventos() {
        List<Evento> eventos = consultarEstadoCuentaUseCase.obtenerEventosCobranza();
        eventosMaster.clear();
        eventosMaster.addAll(eventos);

        txtBuscar.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isEmpty()) {
                listaEventos.setItems(eventosMaster);
            } else {
                String lower = val.toLowerCase();
                ObservableList<Evento> filtrados = FXCollections.observableArrayList();
                for (Evento e : eventosMaster) {
                    boolean coincide = false;
                    if (e.getCotizacion() != null) {
                        if (e.getCotizacion().getFolio() != null && e.getCotizacion().getFolio().toLowerCase().contains(lower)) {
                            coincide = true;
                        }
                        if (e.getCotizacion().getCliente() != null && e.getCotizacion().getCliente().getNombre().toLowerCase().contains(lower)) {
                            coincide = true;
                        }
                    }
                    if (coincide) filtrados.add(e);
                }
                listaEventos.setItems(filtrados);
            }
        });
    }

    private void configurarListaEventos() {
        listaEventos.setItems(eventosMaster);
        listaEventos.setCellFactory(lv -> new ListCell<Evento>() {
            @Override
            protected void updateItem(Evento item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String folio = item.getCotizacion() != null ? item.getCotizacion().getFolio() : "N/A";
                    String cliente = item.getCotizacion() != null && item.getCotizacion().getCliente() != null
                            ? item.getCotizacion().getCliente().getNombre() : "Sin Cliente";
                    String fecha = item.getFecha() != null ? item.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";

                    Label lbl1 = new Label(folio + " - " + cliente);
                    lbl1.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");
                    Label lbl2 = new Label("📅 " + fecha + "  [" + item.getEstado().name() + "]");
                    lbl2.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
                    
                    VBox box = new VBox(4, lbl1, lbl2);
                    box.setPadding(new Insets(5));
                    setGraphic(box);
                }
            }
        });

        listaEventos.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                cargarEstadoCuenta(newVal.getId());
            } else {
                panelDetalle.setVisible(false);
            }
        });
    }

    public void cargarEstadoCuenta(Long eventoId) {
        try {
            estadoCuentaActual = consultarEstadoCuentaUseCase.generarEstadoCuenta(eventoId);
            mostrarDetalles(estadoCuentaActual);
            panelDetalle.setVisible(true);
        } catch (CotizacionException ex) {
            mostrarAlerta("Error", ex.getMessage());
        }
    }

    private void mostrarDetalles(EstadoCuentaDTO dto) {
        lblFolio.setText("Folio: " + dto.getFolioCotizacion());
        lblCliente.setText("Cliente: " + dto.getClienteNombre());
        lblFecha.setText("Fecha: " + dto.getFechaEventoFormateada());
        lblEstado.setText("Estado: " + dto.getEstadoEvento());

        // Presupuesto
        ObservableList<String> itemsPresupuesto = FXCollections.observableArrayList();
        itemsPresupuesto.add(String.format("Paquete Base (%s): $%,.2f", dto.getNombrePaqueteBase(), dto.getPrecioPaqueteBase()));
        if (dto.getServiciosExtrasOriginales() != null) {
            for (ServicioExtraDTO s : dto.getServiciosExtrasOriginales()) {
                itemsPresupuesto.add(String.format("+ %d x %s ($%,.2f c/u): $%,.2f", s.getCantidad(), s.getNombre(), s.getPrecioUnitario(), s.getSubtotal()));
            }
        }
        listaPresupuesto.setItems(itemsPresupuesto);

        // Cargos Extras
        ObservableList<String> itemsCargos = FXCollections.observableArrayList();
        if (dto.getCargosExtras() != null && !dto.getCargosExtras().isEmpty()) {
            for (CargoExtraDTO c : dto.getCargosExtras()) {
                String f = c.getFechaHoraCargo() != null ? c.getFechaHoraCargo().format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")) : "";
                itemsCargos.add(String.format("[%s] %s (x%d): $%,.2f", f, c.getDescripcion(), c.getCantidad(), c.getSubtotal()));
            }
        } else {
            itemsCargos.add("Sin cargos adicionales.");
        }
        listaCargos.setItems(itemsCargos);

        // Pagos
        ObservableList<String> itemsPagos = FXCollections.observableArrayList();
        if (dto.getPagosRealizados() != null && !dto.getPagosRealizados().isEmpty()) {
            for (PagoDTO p : dto.getPagosRealizados()) {
                String f = p.getFechaHora() != null ? p.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")) : "";
                itemsPagos.add(String.format("[%s] %s (%s): $%,.2f", f, p.getFolioPago(), p.getTipo(), p.getCantidad()));
            }
        } else {
            itemsPagos.add("Aún no hay pagos registrados.");
        }
        listaPagos.setItems(itemsPagos);

        // Totales
        lblGranTotal.setText(String.format("$%,.2f", dto.getGranTotal()));
        lblPagado.setText(String.format("$%,.2f", dto.getTotalPagado()));
        lblSaldoPendiente.setText(String.format("$%,.2f", dto.getSaldoPendiente()));
    }

    @FXML
    private void abrirModalCargoExtra() {
        if (estadoCuentaActual == null) return;
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/presentacion/views/NuevoCargoExtraModal.fxml"));
            loader.setControllerFactory(ViewSwitcher.getSpringContext()::getBean);
            Parent root = loader.load();

            NuevoCargoExtraModalController controller = loader.getController();
            controller.setEventoId(estadoCuentaActual.getEventoId());

            Stage stage = new Stage();
            stage.setTitle("Registrar Cargo Extra");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            if (controller.isCargoRegistrado()) {
                // Recargar el estado de cuenta
                cargarEstadoCuenta(estadoCuentaActual.getEventoId());
            }
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir la ventana de cargos extras.");
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
