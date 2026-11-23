package com.mycompany.presentacion.controllers;

import com.example.negocio.agenda.usecase.VerificarDisponibilidadTurnoUseCase;
import com.example.negocio.catalogo.usecase.ConsultarCatalogoUseCase;
import com.example.negocio.cliente.usecase.BuscarClienteUseCase;
import com.example.negocio.cotizacion.usecase.EliminarCotizacionUseCase;
import com.example.negocio.cotizacion.usecase.CalcularTotalCotizacionUseCase;
import com.example.negocio.cotizacion.usecase.CrearCotizacionUseCase;
import com.example.negocio.cotizacion.usecase.GenerarComprobanteUseCase;
import com.example.negocio.cotizacion.usecase.GenerarContratoUseCase;
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
import javafx.concurrent.Task;
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
import javafx.scene.layout.Priority;
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

@Controller
@RequiredArgsConstructor
public class CotizacionController {

    // ─── USE CASES ───────────────────────────────────────────────────────────
    private final BuscarClienteUseCase buscarClienteUseCase;
    private final ConsultarCatalogoUseCase consultarCatalogoUseCase;
    private final VerificarDisponibilidadTurnoUseCase verificarDisponibilidadTurnoUseCase;
    private final CalcularTotalCotizacionUseCase calcularTotalCotizacionUseCase;
    private final CrearCotizacionUseCase crearCotizacionUseCase;
    private final ListarCotizacionesUseCase listarCotizacionesUseCase;  // Fix #3: fuente de verdad para totales
    private final ModificarCotizacionUseCase modificarCotizacionUseCase;
    private final EliminarCotizacionUseCase eliminarCotizacionUseCase;
    private final GenerarComprobanteUseCase generarComprobanteUseCase;
    private final GenerarContratoUseCase generarContratoUseCase;
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
    
    @FXML private ListView<ServicioAgrupado> listaServiciosExtras;
    @FXML private Label lblSubtotalExtras;

    // ─── PANEL LATERAL COTIZACIONES ─────────────────────────────────────
    @FXML
    private TextField txtBuscarCotizaciones;
    @FXML
    private ListView<CotizacionDTO> listaCotizaciones;
    @FXML
    private Button btnNuevaCotizacion;
    @FXML
    private Button btnEliminarCotizacion;

    @FXML
    private Button btnAnadirServicio;
    @FXML
    private Button btnGuardarBorrador;
    @FXML
    private Button btnConfirmarCotizacion;

    private final ObservableList<CotizacionDTO> cotizacionesPanel = FXCollections.observableArrayList();
    private FilteredList<CotizacionDTO> cotizacionesFiltradas;
    @FXML private ComboBox<MetodoPago> comboMetodoPago;
    @FXML private HBox boxBotonesNuevos;
    @FXML private HBox boxBotonesEdicion;
    @FXML private Button btnGuardarCambios;
    @FXML private Button btnDescartarCambios;

    private final ObservableList<ServicioDTO> serviciosSeleccionados = FXCollections.observableArrayList();
    private final ObservableList<ServicioAgrupado> serviciosAgrupados = FXCollections.observableArrayList();

    /**
     * Datos de cada extra seleccionado que no caben en ServicioDTO: hora,
     * especificaciones, desglose y ubicación. La clave es la instancia concreta
     * que está en serviciosSeleccionados (por identidad, no por id), porque el
     * mismo servicio puede agregarse varias veces con datos distintos.
     */
    private final java.util.Map<ServicioDTO, DetalleCotizacionDTO> datosDeExtras = new java.util.IdentityHashMap<>();
    private javafx.collections.ListChangeListener<ServicioDTO> listenerExtras;

    /**
     * Disponibilidad ya consultada, por mes. El calendario pinta unas 42 celdas por
     * mes; consultar cada día por separado eran 84 consultas a la base cada vez que
     * se abría o se cambiaba de mes. Aquí se hace una consulta por mes mostrado.
     */
    private final java.util.Map<java.time.YearMonth, java.util.Map<LocalDate, String>> disponibilidadPorMes =
        new java.util.HashMap<>();

    /** "LIBRE", "AMARILLO" (un turno ocupado) o "GRIS" (día lleno). */
    private String estadoDelDia(LocalDate fecha) {
        java.time.YearMonth mes = java.time.YearMonth.from(fecha);
        return disponibilidadPorMes
            .computeIfAbsent(mes, m -> verificarDisponibilidadTurnoUseCase
                .obtenerEstadosDisponibilidad(m.atDay(1), m.atEndOfMonth()))
            .getOrDefault(fecha, "LIBRE");
    }

    public static class ServicioAgrupado {
        public ServicioDTO servicio;
        public int cantidad;
        public ServicioAgrupado(ServicioDTO s, int c) { this.servicio = s; this.cantidad = c; }
    }

    private boolean isModoEdicion()              { return cotizacionContext.isModoEdicion(); }
    private Long getCotizacionEnEdicionId()       { return cotizacionContext.getCotizacionEnEdicionId(); }
    private EstadoCotizacion getEstadoEdicion()   { return cotizacionContext.getEstadoEdicionActual(); }
    private LocalDate getFechaEdicion()           { return cotizacionContext.getFechaEdicionActual(); }
    private TurnoEvento getTurnoEdicion()         { return cotizacionContext.getTurnoEdicionActual(); }

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
        EstadoCotizacion estado = getEstadoEdicion();
        if (estado == null) estado = EstadoCotizacion.BORRADOR;

        CotizacionDTO r = guardarOActualizarCotizacion(estado);
        if (r != null) {
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Cambios guardados exitosamente.");
            ok.showAndWait();
            nuevaCotizacion();
        }
    }
    @FXML
    public void initialize() {
        if (comboMetodoPago != null) {
            comboMetodoPago.getItems().setAll(MetodoPago.values());
            comboMetodoPago.setValue(MetodoPago.EFECTIVO);
        }

        if (txtAbono != null) {
            txtAbono.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
                if (change.getControlNewText().matches("\\d*(\\.\\d*)?")) return change;
                return null;
            }));
        }
        
        configurarComboClientes();
        configurarComboPaquetes();
        configurarListaServiciosExtras();

        if (listenerExtras != null) {
            serviciosSeleccionados.removeListener(listenerExtras);
        }
        listenerExtras = (javafx.collections.ListChangeListener.Change<? extends ServicioDTO> c) -> {
            java.util.Map<Long, java.util.List<ServicioDTO>> map = serviciosSeleccionados.stream()
                .collect(java.util.stream.Collectors.groupingBy(ServicioDTO::getId));
            // Se olvidan los datos de los extras que ya no están seleccionados.
            datosDeExtras.keySet().removeIf(k -> serviciosSeleccionados.stream().noneMatch(s -> s == k));
            serviciosAgrupados.clear();
            for (java.util.List<ServicioDTO> group : map.values()) {
                serviciosAgrupados.add(new ServicioAgrupado(group.get(0), group.size()));
            }
            
            double subtotalExtras = serviciosSeleccionados.stream().mapToDouble(ServicioDTO::getPrecio).sum();
            if (lblSubtotalExtras != null) {
                lblSubtotalExtras.setText(String.format("Subtotal Extras: $%,.2f", subtotalExtras));
            }
        };
        serviciosSeleccionados.addListener(listenerExtras);

        serviciosSeleccionados.clear();
        
        cambiarModoInterfaz(false);

        boolean catalogoDisponible = true;
        try {
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

            // Cada vez que se abre el calendario se vuelve a consultar la disponibilidad.
            disponibilidadPorMes.clear();
            datePickerFecha.setOnShowing(e -> disponibilidadPorMes.clear());

            datePickerFecha.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    if (date != null) {
                        // Si se está editando, permitimos seleccionar la fecha actual aunque no haya turnos
                        if (isModoEdicion() && getFechaEdicion() != null && date.equals(getFechaEdicion())) {
                            setDisable(false);
                            return;
                        }

                        if (date.isBefore(LocalDate.now())) {
                            setDisable(true);
                            setStyle("-fx-background-color: #f2f2f2; -fx-text-fill: #b2b2b2;");
                        } else {
                            // Lógica de colores según Flujo 2.2.1 y 2.2.2
                            String estado = estadoDelDia(date);
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
                validarFormularioCompleto();
            });
        }

        comboClientes.valueProperty().addListener((obs, old, val) -> validarFormularioCompleto());
        comboPaquetes.valueProperty().addListener((obs, old, val) -> validarFormularioCompleto());
        comboTurnos.valueProperty().addListener((obs, old, val) -> validarFormularioCompleto());
        if (txtNombreFestejado != null) txtNombreFestejado.textProperty().addListener((obs, old, val) -> validarFormularioCompleto());
        if (txtTematica != null) txtTematica.textProperty().addListener((obs, old, val) -> validarFormularioCompleto());

        actualizarTotalEstimado();
        validarFormularioCompleto();
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
        // Fix #7: leer estado desde contexto
        CotizacionDTO r;
        if (!isModoEdicion()) {
            r = guardarOActualizarCotizacion(EstadoCotizacion.BORRADOR);
        } else {
            EstadoCotizacion estado = getEstadoEdicion();
            r = guardarOActualizarCotizacion(estado != null ? estado : EstadoCotizacion.BORRADOR);
        }

        if (r == null) return;

        double pagado = registrarAnticipoUseCase.obtenerTotalAbonado(getCotizacionEnEdicionId());
        // Anticipo que confirma la cotización: $3,000, o el total si vale menos.
        double minimo = registrarAnticipoUseCase.obtenerAnticipoMinimo(getCotizacionEnEdicionId());
        if (pagado >= minimo) {
            guardarOActualizarCotizacion(EstadoCotizacion.VIGENTE);
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Cotización confirmada y evento creado (Fechas bloqueadas).");
            ok.showAndWait();
            nuevaCotizacion();
        } else {
            TextInputDialog dialog = new TextInputDialog(String.format(java.util.Locale.ROOT, "%.2f", minimo - pagado));
            dialog.setTitle("Confirmar Cotización");
            dialog.setHeaderText(String.format("Anticipo mínimo de $%,.2f para confirmar y bloquear la fecha.", minimo));
            dialog.setContentText("Ingrese el monto a pagar:");

            dialog.showAndWait().ifPresent(montoStr -> {
                try {
                    double monto = Double.parseDouble(montoStr.replace("$", "").replace(",", "").trim());
                    if (pagado + monto < minimo) {
                        mostrarError(String.format("El pago total no alcanza el anticipo mínimo de $%,.2f.", minimo));
                        return;
                    }
                    MetodoPago metodo = pedirMetodoPago();
                    if (metodo == null) {
                        return; // se canceló la selección del método de pago
                    }
                    registrarAnticipoUseCase.registrarAnticipo(getCotizacionEnEdicionId(), monto, metodo);
                    guardarOActualizarCotizacion(EstadoCotizacion.VIGENTE);
                    actualizarPagosUI();
                    imprimirComprobante(getCotizacionEnEdicionId());

                    Alert ok = new Alert(Alert.AlertType.INFORMATION, "Cotización confirmada y evento creado exitosamente.");
                    ok.showAndWait();
                    nuevaCotizacion();
                } catch(NumberFormatException e) {
                    mostrarError("Monto inválido.");
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
            if (isModoEdicion()) {  // Fix #7: leer desde contexto
                resultado = modificarCotizacionUseCase.modificarCotizacion(getCotizacionEnEdicionId(), dto);
            } else {
                resultado = crearCotizacionUseCase.crearCotizacion(dto);
                // Fix #7: escribir al contexto en lugar de campos locales
                cotizacionContext.setModoEdicion(true);
                cotizacionContext.setCotizacionEnEdicionId(resultado.getId());
                cotizacionContext.setFechaEdicionActual(resultado.getFecha());
                cotizacionContext.setTurnoEdicionActual(resultado.getTurno());
                cotizacionContext.setEstadoEdicionActual(resultado.getEstado());
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

        Button btnContrato = new Button("Generar Contrato");
        btnContrato.getStyleClass().add("boton-azul-claro");
        btnContrato.setOnAction(ev -> {
            if (cotizacionGuardada.getId() == null) {
                mostrarError("No se encontró el ID de la cotización para imprimir.");
                return;
            }
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar contrato PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            fileChooser.setInitialFileName(cotizacionGuardada.getFolio() != null ? "Contrato_" + cotizacionGuardada.getFolio() + ".pdf" : "contrato.pdf");

            File destino = fileChooser.showSaveDialog(modalStage);
            if (destino == null) return;

            try {
                byte[] pdf = generarContratoUseCase.generarContrato(cotizacionGuardada.getId());
                try (FileOutputStream fos = new FileOutputStream(destino)) {
                    fos.write(pdf);
                }

                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(destino);
                }
            } catch (Exception ex) {
                mostrarError("No se pudo generar el contrato. Intente de nuevo.");
            }
        });

        HBox footer = new HBox(18, btnCrearSalir, btnImprimir, btnContrato);
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

                // Lógica de fechas
                boolean esPasada = item.getFecha() != null && item.getFecha().isBefore(LocalDate.now());
                boolean esBorrador = (item.getEstado() == EstadoCotizacion.BORRADOR);
                boolean esVigenteFutura = (item.getEstado() == EstadoCotizacion.VIGENTE && !esPasada);
                boolean esEditable = esBorrador || esVigenteFutura;

                // Estilo para pasadas
                if (esPasada) {
                    folio.setStyle("-fx-font-weight: bold; -fx-text-fill: #95a5a6;");
                    cliente.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 11px;");
                    fecha.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 10px;");
                    Label lblPasada = new Label("[Pasada]");
                    lblPasada.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10px; -fx-font-weight: bold;");
                    folio.setGraphic(lblPasada);
                    folio.setContentDisplay(ContentDisplay.RIGHT);
                }

                // NUEVO BOTÓN EDITAR EN CADA CELDA
                Button btnEditar = new Button(esEditable ? "Editar" : "Ver");
                btnEditar.setStyle("-fx-font-size: 10px; -fx-background-color: #ecf0f1; -fx-cursor: hand;");
                btnEditar.setOnAction(e -> {
                    cotizacionContext.setModoEdicion(true);
                    cotizacionContext.setCotizacionEnEdicionId(item.getId());
                    cotizacionContext.setFechaEdicionActual(item.getFecha());
                    cotizacionContext.setTurnoEdicionActual(item.getTurno());
                    cotizacionContext.setEstadoEdicionActual(item.getEstado()); // Guardamos el estado original
                    btnEliminarCotizacion.setDisable(item.getEstado() != EstadoCotizacion.BORRADOR);
                    
                    cargarCotizacionEnFormulario(item);
                    cambiarModoInterfaz(true); // Mostrar botones de Guardar/Descartar

                    // Si no es editable, dejar en modo de solo lectura (ocultando guardado y bloqueando campos)
                    boolean esBorradorLocal = (item.getEstado() == EstadoCotizacion.BORRADOR);
                    boolean esVigenteFuturaLocal = (item.getEstado() == EstadoCotizacion.VIGENTE && item.getFecha() != null && !item.getFecha().isBefore(LocalDate.now()));
                    boolean esEditableLocal = esBorradorLocal || esVigenteFuturaLocal;

                    if (btnGuardarCambios != null) btnGuardarCambios.setDisable(!esEditableLocal);
                    if (btnGuardarBorrador != null) btnGuardarBorrador.setDisable(!esBorradorLocal);
                    if (btnConfirmarCotizacion != null) btnConfirmarCotizacion.setDisable(!esBorradorLocal);
                    
                    setSoloLectura(!esEditableLocal);
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

    /**
     * Fix #1 — Carga asincrónica del panel de cotizaciones.
     * Ejecuta la query en un hilo de fondo (Task) para no bloquear el
     * Application Thread de JavaFX. La ObservableList se actualiza
     * en setOnSucceeded, que ya corre en el FX Thread, por lo que
     * NO es necesario envolver con Platform.runLater().
     */
    private void cargarCotizacionesPanel() {
        if (listarCotizacionesUseCase == null || cotizacionesPanel == null) return;

        Task<List<CotizacionDTO>> task = new Task<>() {
            @Override
            protected List<CotizacionDTO> call() {
                return listarCotizacionesUseCase.obtenerCotizaciones();
            }
        };
        task.setOnSucceeded(e -> {
            java.util.List<CotizacionDTO> lista = new java.util.ArrayList<>(task.getValue());
            LocalDate hoy = LocalDate.now();
            lista.sort((c1, c2) -> {
                LocalDate f1 = c1.getFecha() != null ? c1.getFecha() : LocalDate.MIN;
                LocalDate f2 = c2.getFecha() != null ? c2.getFecha() : LocalDate.MIN;
                boolean f1Futura = !f1.isBefore(hoy);
                boolean f2Futura = !f2.isBefore(hoy);
                
                if (f1Futura && !f2Futura) return -1;
                if (!f1Futura && f2Futura) return 1;
                if (f1Futura && f2Futura) {
                    return f1.compareTo(f2); // Mas cercana a hoy primero (ascendente)
                } else {
                    return f2.compareTo(f1); // Mas reciente primero (descendente)
                }
            });
            cotizacionesPanel.setAll(lista);
        });
        task.setOnFailed(e -> {
            System.err.println("[CotizacionController] Error cargando panel: " + task.getException().getMessage());
            com.mycompany.presentacion.config.RegistroErrores.avisar("No se pudo cargar la lista de cotizaciones", task.getException());
        });
        new Thread(task, "hilo-carga-cotizaciones").start();
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

        // Seleccionamos los extras previamente guardados y los metemos a la lista visual
        if (listaServiciosExtras != null) {
            serviciosSeleccionados.clear();
            if (cotizacion.getDetalles() != null) {
                // Los extras se reconstruyen desde lo GUARDADO en la cotización (nombre,
                // precio pactado y datos de logística), no desde el catálogo actual: así
                // editar no cambia el precio si el servicio subió después, ni pierde los
                // extras cuyo servicio fue desactivado.
                List<ServicioDTO> guardados = new ArrayList<>();
                for (DetalleCotizacionDTO detalle : cotizacion.getDetalles()) {
                    if (detalle == null || detalle.getServicioId() == null) continue;
                    int cantidad = Math.max(1, detalle.getCantidad());
                    for (int i = 0; i < cantidad; i++) {
                        ServicioDTO extra = new ServicioDTO();
                        extra.setId(detalle.getServicioId());
                        extra.setNombre(detalle.getNombreServicio());
                        extra.setPrecio(detalle.getPrecioUnitario());
                        datosDeExtras.put(extra, detalle);
                        guardados.add(extra);
                    }
                }
                serviciosSeleccionados.addAll(guardados);
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
        // Fix #7: limpiar TODO el estado de sesión con una sola llamada al contexto.
        // Esto garantiza que ninguna fecha/turno/id de una cotización anterior persista.
        cotizacionContext.limpiar();

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
            datePickerFecha.setValue(null);
            comboTurnos.setDisable(true);
        }
        if (comboTurnos != null) {
            comboTurnos.setValue(null);
        }

        textAreaNotas.setText("");
        txtNombreFestejado.setText("");
        txtTematica.setText("");

        if (listaServiciosExtras != null) {
            serviciosSeleccionados.clear();
        }

        lblNombrePaquete.setText("Seleccione un paquete");
        lblPrecioPaquete.setText(" $0.00");
        lblDetallesPaquete.setText("Los detalles aparecerán aquí");
        actualizarTotalEstimado();

        // Restaurar botones por si se había abierto una en solo lectura
        if (btnGuardarCambios != null) btnGuardarCambios.setDisable(false);
        
        setSoloLectura(false);
        validarFormularioCompleto();
    }

    private void setSoloLectura(boolean soloLectura) {
        if (comboClientes != null) comboClientes.setDisable(soloLectura);
        if (comboPaquetes != null) comboPaquetes.setDisable(soloLectura);
        if (datePickerFecha != null) datePickerFecha.setDisable(soloLectura);
        if (comboTurnos != null) {
            comboTurnos.setDisable(soloLectura);
            // Si sale de solo lectura pero no hay fecha, debe seguir deshabilitado
            if (!soloLectura && datePickerFecha != null && datePickerFecha.getValue() == null) {
                comboTurnos.setDisable(true);
            }
        }
        if (textAreaNotas != null) textAreaNotas.setDisable(soloLectura);
        if (txtNombreFestejado != null) txtNombreFestejado.setDisable(soloLectura);
        if (txtTematica != null) txtTematica.setDisable(soloLectura);
        if (btnAnadirServicio != null) btnAnadirServicio.setDisable(soloLectura);
        if (listaServiciosExtras != null) listaServiciosExtras.setDisable(soloLectura);
    }

    private void validarFormularioCompleto() {
        boolean incompleto = comboClientes == null || comboClientes.getValue() == null ||
                             comboPaquetes == null || comboPaquetes.getValue() == null ||
                             datePickerFecha == null || datePickerFecha.getValue() == null ||
                             comboTurnos == null || comboTurnos.getValue() == null ||
                             txtNombreFestejado == null || txtNombreFestejado.getText() == null || txtNombreFestejado.getText().trim().isEmpty() ||
                             txtTematica == null || txtTematica.getText() == null || txtTematica.getText().trim().isEmpty();
                             
        if (!isModoEdicion()) {
            if (btnConfirmarCotizacion != null) btnConfirmarCotizacion.setDisable(incompleto);
            if (btnGuardarBorrador != null) btnGuardarBorrador.setDisable(incompleto);
        }
    }

    // ─── ELIMINAR COTIZACIÓN (Flujo 2.2.7) ────────────────────────────────
    @FXML
    private void eliminarCotizacion() {
        if (!isModoEdicion() || getCotizacionEnEdicionId() == null) {  // Fix #7
            mostrarError("Seleccione una cotización para eliminar.");
            return;
        }

        // El folio es el de la cotización EN EDICIÓN (la que se va a eliminar), no el de
        // la fila seleccionada en la lista, que puede ser otra.
        String folio = "";
        try {
            CotizacionDTO enEdicion = listarCotizacionesUseCase.obtenerPorId(getCotizacionEnEdicionId());
            if (enEdicion != null && enEdicion.getFolio() != null) {
                folio = enEdicion.getFolio();
            }
        } catch (RuntimeException ex) {
            // Sin folio la confirmación sigue siendo válida.
        }

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
                eliminarCotizacionUseCase.eliminarCotizacion(getCotizacionEnEdicionId());  // Fix #7
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
        // Usuario con la sesión abierta: queda registrado como quien elaboró la cotización.
        var sesion = ViewSwitcher.getSpringContext().getBean(com.mycompany.presentacion.context.SesionContext.class);
        if (sesion.getUsuarioAutenticado() != null) {
            dto.setUsuarioId(sesion.getUsuarioAutenticado().getId());
        }
        dto.setClienteId(clienteSeleccionado.getId());
        dto.setPaqueteId(paqueteSeleccionado.getId());
        dto.setFecha(fecha);
        dto.setTurno(turno);
        dto.setNotas(notas != null && !notas.isBlank() ? notas.trim() : null);
        dto.setNombreFestejado(nombreFestejado != null && !nombreFestejado.isBlank() ? nombreFestejado.trim() : null);
        dto.setTematica(tematica != null && !tematica.isBlank() ? tematica.trim() : null);
        dto.setEstado(estadoObjetivo);

        // Servicios extras seleccionados
        List<ServicioDTO> extrasSel = new ArrayList<>(serviciosSeleccionados);

        List<DetalleCotizacionDTO> detalles = new ArrayList<>();
        for (ServicioDTO s : extrasSel) {
            if (s == null) continue;
            DetalleCotizacionDTO d = new DetalleCotizacionDTO();
            d.setServicioId(s.getId());
            d.setNombreServicio(s.getNombre());
            d.setPrecioUnitario(s.getPrecio());
            d.setCantidad(1);
            d.setSubtotal(s.getPrecio());
            DetalleCotizacionDTO datos = datosDeExtras.get(s);
            if (datos != null) {
                d.setHoraSugerida(datos.getHoraSugerida());
                d.setEspecificacionesCliente(datos.getEspecificacionesCliente());
                d.setDesgloseOpciones(datos.getDesgloseOpciones());
                d.setUbicacionMontaje(datos.getUbicacionMontaje());
            }
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
                boolean incluirTurnoEnEdicion = isModoEdicion()
                    && getFechaEdicion() != null
                    && getFechaEdicion().equals(fecha)
                    && getTurnoEdicion() != null
                    && getTurnoEdicion().equals(turno);

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

    // ─── CONFIGURACIÓN LISTA SERVICIOS EXTRAS ──────────
    private void configurarListaServiciosExtras() {
        if (listaServiciosExtras != null) {
            listaServiciosExtras.setItems(serviciosAgrupados);
            
            // Formato visual para agregar el ícono de basura y el texto
            listaServiciosExtras.setCellFactory(lv -> new ListCell<ServicioAgrupado>() {
                @Override
                protected void updateItem(ServicioAgrupado item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        HBox hbox = new HBox(10);
                        hbox.setAlignment(Pos.CENTER_LEFT);
                        
                        // Botón de basurita
                        Button btnDelete = new Button();
                        javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
                        trashIcon.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
                        trashIcon.setFill(javafx.scene.paint.Color.web("#e74c3c"));
                        btnDelete.setGraphic(trashIcon);
                        btnDelete.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                        btnDelete.setOnAction(e -> {
                            serviciosSeleccionados.removeIf(s -> s.getId().equals(item.servicio.getId()));
                            actualizarTotalEstimado();
                        });

                        // Texto del servicio
                        double subtotalItem = item.servicio.getPrecio() * item.cantidad;
                        Label lblText;
                        if (item.cantidad > 1) {
                            lblText = new Label(String.format("%s ($%,.2f) * %d = $%,.2f", 
                                item.servicio.getNombre(), item.servicio.getPrecio(), item.cantidad, subtotalItem));
                        } else {
                            lblText = new Label(String.format("%s (+$%,.2f)", 
                                item.servicio.getNombre(), item.servicio.getPrecio()));
                        }
                        lblText.setStyle("-fx-text-fill: #2c3e50;");

                        hbox.getChildren().addAll(btnDelete, lblText);
                        setGraphic(hbox);
                        setText(null);
                    }
                }
            });
        }
    }

    // ─── CÁLCULO DE TOTAL ESTIMADO ────────────────────────────────────────────
    private void actualizarTotalEstimado() {
        PaqueteDTO paqueteSeleccionado = comboPaquetes.getValue();
        
        // El total se calcula usando los servicios que están en la lista (y se pueden repetir)
        List<ServicioDTO> extras = new ArrayList<>(serviciosSeleccionados);

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
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.initStyle(StageStyle.UTILITY);
            com.mycompany.presentacion.utils.ModalHelper.mostrarModal(root, "Registrar Nuevo Cliente", modalStage);

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

    // ─── ACCIÓN DEL BOTÓN AÑADIR SERVICIO A LA COTIZACIÓN ─────────────────────
    private static String textoONull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    @FXML
    private void abrirDialogoAgregarServicio() {
        List<ServicioDTO> todosLosServicios = consultarCatalogoUseCase.obtenerTodosLosServicios();

        if (todosLosServicios.isEmpty()) {
            mostrarError("No hay servicios disponibles en el catálogo.");
            return;
        }

        // Creamos el Dialog esperando una lista de servicios (para poder agregar varios según la 'cantidad')
        Dialog<List<ServicioDTO>> dialog = new Dialog<>();
        dialog.setTitle("Servicios Extras");
        dialog.setHeaderText(null); 

        ButtonType agregarButtonType = new ButtonType("Añadir Servicio Extra", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, agregarButtonType);

        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));

        // --- SECCIÓN 1: DATOS DEL COBRO ---
        Label lblCobro = new Label("DATOS DEL COBRO:");
        lblCobro.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        GridPane gridCobro = new GridPane();
        gridCobro.setHgap(10); 
        gridCobro.setVgap(10);

        ComboBox<ServicioDTO> comboServicio = new ComboBox<>(FXCollections.observableArrayList(todosLosServicios));
        comboServicio.setPromptText("Seleccione un servicio...");
        comboServicio.setPrefWidth(250);
        comboServicio.setConverter(new StringConverter<ServicioDTO>() {
            @Override
            public String toString(ServicioDTO s) {
                return s == null ? "" : s.getNombre();
            }
            @Override
            public ServicioDTO fromString(String string) { return null; }
        });

        ComboBox<Integer> comboCantidad = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        comboCantidad.getSelectionModel().selectFirst();
        comboCantidad.setPrefWidth(80);

        TextField txtCosto = new TextField();
        txtCosto.setPromptText("$0.00");
        txtCosto.setEditable(false);
        txtCosto.setStyle("-fx-background-color: #f2f2f2; -fx-text-fill: #333333;");

        gridCobro.add(new Label("Servicio del catálogo:"), 0, 0);
        gridCobro.add(comboServicio, 1, 0);
        gridCobro.add(new Label("Cantidad:"), 0, 1);
        gridCobro.add(comboCantidad, 1, 1);
        gridCobro.add(new Label("Precio Unit:"), 0, 2);
        gridCobro.add(txtCosto, 1, 2);

        // --- SECCIÓN 2: DETALLES OPERATIVOS (LOGÍSTICA) ---
        Label lblLogistica = new Label("DETALLES OPERATIVOS (LOGÍSTICA)");
        lblLogistica.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        GridPane gridLogistica = new GridPane();
        gridLogistica.setHgap(10); 
        gridLogistica.setVgap(10);

        TextField txtEspecificaciones = new TextField();
        txtEspecificaciones.setPromptText("Ej. Colores, temática...");
        
        TextField txtDesglose = new TextField();
        txtDesglose.setPromptText("Opcional");
        
        ComboBox<String> comboHora = new ComboBox<>();
        for(int i = 0; i <= 23; i++) {
            comboHora.getItems().add(String.format("%02d:00", i));
            comboHora.getItems().add(String.format("%02d:30", i));
        }
        comboHora.setPromptText("Seleccione hora");

        TextField txtUbicacion = new TextField();
        
        TextField txtResponsable = new TextField();
        txtResponsable.setPromptText("Opcional");

        gridLogistica.add(new Label("Especificaciones/Temática:"), 0, 0);
        gridLogistica.add(txtEspecificaciones, 1, 0);
        gridLogistica.add(new Label("Desglose o variedades:"), 0, 1);
        gridLogistica.add(txtDesglose, 1, 1);
        gridLogistica.add(new Label("Hora de entrega/inicio:"), 0, 2);
        gridLogistica.add(comboHora, 1, 2);
        gridLogistica.add(new Label("Ubicación en salón:"), 0, 3);
        gridLogistica.add(txtUbicacion, 1, 3);
        gridLogistica.add(new Label("Responsable asignado:"), 0, 4);
        gridLogistica.add(txtResponsable, 1, 4);

        vbox.getChildren().addAll(lblCobro, gridCobro, new Separator(), lblLogistica, gridLogistica);
        dialog.getDialogPane().setContent(vbox);

        // --- ESTILOS DE BOTONES ---
        Button agregarBtn = (Button) dialog.getDialogPane().lookupButton(agregarButtonType);
        agregarBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        agregarBtn.setDisable(true); // Bloqueado hasta que seleccione un servicio
        
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.setText("Cancelar");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #7f8c8d; -fx-cursor: hand;");

        // --- LÓGICA DE ACTUALIZACIÓN DE PRECIOS ---
        comboServicio.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                txtCosto.setText(String.format("$%,.2f", newVal.getPrecio()));
                agregarBtn.setDisable(false);
            } else {
                txtCosto.setText("$0.00");
                agregarBtn.setDisable(true);
            }
        });

        // --- CONVERTIR RESULTADO (Se agregan los servicios X veces según la cantidad elegida) ---
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == agregarButtonType) {
                ServicioDTO s = comboServicio.getValue();
                if (s != null) {
                    List<ServicioDTO> listaAEnviar = new ArrayList<>();
                    int cant = comboCantidad.getValue() != null ? comboCantidad.getValue() : 1;
                    
                    // Los datos operativos viajan con cada extra y se guardan en el detalle
                    // de la cotización; al confirmar el evento pasan a su bitácora de logística.
                    DetalleCotizacionDTO datos = new DetalleCotizacionDTO();
                    datos.setEspecificacionesCliente(textoONull(txtEspecificaciones.getText()));
                    datos.setDesgloseOpciones(textoONull(txtDesglose.getText()));
                    datos.setUbicacionMontaje(textoONull(txtUbicacion.getText()));
                    if (comboHora.getValue() != null) {
                        datos.setHoraSugerida(java.time.LocalTime.parse(comboHora.getValue()));
                    }

                    for(int i = 0; i < cant; i++) {
                        // Una instancia por unidad, para que cada una conserve sus datos.
                        ServicioDTO unidad = new ServicioDTO();
                        unidad.setId(s.getId());
                        unidad.setNombre(s.getNombre());
                        unidad.setDescripcion(s.getDescripcion());
                        unidad.setPrecio(s.getPrecio());
                        datosDeExtras.put(unidad, datos);
                        listaAEnviar.add(unidad);
                    }
                    return listaAEnviar;
                }
            }
            return null;
        });

        // --- APLICAR RESULTADO A LA LISTA PRINCIPAL ---
        dialog.showAndWait().ifPresent(listaServiciosElegidos -> {
            if (listaServiciosElegidos != null && !listaServiciosElegidos.isEmpty()) {
                serviciosSeleccionados.addAll(listaServiciosElegidos);
                actualizarTotalEstimado();
            }
        });
    }
    
    /** Pregunta con qué método se recibe el pago; devuelve null si se cancela. */
    private MetodoPago pedirMetodoPago() {
        ChoiceDialog<MetodoPago> dialogo = new ChoiceDialog<>(MetodoPago.EFECTIVO, MetodoPago.values());
        dialogo.setTitle("Método de pago");
        dialogo.setHeaderText("¿Cómo se recibe el pago?");
        dialogo.setContentText("Método:");
        return dialogo.showAndWait().orElse(null);
    }

    @FXML
    private void registrarAbonoCotizacion() {
        if (!isModoEdicion() || getCotizacionEnEdicionId() == null) {
            mostrarError("Primero debe guardar la cotización como borrador.");
            return;
        }
        try {
            double monto = Double.parseDouble(txtAbono.getText().trim());
            double pagadoActual = registrarAnticipoUseCase.obtenerTotalAbonado(getCotizacionEnEdicionId());
            double minimo = registrarAnticipoUseCase.obtenerAnticipoMinimo(getCotizacionEnEdicionId());
            if (pagadoActual < minimo && (pagadoActual + monto) < minimo) {
                mostrarError(String.format("El pago acumulado no alcanza el anticipo mínimo de $%,.2f.", minimo));
                return;
            }
            MetodoPago metodo = comboMetodoPago != null && comboMetodoPago.getValue() != null
                ? comboMetodoPago.getValue() : MetodoPago.EFECTIVO;
            registrarAnticipoUseCase.registrarAnticipo(getCotizacionEnEdicionId(), monto, metodo);
            txtAbono.setText("");
            
            cargarCotizacionesPanel();
            actualizarPagosUI();
            imprimirComprobante(getCotizacionEnEdicionId());
            
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Abono registrado correctamente.");
            ok.showAndWait();
        } catch(NumberFormatException e) {
            mostrarError("Monto inválido.");
        } catch(Exception e) {
            mostrarError(e.getMessage());
        }
    }
    /**
     * Fix #3 — Fuente de verdad corregida para actualizarPagosUI.
     * Antes: parseaba el texto de lblTotalEstimado con Double.parseDouble(),
     *        lo que crasheaba con NumberFormatException si el label tenía
     *        un valor vacío, "$null" o formato decimal local incompatible.
     * Ahora: obtiene el total directamente desde la BD via listarCotizacionesUseCase,
     *        garantizando que el dato siempre es un double válido.
     *
     * Fix #7 — Usa isModoEdicion() y getCotizacionEnEdicionId() del contexto.
     */
    private void actualizarPagosUI() {
        if (!isModoEdicion() || getCotizacionEnEdicionId() == null || panelPagos == null) {
            if (panelPagos != null) { panelPagos.setVisible(false); panelPagos.setManaged(false); }
            return;
        }
        panelPagos.setVisible(true);
        panelPagos.setManaged(true);

        try {
            // Fix #3: obtener el total desde BD, NO desde el texto del Label
            CotizacionDTO cot = listarCotizacionesUseCase.obtenerPorId(getCotizacionEnEdicionId());
            double pagado  = registrarAnticipoUseCase.obtenerTotalAbonado(getCotizacionEnEdicionId());
            // Incluye los cargos extra del evento: es el mismo saldo que limita los abonos.
            double restante = registrarAnticipoUseCase.obtenerSaldoPendiente(getCotizacionEnEdicionId());

            // Un abono puede cambiar el estado (BORRADOR -> VIGENTE): se refleja en
            // el contexto y en el botón Eliminar, que solo aplica a borradores.
            if (cot != null) {
                cotizacionContext.setEstadoEdicionActual(cot.getEstado());
                if (btnEliminarCotizacion != null) {
                    btnEliminarCotizacion.setDisable(cot.getEstado() != EstadoCotizacion.BORRADOR);
                }
            }

            lblPagado.setText(String.format("Pagado: $%,.2f", pagado));
            lblRestante.setText(String.format("Restante: $%,.2f", restante));
        } catch (Exception e) {
            // Degradación elegante: mostrar N/A sin crashear
            if (lblPagado   != null) lblPagado.setText("Pagado: N/A");
            if (lblRestante != null) lblRestante.setText("Restante: N/A");
        }
    }

    /** Vuelve a generar el comprobante (por si se canceló el guardado al registrar el pago). */
    @FXML
    private void generarComprobante() {
        if (!isModoEdicion() || getCotizacionEnEdicionId() == null) {
            mostrarError("Seleccione una cotización.");
            return;
        }
        if (registrarAnticipoUseCase.obtenerTotalAbonado(getCotizacionEnEdicionId()) <= 0) {
            mostrarError("La cotización todavía no tiene pagos registrados.");
            return;
        }
        imprimirComprobante(getCotizacionEnEdicionId());
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
    private void generarContratoEnEdicion() {
        if (!isModoEdicion() || getCotizacionEnEdicionId() == null) {
            mostrarError("Seleccione una cotización guardada para generar el contrato.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar contrato PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fileChooser.setInitialFileName("Contrato.pdf");
        
        File destino = fileChooser.showSaveDialog(btnConfirmarCotizacion.getScene().getWindow());
        if (destino == null) return;
        
        try {
            byte[] pdf = generarContratoUseCase.generarContrato(getCotizacionEnEdicionId());
            try (FileOutputStream fos = new FileOutputStream(destino)) {
                fos.write(pdf);
            }
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(destino);
        } catch (Exception ex) {
            mostrarError("No se pudo generar el contrato PDF.");
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