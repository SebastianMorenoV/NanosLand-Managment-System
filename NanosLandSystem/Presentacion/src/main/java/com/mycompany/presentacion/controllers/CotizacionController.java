package com.mycompany.presentacion.controllers;

import com.example.negocio.agenda.usecase.VerificarDisponibilidadTurnoUseCase;
import com.example.negocio.catalogo.usecase.ConsultarCatalogoUseCase;
import com.example.negocio.cliente.usecase.BuscarClienteUseCase;
import com.example.negocio.cotizacion.usecase.EliminarCotizacionUseCase;
import com.example.negocio.cotizacion.usecase.CalcularTotalCotizacionUseCase;
import com.example.negocio.cotizacion.usecase.CrearCotizacionUseCase;
import com.example.negocio.cotizacion.usecase.GenerarComprobanteUseCase;
import com.example.negocio.cotizacion.usecase.ListarCotizacionesUseCase;
import com.example.negocio.cotizacion.usecase.RegistrarAnticipoUseCase;
import com.example.negocio.cotizacion.usecase.ModificarCotizacionUseCase;
import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.ClienteDTO;
import com.mycompany.common.dtos.CotizacionDTO;
import com.mycompany.common.dtos.DetalleCotizacionDTO;
import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.dtos.ServicioDTO;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.MetodoPago;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.presentacion.context.CotizacionContext;
import com.mycompany.presentacion.utils.ViewSwitcher;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener; 
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets; 
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.scene.layout.GridPane; 
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.controlsfx.control.SearchableComboBox;
import org.springframework.stereotype.Controller;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.layout.Priority;

@Controller
@RequiredArgsConstructor
public class CotizacionController {

    // ─── USE CASES ───────────────────────────────────────────────────────────
    private final BuscarClienteUseCase buscarClienteUseCase;
    private final ConsultarCatalogoUseCase consultarCatalogoUseCase;
    private final VerificarDisponibilidadTurnoUseCase verificarDisponibilidadTurnoUseCase;
    private final CalcularTotalCotizacionUseCase calcularTotalCotizacionUseCase;
    private final CrearCotizacionUseCase crearCotizacionUseCase;
    private final ListarCotizacionesUseCase listarCotizacionesUseCase;
    private final ModificarCotizacionUseCase modificarCotizacionUseCase;
    private final EliminarCotizacionUseCase eliminarCotizacionUseCase;
    private final GenerarComprobanteUseCase generarComprobanteUseCase;
    private final RegistrarAnticipoUseCase registrarAnticipoUseCase;
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
    @FXML private VBox panelPagos;
    @FXML private Label lblPagado;
    @FXML private Label lblRestante;
    @FXML private TextField txtAbono;
    // ─── PANEL LATERAL COTIZACIONES ─────────────────────────────────────
    @FXML
    private TextField txtBuscarCotizaciones;
    @FXML
    private ListView<CotizacionDTO> listaCotizaciones;
    @FXML
    private Button btnNuevaCotizacion;
    @FXML
    private Button btnEliminarCotizacion;

    // Elementos para Servicios Extras (Flujo 2.2.3 y 2.2.4)
    @FXML
    private ListView<ServicioDTO> listaServiciosExtras;

    @FXML
    private Button btnAnadirServicio;
    @FXML
    private Button btnGuardarBorrador;
    @FXML
    private Button btnConfirmarCotizacion;

    // ─── ESTADO UI (EDICIÓN) ────────────────────────────────────────────
    private boolean modoEdicion = false;
    private Long cotizacionEnEdicionId;
    private LocalDate fechaEdicionActual;
    private TurnoEvento turnoEdicionActual;

    private final ObservableList<CotizacionDTO> cotizacionesPanel = FXCollections.observableArrayList();
    private FilteredList<CotizacionDTO> cotizacionesFiltradas;
    @FXML private HBox boxBotonesNuevos;
    @FXML private HBox boxBotonesEdicion;
    @FXML private Button btnGuardarCambios;
    @FXML private Button btnDescartarCambios;
    
    // Y añade esta variable para saber en qué estado estaba la cotización que editamos
    private EstadoCotizacion estadoEdicionActual;
    // ─── INICIALIZACIÓN ───────────────────────────────────────────────────────
    private void cambiarModoInterfaz(boolean edicion) {
        if (boxBotonesNuevos != null) {
            boxBotonesNuevos.setVisible(!edicion);
            boxBotonesNuevos.setManaged(!edicion);
        }
        if (boxBotonesEdicion != null) {
            boxBotonesEdicion.setVisible(edicion);
            boxBotonesEdicion.setManaged(edicion);
        }
    }
    
    @FXML
    private void guardarCambios() {
        if (estadoEdicionActual == null) estadoEdicionActual = EstadoCotizacion.BORRADOR;
        
        CotizacionDTO r = guardarOActualizarCotizacion(estadoEdicionActual);
        if (r != null) {
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Cambios guardados exitosamente.");
            ok.showAndWait();
            nuevaCotizacion(); // Vuelve el formulario a su estado en blanco
        }
    }
    @FXML
    public void initialize() {
        configurarComboClientes();

        boolean catalogoDisponible = true;
        try {
            configurarComboPaquetes();
            configurarListaServiciosExtras();
        } catch (Exception ex) {
            catalogoDisponible = false;
            mostrarError("No fue posible obtener el catálogo de servicios/paquetes. El flujo se cancela.");
            deshabilitarFlujoPorCatalogo();
        }

        configurarPanelCotizaciones();

        LocalDate fechaSeleccionada = cotizacionContext.getFechaSeleccionada();
        if (fechaSeleccionada != null && datePickerFecha != null) {
            datePickerFecha.setValue(fechaSeleccionada);
            configurarComboTurnos(fechaSeleccionada);
        } else if (comboTurnos != null) {
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
                        // Si se está editando, permitimos seleccionar la fecha actual aunque no haya turnos
                        if (modoEdicion && fechaEdicionActual != null && date.equals(fechaEdicionActual)) {
                            setDisable(false);
                            return;
                        }

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
        btnEliminarCotizacion.setDisable(true);
        if (!catalogoDisponible) {
            // Garantizamos cancelación del flujo 2.2.8
            btnAnadirServicio.setDisable(true);
            btnGuardarBorrador.setDisable(true);
            btnConfirmarCotizacion.setDisable(true);
        }
    }

    // ─── GUARDAR/ACTUALIZAR COTIZACIÓN ──────────────────────────────────────
    @FXML
    private void guardarBorrador() {
        CotizacionDTO r = guardarOActualizarCotizacion(EstadoCotizacion.BORRADOR);
        if(r != null) {
            actualizarPagosUI();
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Borrador guardado. Folio: " + r.getFolio());
            ok.showAndWait();
        }
    }

   @FXML
    private void confirmarCotizacion() {
        // 1. Intentamos guardar la cotización para validar que no falten campos
        CotizacionDTO r;
        if (!modoEdicion) {
            r = guardarOActualizarCotizacion(EstadoCotizacion.BORRADOR);
        } else {
            r = guardarOActualizarCotizacion(estadoEdicionActual);
        }
        
        // 2. Si 'r' es null, significa que faltaron datos y ya se mostró un error. 
        // DETENEMOS el flujo para que no salga el diálogo del abono.
        if (r == null) {
            return;
        }
        
        // 3. Si todo está correcto, procedemos con los pagos
        double pagado = registrarAnticipoUseCase.obtenerTotalAbonado(cotizacionEnEdicionId);
        if (pagado >= 3000) {
            guardarOActualizarCotizacion(EstadoCotizacion.VIGENTE);
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Cotización confirmada y evento creado (Fechas bloqueadas).");
            ok.showAndWait();
        } else {
            TextInputDialog dialog = new TextInputDialog("3000");
            dialog.setTitle("Confirmar Cotización");
            dialog.setHeaderText("Mínimo $3,000 para confirmar y bloquear la fecha.");
            dialog.setContentText("Ingrese el monto a pagar:");
            
            dialog.showAndWait().ifPresent(montoStr -> {
                try {
                    double monto = Double.parseDouble(montoStr);
                    if (pagado + monto < 3000) {
                        mostrarError("El pago total no alcanza los $3,000 requeridos.");
                        return;
                    }
                    registrarAnticipoUseCase.registrarAnticipo(cotizacionEnEdicionId, monto, MetodoPago.EFECTIVO);
                    guardarOActualizarCotizacion(EstadoCotizacion.VIGENTE);
                    actualizarPagosUI();
                    imprimirComprobante(cotizacionEnEdicionId);
                    
                    Alert ok = new Alert(Alert.AlertType.INFORMATION, "Cotización confirmada y evento creado exitosamente.");
                    ok.showAndWait();
                } catch(Exception e) {
                    mostrarError(e.getMessage());
                }
            });
        }
    }

    private CotizacionDTO guardarOActualizarCotizacion(EstadoCotizacion estadoObjetivo) {
        try {
            CotizacionDTO dto = construirDtoCotizacionDesdeFormulario(estadoObjetivo);
            CotizacionDTO resultado;
            if (modoEdicion) {
                resultado = modificarCotizacionUseCase.modificarCotizacion(cotizacionEnEdicionId, dto);
            } else {
                resultado = crearCotizacionUseCase.crearCotizacion(dto);
                modoEdicion = true;
                cotizacionEnEdicionId = resultado.getId();
            }
            cargarCotizacionesPanel();
            return resultado;
        } catch (CotizacionException ex) {
            mostrarError(ex.getMessage());
            return null;
        } catch (Exception ex) {
            mostrarError("Error inesperado al guardar la cotización.");
            ex.printStackTrace();
            return null;
        }
    }

    // ─── ALERTAS ─────────────────────────────────────────────────────────────
    private void mostrarModalCotizacionGuardada(CotizacionDTO cotizacionGuardada) {
        if (cotizacionGuardada == null) return;

        Stage modalStage = new Stage();
        modalStage.setTitle("Cotización Guardada");
        modalStage.initModality(Modality.APPLICATION_MODAL);
        modalStage.initStyle(StageStyle.UTILITY);
        modalStage.setResizable(false);

        String folio = cotizacionGuardada.getFolio() != null ? cotizacionGuardada.getFolio() : "-";

        VBox root = new VBox();
        root.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 0;");

        VBox header = new VBox();
        header.setStyle("-fx-background-color: #2ecc71; -fx-padding: 16 22 12 22; -fx-background-radius: 10 10 0 0;");

        HBox headerTop = new HBox();
        headerTop.setAlignment(Pos.CENTER_LEFT);

        SVGPath checkIcon = new SVGPath();
        // Check simple
        checkIcon.setContent("M9 11.5L4.5 7l-1.5 1.5L9 14.5 21 2.5 19.5 1 9 11.5Z");
        checkIcon.setFill(javafx.scene.paint.Color.WHITE);
        checkIcon.setScaleX(1.2);
        checkIcon.setScaleY(1.2);
        headerTop.getChildren().add(checkIcon);

        Label title = new Label("¡Cotización Guardada!");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label subtitle = new Label("Folio generado: " + folio);
        subtitle.setStyle("-fx-text-fill: white; -fx-font-size: 11px;");

        VBox headerText = new VBox(title, subtitle);
        headerText.setAlignment(Pos.CENTER_LEFT);
        headerText.setSpacing(4);

        header.getChildren().addAll(headerTop, headerText);

        // Body
        VBox body = new VBox();
        body.setStyle("-fx-padding: 16 22 18 22;");
        body.setSpacing(12);

        Label q = new Label("¿Desea registrar anticipo ahora?");
        q.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label req = new Label("Requerido para hacer el pago final.");
        req.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

        TextField txtAnticipo = new TextField("0.00");
        txtAnticipo.setPrefWidth(120);
        txtAnticipo.setStyle("-fx-text-fill: #2c3e50;");
        txtAnticipo.setPromptText("0.00");

        Button btnRegistrarPago = new Button("Registrar Pago");
        btnRegistrarPago.getStyleClass().add("boton-azul");
        btnRegistrarPago.setOnAction(ev -> {
            try {
                if (cotizacionGuardada.getId() == null) {
                    mostrarError("No se encontró el ID de la cotización para registrar el pago.");
                    return;
                }
                String raw = txtAnticipo.getText();
                if (raw == null || raw.trim().isEmpty()) raw = "0";
                raw = raw.replace("$", "").replace(",", "").trim();

                double anticipo = Double.parseDouble(raw);
                registrarAnticipoUseCase.registrarAnticipo(cotizacionGuardada.getId(), anticipo, MetodoPago.EFECTIVO);

                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Pago registrado");
                ok.setHeaderText(null);
                ok.setContentText("Anticipo registrado exitosamente.");
                ok.showAndWait();
            } catch (CotizacionException ex) {
                mostrarError(ex.getMessage());
            } catch (Exception ex) {
                mostrarError("No se pudo registrar el anticipo. Intente de nuevo.");
            }
        });

        HBox anticipoRow = new HBox(12, txtAnticipo, btnRegistrarPago);
        anticipoRow.setAlignment(Pos.CENTER_LEFT);

        // Footer buttons
        Button btnCrearSalir = new Button("Crear y Salir");
        btnCrearSalir.setStyle("-fx-background-color: transparent; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        btnCrearSalir.setOnAction(ev -> modalStage.close());

        Button btnImprimir = new Button("Imprimir Comprobante");
        btnImprimir.getStyleClass().add("boton-azul-claro");
        btnImprimir.setOnAction(ev -> {
            if (cotizacionGuardada.getId() == null) {
                mostrarError("No se encontró el ID de la cotización para imprimir.");
                return;
            }
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar comprobante PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            fileChooser.setInitialFileName(cotizacionGuardada.getFolio() != null ? cotizacionGuardada.getFolio() + ".pdf" : "comprobante.pdf");

            File destino = fileChooser.showSaveDialog(modalStage);
            if (destino == null) return;

            try {
                byte[] pdf = generarComprobanteUseCase.generarComprobante(cotizacionGuardada.getId());
                try (FileOutputStream fos = new FileOutputStream(destino)) {
                    fos.write(pdf);
                }

                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(destino);
                }
            } catch (Exception ex) {
                mostrarError("No se pudo generar el comprobante. Intente de nuevo.");
            }
        });

        HBox footer = new HBox(18, btnCrearSalir, btnImprimir);
        footer.setAlignment(Pos.CENTER_RIGHT);

        body.getChildren().addAll(q, req, anticipoRow, footer);

        root.getChildren().addAll(header, body);
        modalStage.setScene(new Scene(root));
        modalStage.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Error");
        alert.setHeaderText("Revisa los datos ingresados");
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // ─── CANCELAR FLUJO POR CATÁLOGO (Flujo 2.2.8) ──────────────────────────
    private void deshabilitarFlujoPorCatalogo() {
        if (comboPaquetes != null) comboPaquetes.setDisable(true);
        if (listaServiciosExtras != null) listaServiciosExtras.setDisable(true);
        if (btnAnadirServicio != null) btnAnadirServicio.setDisable(true);
        if (btnGuardarBorrador != null) btnGuardarBorrador.setDisable(true);
        if (btnConfirmarCotizacion != null) btnConfirmarCotizacion.setDisable(true);
    }

    // ─── PANEL LATERAL COTIZACIONES ────────────────────────────────────────
    private void configurarPanelCotizaciones() {
        if (listaCotizaciones == null) return;

        cotizacionesFiltradas = new FilteredList<>(cotizacionesPanel, c -> true);
        listaCotizaciones.setItems(cotizacionesFiltradas);

        listaCotizaciones.setCellFactory(lv -> new ListCell<CotizacionDTO>() {
            @Override
            protected void updateItem(CotizacionDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label folio = new Label(item.getFolio() != null ? item.getFolio() : "");
                folio.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
                Label cliente = new Label(item.getNombreCliente() != null ? item.getNombreCliente() : "");
                cliente.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
                Label fecha = new Label(item.getFecha() != null ? item.getFecha().toString() : "");
                fecha.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 10px;");

                Label estado = new Label(item.getEstado() != null ? item.getEstado().name() : "");
                estado.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 4 8 4 8; -fx-background-radius: 12;");
                if (item.getEstado() == EstadoCotizacion.BORRADOR) {
                    estado.setStyle(estado.getStyle() + " -fx-background-color: #ecf0f1; -fx-text-fill: #7f8c8d;");
                } else if (item.getEstado() == EstadoCotizacion.VIGENTE) {
                    estado.setStyle(estado.getStyle() + " -fx-background-color: #e8f4fa; -fx-text-fill: #1a82b8;");
                } else if (item.getEstado() == EstadoCotizacion.CANCELADA) {
                    estado.setStyle(estado.getStyle() + " -fx-background-color: #fdecea; -fx-text-fill: #e74c3c;");
                } else {
                    estado.setStyle(estado.getStyle() + " -fx-background-color: #f0f4f8; -fx-text-fill: #bdc3c7;");
                }

                Label total = new Label(String.format("$%,.2f", item.getTotal()));
                total.setStyle("-fx-font-weight: bold; -fx-text-fill: #1a82b8;");

                // NUEVO BOTÓN EDITAR EN CADA CELDA
                Button btnEditar = new Button("Editar");
                btnEditar.setStyle("-fx-font-size: 10px; -fx-background-color: #ecf0f1; -fx-cursor: hand;");
                btnEditar.setOnAction(e -> {
                    modoEdicion = true;
                    cotizacionEnEdicionId = item.getId();
                    fechaEdicionActual = item.getFecha();
                    turnoEdicionActual = item.getTurno();
                    estadoEdicionActual = item.getEstado(); // Guardamos el estado original
                    btnEliminarCotizacion.setDisable(false);
                    
                    cargarCotizacionEnFormulario(item);
                    cambiarModoInterfaz(true); // Mostrar botones de Guardar/Descartar
                });

                VBox left = new VBox(4, folio, cliente, fecha);
                VBox right = new VBox(6, total, estado, btnEditar); // Agregamos el botón aquí
                right.setAlignment(Pos.TOP_RIGHT);

                HBox row = new HBox(10, left, right);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 8, 8, 8));
                
                // Asegurar que HBox ocupe el espacio
                HBox.setHgrow(left, Priority.ALWAYS);
                
                setGraphic(row);
            }
        });
        if (txtBuscarCotizaciones != null) {
            txtBuscarCotizaciones.textProperty().addListener((obs, old, nuevo) -> {
                String filtro = nuevo != null ? nuevo.trim().toUpperCase() : "";
                cotizacionesFiltradas.setPredicate(c -> {
                    if (filtro.isEmpty()) return true;
                    return c.getFolio() != null && c.getFolio().toUpperCase().contains(filtro);
                });
            });
        }

        cargarCotizacionesPanel();
    }

    private void cargarCotizacionesPanel() {
        if (listarCotizacionesUseCase == null || cotizacionesPanel == null) return;
        List<CotizacionDTO> lista = listarCotizacionesUseCase.obtenerCotizaciones();
        cotizacionesPanel.setAll(lista);
    }

    // ─── CARGAR COTIZACIÓN EN FORMULARIO (Flujo 2.2.6) ─────────────────────
    private void cargarCotizacionEnFormulario(CotizacionDTO cotizacion) {
        if (cotizacion == null) return;

        if (cotizacion.getFecha() != null && datePickerFecha != null) {
            datePickerFecha.setValue(cotizacion.getFecha());
            configurarComboTurnos(cotizacion.getFecha());
        }

        if (cotizacion.getClienteId() != null) {
            ClienteDTO clienteEncontrado = comboClientes.getItems().stream()
                .filter(c -> c.getId() != null && c.getId().equals(cotizacion.getClienteId()))
                .findFirst()
                .orElse(null);
            comboClientes.setValue(clienteEncontrado);
        }

        if (cotizacion.getPaqueteId() != null) {
            PaqueteDTO paqueteEncontrado = comboPaquetes.getItems().stream()
                .filter(p -> p.getId() != null && p.getId().equals(cotizacion.getPaqueteId()))
                .findFirst()
                .orElse(null);
            comboPaquetes.setValue(paqueteEncontrado);
            actualizarLabelsPaquete(paqueteEncontrado);
        }

        if (cotizacion.getTurno() != null) {
            comboTurnos.setValue(cotizacion.getTurno());
        }

        textAreaNotas.setText(cotizacion.getNotas() != null ? cotizacion.getNotas() : "");
        txtNombreFestejado.setText(cotizacion.getNombreFestejado() != null ? cotizacion.getNombreFestejado() : "");
        txtTematica.setText(cotizacion.getTematica() != null ? cotizacion.getTematica() : "");

        // Seleccionamos los extras previamente guardados
        if (listaServiciosExtras != null) {
            listaServiciosExtras.getSelectionModel().clearSelection();
            if (cotizacion.getDetalles() != null) {
                for (DetalleCotizacionDTO detalle : cotizacion.getDetalles()) {
                    if (detalle == null || detalle.getServicioId() == null) continue;
                    ServicioDTO match = listaServiciosExtras.getItems().stream()
                        .filter(s -> s.getId() != null && s.getId().equals(detalle.getServicioId()))
                        .findFirst()
                        .orElse(null);
                    if (match != null) {
                        listaServiciosExtras.getSelectionModel().select(match);
                    }
                }
            }
        }

        actualizarTotalEstimado();
        actualizarPagosUI();
    }

    private void actualizarLabelsPaquete(PaqueteDTO paquete) {
        if (paquete == null) return;
        lblNombrePaquete.setText(paquete.getNombre());
        lblPrecioPaquete.setText("$" + String.format("%,.2f", paquete.getCostoBase()));
        String detalles = paquete.getDescripcion() != null ? paquete.getDescripcion() : "Sin detalles adicionales";
        lblDetallesPaquete.setText(detalles);
    }

    // ─── NUEVA COTIZACIÓN / SALIR DE EDICIÓN ──────────────────────────────
    @FXML
    private void nuevaCotizacion() {
        modoEdicion = false;
        cotizacionEnEdicionId = null;
        fechaEdicionActual = null;
        turnoEdicionActual = null;
        cambiarModoInterfaz(false);
        if (panelPagos != null) {
            panelPagos.setVisible(false);
            panelPagos.setManaged(false);
        }
        if (listaCotizaciones != null) {
            listaCotizaciones.getSelectionModel().clearSelection();
        }
        btnEliminarCotizacion.setDisable(true);

        comboClientes.setValue(null);
        comboPaquetes.setValue(null);
        if (datePickerFecha != null) {
            LocalDate fechaSeleccionada = cotizacionContext.getFechaSeleccionada();
            if (fechaSeleccionada != null) {
                datePickerFecha.setValue(fechaSeleccionada);
                configurarComboTurnos(fechaSeleccionada);
            }
        }
        if (comboTurnos != null) {
            comboTurnos.setValue(null);
        }

        textAreaNotas.setText("");
        txtNombreFestejado.setText("");
        txtTematica.setText("");

        if (listaServiciosExtras != null) {
            listaServiciosExtras.getSelectionModel().clearSelection();
        }

        lblNombrePaquete.setText("Seleccione un paquete");
        lblPrecioPaquete.setText(" $0.00");
        lblDetallesPaquete.setText("Los detalles aparecerán aquí");
        actualizarTotalEstimado();
    }

    // ─── ELIMINAR COTIZACIÓN (Flujo 2.2.7) ────────────────────────────────
    @FXML
    private void eliminarCotizacion() {
        if (!modoEdicion || cotizacionEnEdicionId == null) {
            mostrarError("Seleccione una cotización para eliminar.");
            return;
        }

        CotizacionDTO seleccionada = listaCotizaciones != null
            ? listaCotizaciones.getSelectionModel().getSelectedItem()
            : null;
        String folio = seleccionada != null && seleccionada.getFolio() != null ? seleccionada.getFolio() : "";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar cotización");
        confirm.setHeaderText("Confirmar eliminación");
        confirm.setContentText("¿Está seguro de eliminar/desactivar la cotización " + folio + "?");

        ButtonType eliminarBtn = new ButtonType("Eliminar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelarBtn = ButtonType.CANCEL;
        confirm.getButtonTypes().setAll(eliminarBtn, cancelarBtn);

        var result = confirm.showAndWait();
        if (result.isPresent() && result.get() == eliminarBtn) {
            try {
                eliminarCotizacionUseCase.eliminarCotizacion(cotizacionEnEdicionId);
                Alert ok = new Alert(Alert.AlertType.INFORMATION, "Cotización eliminada correctamente.");
                ok.showAndWait();

                cargarCotizacionesPanel();
                nuevaCotizacion();
            } catch (CotizacionException ex) {
                mostrarError(ex.getMessage());
            } catch (Exception ex) {
                mostrarError("No se pudo eliminar la cotización. Intente de nuevo.");
            }
        }
    }

    // ─── Construir DTO desde el formulario ───────────────────────────────
    private CotizacionDTO construirDtoCotizacionDesdeFormulario(EstadoCotizacion estadoObjetivo) {
        ClienteDTO clienteSeleccionado = comboClientes.getValue();
        PaqueteDTO paqueteSeleccionado = comboPaquetes.getValue();
        LocalDate fecha = datePickerFecha.getValue();
        TurnoEvento turno = comboTurnos.getValue();

        if (clienteSeleccionado == null) {
            throw new CotizacionException("Debe seleccionar un cliente.");
        }
        if (paqueteSeleccionado == null) {
            throw new CotizacionException("Debe seleccionar un paquete base.");
        }
        if (fecha == null) {
            throw new CotizacionException("Debe seleccionar una fecha para el evento.");
        }
        if (turno == null) {
            throw new CotizacionException("Debe seleccionar un turno (Matutino o Vespertino).");
        }

        String notas = textAreaNotas.getText();
        String nombreFestejado = txtNombreFestejado.getText();
        String tematica = txtTematica.getText();

        CotizacionDTO dto = new CotizacionDTO();
        dto.setClienteId(clienteSeleccionado.getId());
        dto.setPaqueteId(paqueteSeleccionado.getId());
        dto.setFecha(fecha);
        dto.setTurno(turno);
        dto.setNotas(notas != null && !notas.isBlank() ? notas.trim() : null);
        dto.setNombreFestejado(nombreFestejado != null && !nombreFestejado.isBlank() ? nombreFestejado.trim() : null);
        dto.setTematica(tematica != null && !tematica.isBlank() ? tematica.trim() : null);
        dto.setEstado(estadoObjetivo);

        // Servicios extras seleccionados
        List<ServicioDTO> extrasSel = listaServiciosExtras != null
            ? new ArrayList<>(listaServiciosExtras.getSelectionModel().getSelectedItems())
            : List.of();

        List<DetalleCotizacionDTO> detalles = new ArrayList<>();
        for (ServicioDTO s : extrasSel) {
            if (s == null) continue;
            DetalleCotizacionDTO d = new DetalleCotizacionDTO();
            d.setServicioId(s.getId());
            d.setNombreServicio(s.getNombre());
            d.setPrecioUnitario(s.getPrecio());
            d.setCantidad(1);
            d.setSubtotal(s.getPrecio());
            detalles.add(d);
        }
        dto.setDetalles(detalles);

        return dto;
    }

    // ─── CONFIGURACIÓN COMBO TURNOS ───────────────────────────────────────────
    private void configurarComboTurnos(LocalDate fecha) {
        ObservableList<TurnoEvento> turnosDisponibles = FXCollections.observableArrayList();

        if (fecha != null && !fecha.isBefore(LocalDate.now())) {
            for (TurnoEvento turno : TurnoEvento.values()) {
                boolean incluirTurnoEnEdicion = modoEdicion
                    && fechaEdicionActual != null
                    && fechaEdicionActual.equals(fecha)
                    && turnoEdicionActual != null
                    && turnoEdicionActual.equals(turno);

                if (incluirTurnoEnEdicion || verificarDisponibilidadTurnoUseCase.esTurnoDisponible(fecha, turno)) {
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
            NuevoClienteModalController modalController = loader.getController();

            Stage modalStage = new Stage();
            modalStage.setTitle("Registrar Nuevo Cliente");
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.initStyle(StageStyle.UTILITY);
            modalStage.setScene(new Scene(root));
            modalStage.setResizable(false);
            modalStage.showAndWait();

            configurarComboClientes();

            // Asociamos automáticamente el cliente recién creado a la cotización (Flujo 2.2.5)
            ClienteDTO creado = modalController != null ? modalController.getClienteCreado() : null;
            if (creado != null && comboClientes != null) {
                ClienteDTO encontrado = comboClientes.getItems().stream()
                    .filter(c -> c.getId() != null && c.getId().equals(creado.getId()))
                    .findFirst()
                    .orElse(creado);
                comboClientes.setValue(encontrado);
            }

            // Continuamos el flujo hacia el paso 5 (agenda/turno)
            if (datePickerFecha != null) {
                datePickerFecha.requestFocus();
            }

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
    
    @FXML
    private void registrarAbonoCotizacion() {
        if (!modoEdicion || cotizacionEnEdicionId == null) {
            mostrarError("Primero debe guardar la cotización como borrador.");
            return;
        }
        try {
            double monto = Double.parseDouble(txtAbono.getText().trim());
            registrarAnticipoUseCase.registrarAnticipo(cotizacionEnEdicionId, monto, MetodoPago.EFECTIVO);
            txtAbono.setText("");
            
            cargarCotizacionesPanel();
            actualizarPagosUI();
            imprimirComprobante(cotizacionEnEdicionId);
            
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Abono registrado correctamente.");
            ok.showAndWait();
        } catch(NumberFormatException e) {
            mostrarError("Monto inválido.");
        } catch(Exception e) {
            mostrarError(e.getMessage());
        }
    }
    private void actualizarPagosUI() {
        if (!modoEdicion || cotizacionEnEdicionId == null || panelPagos == null) {
            if (panelPagos != null) { panelPagos.setVisible(false); panelPagos.setManaged(false); }
            return;
        }
        panelPagos.setVisible(true);
        panelPagos.setManaged(true);
        
        double total = Double.parseDouble(lblTotalEstimado.getText().replace("$", "").replace(",", ""));
        double pagado = registrarAnticipoUseCase.obtenerTotalAbonado(cotizacionEnEdicionId);
        double restante = total - pagado;
        if (restante < 0) restante = 0;
        
        lblPagado.setText(String.format("Pagado: $%,.2f", pagado));
        lblRestante.setText(String.format("Restante: $%,.2f", restante));
    }

    private void imprimirComprobante(Long id) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar comprobante PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fileChooser.setInitialFileName("comprobante_abono.pdf");
        
        File destino = fileChooser.showSaveDialog(btnConfirmarCotizacion.getScene().getWindow());
        if (destino == null) return;
        
        try {
            byte[] pdf = generarComprobanteUseCase.generarComprobante(id);
            try (FileOutputStream fos = new FileOutputStream(destino)) {
                fos.write(pdf);
            }
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(destino);
        } catch (Exception ex) {
            mostrarError("No se pudo generar el comprobante PDF.");
        }
    }
    
    @FXML
    private void abrirDialogoAgregarPaquete() {
        Dialog<PaqueteDTO> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Paquete");
        dialog.setHeaderText("Añadir un nuevo paquete al catálogo");
        
        ButtonType guardarButtonType = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField txtNombre = new TextField(); txtNombre.setPromptText("Ej. Paquete Super");
        TextField txtPrecio = new TextField(); txtPrecio.setPromptText("Ej. 4000.00");
        TextField txtDescripcion = new TextField(); txtDescripcion.setPromptText("Ej. Incluye de todo");
        
        grid.add(new Label("Nombre:"), 0, 0); grid.add(txtNombre, 1, 0);
        grid.add(new Label("Precio Base (MXN):"), 0, 1); grid.add(txtPrecio, 1, 1);
        grid.add(new Label("Descripción:"), 0, 2); grid.add(txtDescripcion, 1, 2);
        
        javafx.scene.Node guardarBtn = dialog.getDialogPane().lookupButton(guardarButtonType);
        guardarBtn.setDisable(true);
        
        txtNombre.textProperty().addListener((o, old, newVal) -> guardarBtn.setDisable(newVal.trim().isEmpty() || txtPrecio.getText().trim().isEmpty()));
        txtPrecio.textProperty().addListener((o, old, newVal) -> guardarBtn.setDisable(newVal.trim().isEmpty() || txtNombre.getText().trim().isEmpty()));
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(btn -> {
            if (btn == guardarButtonType) {
                try {
                    return consultarCatalogoUseCase.guardarNuevoPaquete(txtNombre.getText().trim(), Double.parseDouble(txtPrecio.getText().trim()), txtDescripcion.getText().trim());
                } catch(Exception e) {
                    mostrarError("Error al guardar paquete.");
                }
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(nuevo -> {
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Paquete agregado.");
            ok.showAndWait();
            configurarComboPaquetes();
        });
    }
    
}