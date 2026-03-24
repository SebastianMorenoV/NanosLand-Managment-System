package com.mycompany.presentacion.controllers;

import com.example.negocio.agenda.usecase.VerificarDisponibilidadTurnoUseCase;
import com.example.negocio.catalogo.usecase.ConsultarCatalogoUseCase;
import com.example.negocio.cliente.usecase.BuscarClienteUseCase;
import com.example.negocio.cotizacion.usecase.CalcularTotalCotizacionUseCase;
import com.example.negocio.cotizacion.usecase.CrearCotizacionUseCase;
import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.ClienteDTO;
import com.mycompany.common.dtos.CotizacionDTO;
import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.dtos.ServicioDTO;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.presentacion.context.CotizacionContext;
import com.mycompany.presentacion.utils.ViewSwitcher;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener; 
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets; 
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane; 
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
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CotizacionController {

    // ─── USE CASES ───────────────────────────────────────────────────────────
    private final BuscarClienteUseCase buscarClienteUseCase;
    private final ConsultarCatalogoUseCase consultarCatalogoUseCase;
    private final VerificarDisponibilidadTurnoUseCase verificarDisponibilidadTurnoUseCase;
    private final CalcularTotalCotizacionUseCase calcularTotalCotizacionUseCase;
    private final CrearCotizacionUseCase crearCotizacionUseCase;
    private final CotizacionContext cotizacionContext;

    // ─── COMPONENTES FXML ─────────────────────────────────────────────────────
    @FXML
    private SearchableComboBox<ClienteDTO> comboClientes;
    @FXML
    private SearchableComboBox<PaqueteDTO> comboPaquetes;
    @FXML
    private DatePicker datePickerFecha;
    @FXML
    private ComboBox<TurnoEvento> comboTurnos;
    @FXML
    private TextArea textAreaNotas;

    @FXML
    private Label lblNombrePaquete;
    @FXML
    private Label lblPrecioPaquete;
    @FXML
    private Label lblDetallesPaquete;
    @FXML
    private Label lblTotalEstimado;
    @FXML
    private Label lblErrorTurnos;
    @FXML
    private TextField txtNombreFestejado;
    @FXML
    private TextField txtTematica;

    // Elementos para Servicios Extras (Flujo 2.2.3 y 2.2.4)
    @FXML
    private ListView<ServicioDTO> listaServiciosExtras;

    // ─── INICIALIZACIÓN ───────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        configurarComboClientes();
        configurarComboPaquetes();
        configurarListaServiciosExtras();

        LocalDate fechaSeleccionada = cotizacionContext.getFechaSeleccionada();
        if (fechaSeleccionada != null && datePickerFecha != null) {
            datePickerFecha.setValue(fechaSeleccionada);
            configurarComboTurnos(fechaSeleccionada);
        } else {
            comboTurnos.setDisable(true);
        }

        if (datePickerFecha != null) {
            // Bloquear escritura manual
            datePickerFecha.setEditable(false);

            datePickerFecha.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    if (date != null) {
                        if (date.isBefore(LocalDate.now())) {
                            setDisable(true);
                            setStyle("-fx-background-color: #f2f2f2; -fx-text-fill: #b2b2b2;");
                        } else {
                            // Lógica de colores según Flujo 2.2.1 y 2.2.2
                            String estado = verificarDisponibilidadTurnoUseCase.obtenerEstadoDisponibilidad(date);
                            if (estado.equals("GRIS")) {
                                setDisable(true);
                                setStyle("-fx-background-color: #bdc3c7;"); // Gris (Bloqueado)
                            } else if (estado.equals("AMARILLO")) {
                                setStyle("-fx-background-color: #f1c40f;"); // Amarillo (Parcial)
                            }
                        }
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
        LocalDate fecha = datePickerFecha.getValue();
        TurnoEvento turno = comboTurnos.getValue();
        String notas = textAreaNotas.getText();
        String nombreFestejado = txtNombreFestejado.getText();
        String tematica = txtTematica.getText();

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
        alert.setTitle("Error");
        alert.setHeaderText("Revisa los datos ingresados");
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // ─── CONFIGURACIÓN COMBO TURNOS ───────────────────────────────────────────
    private void configurarComboTurnos(LocalDate fecha) {
        ObservableList<TurnoEvento> turnosDisponibles = FXCollections.observableArrayList();

        if (fecha != null && !fecha.isBefore(LocalDate.now())) {
            for (TurnoEvento turno : TurnoEvento.values()) {
                if (verificarDisponibilidadTurnoUseCase.esTurnoDisponible(fecha, turno)) {
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
            
            String estado = verificarDisponibilidadTurnoUseCase.obtenerEstadoDisponibilidad(fecha);
            if (estado.equals("AMARILLO")) {
                comboTurnos.setPromptText("Día Parcial: Seleccione un turno libre");
            }
        }

        comboTurnos.setConverter(new StringConverter<TurnoEvento>() {
            @Override
            public String toString(TurnoEvento turno) {
                if (turno == null) return "";
                String nombre = turno.name();
                return nombre.substring(0, 1).toUpperCase() + nombre.substring(1).toLowerCase();
            }
            @Override
            public TurnoEvento fromString(String string) { return null; }
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
            public ClienteDTO fromString(String string) { return null; }
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
                    String telefono = cliente.getTelefono() != null ? cliente.getTelefono() : "Sin teléfono";
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
            public PaqueteDTO fromString(String string) { return null; }
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

    // ─── CONFIGURACIÓN LISTA SERVICIOS EXTRAS ─────────────────────────
    private void configurarListaServiciosExtras() {
        List<ServicioDTO> todosLosServicios = consultarCatalogoUseCase.obtenerTodosLosServicios();
        ObservableList<ServicioDTO> items = FXCollections.observableArrayList(todosLosServicios);
        
        if (listaServiciosExtras != null) {
            listaServiciosExtras.setItems(items);
            
            listaServiciosExtras.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            
            listaServiciosExtras.getSelectionModel().getSelectedItems().addListener((ListChangeListener<ServicioDTO>) c -> {
                actualizarTotalEstimado();
            });

            listaServiciosExtras.setCellFactory(lv -> new ListCell<ServicioDTO>() {
                @Override
                protected void updateItem(ServicioDTO item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getNombre() + " (+$" + String.format("%,.2f", item.getPrecio()) + ")");
                    }
                }
            });
        }
    }

    // ─── CÁLCULO DE TOTAL ESTIMADO ────────────────────────────────────────────
    private void actualizarTotalEstimado() {
        PaqueteDTO paqueteSeleccionado = comboPaquetes.getValue();
        List<ServicioDTO> extras = (listaServiciosExtras != null) 
                ? new ArrayList<>(listaServiciosExtras.getSelectionModel().getSelectedItems()) 
                : new ArrayList<>();

        Double total = calcularTotalCotizacionUseCase.ejecutar(paqueteSeleccionado, extras);

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

            configurarComboClientes();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ─── ACCIÓN DEL BOTÓN AÑADIR SERVICIO AL SISTEMA ──────────────────────
    @FXML
    private void abrirDialogoAgregarServicio() {
        // Creamos un Modal nativo de JavaFX
        Dialog<ServicioDTO> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Servicio");
        dialog.setHeaderText("Añadir un nuevo servicio al catálogo");

        // Botones del modal
        ButtonType guardarButtonType = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarButtonType, ButtonType.CANCEL);

        // Cuadrícula para los textos (Actualizado con Descripción)
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Ej. Toro Mecánico");
        
        TextField txtPrecio = new TextField();
        txtPrecio.setPromptText("Ej. 1500.00");
        
        TextField txtDescripcion = new TextField();
        txtDescripcion.setPromptText("Ej. Renta por 4 horas con operador");

        grid.add(new Label("Nombre del Servicio:"), 0, 0);
        grid.add(txtNombre, 1, 0);
        
        grid.add(new Label("Precio (MXN):"), 0, 1);
        grid.add(txtPrecio, 1, 1);
        
        grid.add(new Label("Descripción:"), 0, 2);
        grid.add(txtDescripcion, 1, 2);

        // Habilitar el botón Guardar solo si Nombre y Precio tienen texto (la descripción es opcional)
        javafx.scene.Node guardarBtn = dialog.getDialogPane().lookupButton(guardarButtonType);
        guardarBtn.setDisable(true);
        
        // Listeners para validación básica
        txtNombre.textProperty().addListener((observable, oldValue, newValue) -> {
            guardarBtn.setDisable(newValue.trim().isEmpty() || txtPrecio.getText().trim().isEmpty());
        });
        txtPrecio.textProperty().addListener((observable, oldValue, newValue) -> {
            guardarBtn.setDisable(newValue.trim().isEmpty() || txtNombre.getText().trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Ejecutar al hacer clic en Guardar
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarButtonType) {
                try {
                    String nom = txtNombre.getText().trim();
                    Double prec = Double.parseDouble(txtPrecio.getText().trim());
                    String desc = txtDescripcion.getText().trim();
                    
                    // Usamos la capa de negocio para insertarlo en MySQL
                    return consultarCatalogoUseCase.guardarNuevoServicio(nom, prec, desc);
                } catch (NumberFormatException e) {
                    mostrarError("El precio debe ser un número válido sin letras ni símbolos.");
                } catch (Exception e) {
                    mostrarError("Error al guardar en la base de datos.");
                    e.printStackTrace();
                }
            }
            return null;
        });

        // Mostrar el modal y refrescar la lista si hubo éxito
        dialog.showAndWait().ifPresent(nuevoServicio -> {
            Alert exito = new Alert(Alert.AlertType.INFORMATION, "El servicio '" + nuevoServicio.getNombre() + "' fue agregado exitosamente.");
            exito.showAndWait();
            configurarListaServiciosExtras(); // Volvemos a cargar la lista para que se muestre el nuevo
        });
    }
}