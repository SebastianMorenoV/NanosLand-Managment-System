package com.mycompany.presentacion.controllers;

import com.example.negocio.agenda.usecase.VerificarDisponibilidadTurnoUseCase;
import com.example.negocio.catalogo.usecase.ConsultarCatalogoUseCase;
import com.example.negocio.cliente.usecase.BuscarClienteUseCase;
import com.example.negocio.cotizacion.usecase.CrearCotizacionUseCase;
import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.ClienteDTO;
import com.mycompany.common.dtos.CotizacionDTO;
import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.presentacion.context.CotizacionContext;
import com.mycompany.presentacion.utils.ViewSwitcher;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.controlsfx.control.SearchableComboBox;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CotizacionController {

    // ─── USE CASES ───────────────────────────────────────────────────────────
    private final BuscarClienteUseCase buscarClienteUseCase;
    private final ConsultarCatalogoUseCase consultarCatalogoUseCase;
    private final VerificarDisponibilidadTurnoUseCase verificarDisponibilidadTurnoUseCase;
    private final CrearCotizacionUseCase crearCotizacionUseCase;
    private final CotizacionContext cotizacionContext;

    // ─── COMPONENTES FXML ─────────────────────────────────────────────────────
    @FXML private SearchableComboBox<ClienteDTO>  comboClientes;
    @FXML private SearchableComboBox<PaqueteDTO>  comboPaquetes;
    @FXML private DatePicker                       datePickerFecha;
    @FXML private ComboBox<TurnoEvento>            comboTurnos;
    @FXML private TextArea                         textAreaNotas;

    @FXML private Label lblNombrePaquete;
    @FXML private Label lblPrecioPaquete;
    @FXML private Label lblDetallesPaquete;
    @FXML private Label lblTotalEstimado;
    @FXML private Label lblErrorTurnos;
    @FXML private TextField txtNombreFestejado;
    @FXML private TextField txtTematica;

    // ─── INICIALIZACIÓN ───────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        configurarComboClientes();
        configurarComboPaquetes();

        LocalDate fechaSeleccionada = cotizacionContext.getFechaSeleccionada();
        if (fechaSeleccionada != null && datePickerFecha != null) {
            datePickerFecha.setValue(fechaSeleccionada);
            configurarComboTurnos(fechaSeleccionada);
        } else {
            comboTurnos.setDisable(true);
        }

        if (datePickerFecha != null) {
            datePickerFecha.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    if (date != null && date.isBefore(LocalDate.now())) {
                        setDisable(true);
                        setStyle("-fx-background-color: #f2f2f2; -fx-text-fill: #b2b2b2;");
                    }
                }
            });

            datePickerFecha.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    configurarComboTurnos(newValue);
                } else {
                    comboTurnos.setDisable(true);
                    comboTurnos.getItems().clear();
                    ocultarErrorTurnos();
                }
            });
        }

        actualizarTotalEstimado();
    }

    // ─── GUARDAR COTIZACION ───────────────────────────────────────────────────
    @FXML
    private void guardarCotizacion() {
        // Captura de valores desde los controles
        ClienteDTO clienteSeleccionado = comboClientes.getValue();
        PaqueteDTO paqueteSeleccionado = comboPaquetes.getValue();
        LocalDate fecha                = datePickerFecha.getValue();
        TurnoEvento turno              = comboTurnos.getValue();
        String notas                   = textAreaNotas.getText();
        String nombreFestejado         = txtNombreFestejado.getText();
        String tematica                = txtTematica.getText();

        // Construcción del DTO — las validaciones de negocio las ejecuta el Use Case
        CotizacionDTO dto = new CotizacionDTO();
        dto.setClienteId(clienteSeleccionado != null ? clienteSeleccionado.getId() : null);
        dto.setPaqueteId(paqueteSeleccionado != null ? paqueteSeleccionado.getId() : null);
        dto.setFecha(fecha);
        dto.setTurno(turno);
        dto.setNotas(notas != null && !notas.isBlank() ? notas.trim() : null);
        dto.setNombreFestejado(nombreFestejado != null && !nombreFestejado.isBlank() ? nombreFestejado.trim() : null);
        dto.setTematica(tematica != null && !tematica.isBlank() ? tematica.trim() : null);

        try {
            CotizacionDTO resultado = crearCotizacionUseCase.crearCotizacion(dto);
            mostrarExito(resultado.getFolio());
        } catch (CotizacionException ex) {
            mostrarError(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Ocurrió un error inesperado al guardar la cotización. Intente de nuevo.");
            ex.printStackTrace();
        }
    }

    // ─── ALERTAS ─────────────────────────────────────────────────────────────
    private void mostrarExito(String folio) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Cotización Guardada");
        alert.setHeaderText("¡Cotización guardada exitosamente!");
        alert.setContentText("La cotización fue registrada con el folio:\n\n" + folio
                + "\n\nEstado: BORRADOR");
        alert.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("No se pudo guardar");
        alert.setHeaderText("Revisa los datos ingresados");
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // ─── CONFIGURACIÓN COMBO TURNOS ───────────────────────────────────────────
    private void configurarComboTurnos(LocalDate fecha) {
        ObservableList<TurnoEvento> turnosDisponibles = FXCollections.observableArrayList();

        if (fecha != null && !fecha.isBefore(LocalDate.now())) {
            for (TurnoEvento turno : TurnoEvento.values()) {
                if (verificarDisponibilidadTurnoUseCase.verificar(fecha, turno)) {
                    turnosDisponibles.add(turno);
                }
            }
        }

        comboTurnos.setItems(turnosDisponibles);

        if (turnosDisponibles.isEmpty()) {
            comboTurnos.setValue(null);
            comboTurnos.setPromptText("No hay turnos disponibles");
            comboTurnos.setDisable(true);
            mostrarErrorTurnos();
        } else {
            comboTurnos.setPromptText("Seleccione un turno...");
            comboTurnos.setDisable(false);
            ocultarErrorTurnos();
        }

        comboTurnos.setConverter(new StringConverter<TurnoEvento>() {
            @Override
            public String toString(TurnoEvento turno) {
                if (turno == null) return "";
                String nombre = turno.name();
                return nombre.substring(0, 1).toUpperCase() + nombre.substring(1).toLowerCase();
            }

            @Override
            public TurnoEvento fromString(String string) {
                return null;
            }
        });
    }

    private void mostrarErrorTurnos() {
        if (lblErrorTurnos != null) {
            lblErrorTurnos.setVisible(true);
            lblErrorTurnos.setManaged(true);
        }
        if (datePickerFecha != null) {
            datePickerFecha.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px; -fx-border-radius: 4px;");
        }
    }

    private void ocultarErrorTurnos() {
        if (lblErrorTurnos != null) {
            lblErrorTurnos.setVisible(false);
            lblErrorTurnos.setManaged(false);
        }
        if (datePickerFecha != null) {
            datePickerFecha.setStyle("");
        }
    }

    // ─── CONFIGURACIÓN COMBO CLIENTES ─────────────────────────────────────────
    private void configurarComboClientes() {
        List<ClienteDTO> clienteDTOS = buscarClienteUseCase.obtenerTodos();
        ObservableList<ClienteDTO> listaClientes = FXCollections.observableArrayList(clienteDTOS);

        comboClientes.setConverter(new StringConverter<ClienteDTO>() {
            @Override
            public String toString(ClienteDTO cliente) {
                if (cliente == null) return "";
                String telefono = cliente.getTelefono() != null ? cliente.getTelefono() : "";
                return cliente.getNombre() + " - " + telefono;
            }

            @Override
            public ClienteDTO fromString(String string) {
                return null;
            }
        });

        comboClientes.setCellFactory(listView -> new ListCell<ClienteDTO>() {
            @Override
            protected void updateItem(ClienteDTO cliente, boolean empty) {
                super.updateItem(cliente, empty);
                if (empty || cliente == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    VBox contenedor = new VBox(2);

                    Label lblNombre = new Label(cliente.getNombre());
                    lblNombre.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");

                    String telefono = cliente.getTelefono() != null
                            ? cliente.getTelefono() : "Sin teléfono";
                    Label lblDetalles = new Label("📞 " + telefono);
                    lblDetalles.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

                    contenedor.getChildren().addAll(lblNombre, lblDetalles);
                    setGraphic(contenedor);
                    setText(null);
                }
            }
        });

        comboClientes.setItems(listaClientes);
    }

    // ─── CONFIGURACIÓN COMBO PAQUETES ─────────────────────────────────────────
    private void configurarComboPaquetes() {
        List<PaqueteDTO> paqueteDTOS = consultarCatalogoUseCase.obtenerTodosLosPaquetes();
        ObservableList<PaqueteDTO> listaPaquetes = FXCollections.observableArrayList(paqueteDTOS);

        comboPaquetes.setItems(listaPaquetes);

        comboPaquetes.setConverter(new StringConverter<PaqueteDTO>() {
            @Override
            public String toString(PaqueteDTO paquete) {
                return paquete == null ? "" : paquete.getNombre();
            }

            @Override
            public PaqueteDTO fromString(String string) {
                return null;
            }
        });

        comboPaquetes.setOnAction(event -> {
            PaqueteDTO paqueteSeleccionado = comboPaquetes.getValue();
            if (paqueteSeleccionado != null) {
                lblNombrePaquete.setText(paqueteSeleccionado.getNombre());
                lblPrecioPaquete.setText("$" + String.format("%,.2f", paqueteSeleccionado.getCostoBase()));
                String detalles = paqueteSeleccionado.getDescripcion() != null
                        ? paqueteSeleccionado.getDescripcion() : "Sin detalles adicionales";
                lblDetallesPaquete.setText(detalles);
            }
            actualizarTotalEstimado();
        });
    }

    // ─── CÁLCULO DE TOTAL ESTIMADO ────────────────────────────────────────────
    private void actualizarTotalEstimado() {
        double costoPaquete = 0.0;
        PaqueteDTO paqueteSeleccionado = comboPaquetes.getValue();
        if (paqueteSeleccionado != null) {
            costoPaquete = paqueteSeleccionado.getCostoBase();
        }

        // TODO: Sumar costo de servicios extras cuando se implemente esa sección
        double total = costoPaquete;

        if (lblTotalEstimado != null) {
            lblTotalEstimado.setText(String.format("$%,.2f", total));
        }
    }

    // ─── MODAL NUEVO CLIENTE ─────────────────────────────────────────────────
    @FXML
    private void abrirModalNuevoCliente() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass()
                    .getResource("/com/mycompany/presentacion/views/NuevoClienteModal.fxml"));
            loader.setControllerFactory(ViewSwitcher.getSpringContext()::getBean);

            Parent root = loader.load();

            Stage modalStage = new Stage();
            modalStage.setTitle("Registrar Nuevo Cliente");
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.initStyle(StageStyle.UTILITY);
            modalStage.setScene(new Scene(root));
            modalStage.setResizable(false);
            modalStage.showAndWait();

            // Refrescar combo al cerrar por si se agregó un cliente nuevo
            configurarComboClientes();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}