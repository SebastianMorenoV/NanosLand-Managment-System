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
import com.example.negocio.evento.usecase.HorarioEvento;
import com.example.negocio.exception.CotizacionException;
import com.example.negocio.precio.CalcularPrecioPaqueteUseCase;
import com.example.negocio.precio.PrecioPaquete;
import com.example.negocio.precio.SolicitudPrecioPaquete;
import com.mycompany.common.dtos.ClienteDTO;
import com.mycompany.common.dtos.CotizacionDTO;
import com.mycompany.common.dtos.DetalleCotizacionDTO;
import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.dtos.PaquetePrecioDTO;
import com.mycompany.common.dtos.ServicioDTO;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.MetodoPago;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.presentacion.context.CotizacionContext;
import com.mycompany.presentacion.context.FiltroCotizaciones;
import com.mycompany.presentacion.utils.ComboHoras;
import com.mycompany.presentacion.utils.DialogoGuardarPdf;
import com.mycompany.presentacion.utils.Espera;
import com.mycompany.presentacion.utils.ModalHelper;
import com.mycompany.presentacion.utils.TablaPreciosPaquete;
import com.mycompany.presentacion.utils.ViewSwitcher;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener; 
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets; 
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.shape.SVGPath;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
import java.time.LocalTime;
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
    private final CalcularPrecioPaqueteUseCase calcularPrecioPaqueteUseCase;

    // ─── COMPONENTES FXML ─────────────────────────────────────────────────────
    @FXML
    private SearchableComboBox<ClienteDTO> comboClientes;
    @FXML
    private SearchableComboBox<PaqueteDTO> comboPaquetes;
    @FXML
    private DatePicker datePickerFecha;
    @FXML
    private ComboBox<TurnoEvento> comboTurnos;
    @FXML private ComboBox<LocalTime> comboHoraInicio;
    @FXML private ComboBox<LocalTime> comboHoraFin;
    @FXML
    private TextArea textAreaNotas;

    @FXML private HBox boxOpcionesPaquete;
    @FXML private ComboBox<OpcionComida> comboComida;
    @FXML private ComboBox<Integer> comboPersonas;

    @FXML
    private Label lblNombrePaquete;
    @FXML
    private Label lblPrecioPaquete;
    @FXML
    private Label lblDetallesPaquete;
    // Ticket arriba del total: paquete a precio de lista, extras, descuento y total.
    @FXML private Label lblTicketPaquete;
    @FXML private Label lblTicketPaqueteImporte;
    @FXML private HBox boxTicketDescuento;
    @FXML private Label lblTicketDescuento;
    @FXML private Label lblTicketDescuentoImporte;
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

    @FXML private Button btnVolver;
    @FXML private Label lblTituloCotizacion;
    @FXML private Label lblSubtituloCotizacion;
    @FXML private Button btnRegistrarAbono;
    @FXML private Button btnConfirmarEdicion;
    @FXML
    private Button btnEliminarCotizacion;

    @FXML
    private Button btnAnadirServicio;
    @FXML
    private Button btnGuardarBorrador;
    @FXML
    private Button btnConfirmarCotizacion;

    @FXML private ComboBox<MetodoPago> comboMetodoPago;
    @FXML private HBox boxBotonesNuevos;
    @FXML private HBox boxBotonesEdicion;
    @FXML private Button btnGuardarCambios;

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

    /** Comida que ofrece el paquete elegido. */
    public record OpcionComida(Long id, String nombre) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    /**
     * Paquete, comida y personas de la cotización abierta, como están guardados. Con esa
     * selección el precio lo pide al caso de uso aunque falte elegir comida o personas
     * (p. ej. si el paquete cambió de precio fijo a tabla), para mostrar el pactado.
     */
    private record SeleccionGuardada(Long paqueteId, Long comidaId, Integer personas) {
        boolean coincide(Long paqueteId, Long comidaId, Integer personas) {
            return java.util.Objects.equals(this.paqueteId, paqueteId)
                && java.util.Objects.equals(this.comidaId, comidaId)
                && java.util.Objects.equals(this.personas, personas);
        }
    }

    /**
     * Precio del paquete con la selección actual (de lista, descuento y final);
     * {@code null} si falta elegir algo o no es válida.
     */
    private PrecioPaquete precioActual;
    private SeleccionGuardada seleccionGuardada;
    /** Mientras se llenan los combos por código no se recalcula en cada cambio. */
    private boolean aplicandoPaquete;

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
            volverAlListado(FiltroCotizaciones.de(r.getEstado(), r.getFecha()));
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
        
        precioActual = null;
        seleccionGuardada = null;
        aplicandoPaquete = false;

        ComboHoras.configurarInicio(comboHoraInicio);
        ComboHoras.configurarFin(comboHoraFin);
        configurarComboClientes();
        configurarComboPaquetes();
        configurarOpcionesPaquete();
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
                lblSubtotalExtras.setText(String.format("$%,.2f", subtotalExtras));
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
        // La fecha se manda al cálculo del precio (para los descuentos que vengan después).
        if (datePickerFecha != null) {
            datePickerFecha.valueProperty().addListener((obs, old, val) -> actualizarPrecioPaquete());
        }
        comboTurnos.valueProperty().addListener((obs, old, val) -> validarFormularioCompleto());
        comboHoraInicio.valueProperty().addListener((obs, old, val) -> validarFormularioCompleto());
        comboHoraFin.valueProperty().addListener((obs, old, val) -> validarFormularioCompleto());
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

        // Se llegó desde Cotizaciones, Eventos o Inicio para abrir una ya guardada.
        if (isModoEdicion() && getCotizacionEnEdicionId() != null) {
            abrirCotizacionGuardada(getCotizacionEnEdicionId());
        }
    }

    // ─── GUARDAR/ACTUALIZAR COTIZACIÓN ──────────────────────────────────────
    @FXML
    private void guardarBorrador() {
        CotizacionDTO r = guardarOActualizarCotizacion(EstadoCotizacion.BORRADOR);
        if(r != null) {
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Borrador guardado. Folio: " + r.getFolio());
            ok.showAndWait();
            volverAlListado(FiltroCotizaciones.de(r.getEstado(), r.getFecha()));
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
            volverAlListado(FiltroCotizaciones.CONFIRMADAS);
        } else {
            pedirMontoAnticipo(minimo, minimo - pagado).ifPresent(montoStr -> {
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
                    volverAlListado(FiltroCotizaciones.CONFIRMADAS);
                } catch(NumberFormatException e) {
                    mostrarError("Monto inválido.");
                } catch(Exception e) {
                    mostrarError(e.getMessage());
                }
            });
            // Si no se completó el pago, la cotización ya quedó guardada: se muestra como tal.
            if (isModoEdicion() && getCotizacionEnEdicionId() != null) {
                abrirCotizacionGuardada(getCotizacionEnEdicionId());
            }
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
            File destino = DialogoGuardarPdf.elegir(modalStage, "Guardar comprobante PDF",
                    generarComprobanteUseCase.nombreArchivo(cotizacionGuardada.getId()));
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
            File destino = DialogoGuardarPdf.elegir(modalStage, "Guardar contrato PDF",
                    generarContratoUseCase.nombreArchivo(cotizacionGuardada.getId()));
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
        if (comboComida != null) comboComida.setDisable(true);
        if (comboPersonas != null) comboPersonas.setDisable(true);
        if (listaServiciosExtras != null) listaServiciosExtras.setDisable(true);
        if (btnAnadirServicio != null) btnAnadirServicio.setDisable(true);
        if (btnGuardarBorrador != null) btnGuardarBorrador.setDisable(true);
        if (btnConfirmarCotizacion != null) btnConfirmarCotizacion.setDisable(true);
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
            // El precio pactado se respeta mientras no se cambie el paquete, la comida, las personas
            // o la fecha; lo decide CalcularPrecioPaqueteUseCase comparando con lo guardado.
            seleccionGuardada = new SeleccionGuardada(cotizacion.getPaqueteId(), cotizacion.getComidaId(),
                cotizacion.getPersonas());
            PaqueteDTO paqueteEncontrado = comboPaquetes.getItems().stream()
                .filter(p -> p.getId() != null && p.getId().equals(cotizacion.getPaqueteId()))
                .findFirst()
                .orElse(null);
            if (paqueteEncontrado == null) {
                // Paquete dado de baja después de cotizar: se agrega para que la cotización
                // se vea completa y se pueda seguir editando con su precio pactado.
                paqueteEncontrado = consultarCatalogoUseCase.obtenerPaquetePorId(cotizacion.getPaqueteId());
                if (paqueteEncontrado != null) {
                    comboPaquetes.getItems().add(paqueteEncontrado);
                }
            }
            comboPaquetes.setValue(paqueteEncontrado);
            aplicarPaquete(paqueteEncontrado, cotizacion.getComidaId(), cotizacion.getComidaNombre(), cotizacion.getPersonas());
        }

        if (cotizacion.getTurno() != null) {
            comboTurnos.setValue(cotizacion.getTurno());
        }
        // Las cotizaciones guardadas antes de capturar el horario llegan sin horas: se piden al guardar.
        comboHoraInicio.setValue(cotizacion.getHoraInicio());
        comboHoraFin.setValue(cotizacion.getHoraFin());

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

    // ─── ABRIR COTIZACIÓN GUARDADA / SALIR ─────────────────────────────────
    private void abrirCotizacionGuardada(Long id) {
        CotizacionDTO cotizacion = listarCotizacionesUseCase.obtenerPorId(id);
        if (cotizacion == null || cotizacion.getEstado() == EstadoCotizacion.ELIMINADA) {
            mostrarError("Esta cotización ya no existe.");
            // Después de terminar de cargar esta pantalla, para no pisar la navegación.
            Platform.runLater(() -> volverAlListado(FiltroCotizaciones.POR_CONFIRMAR));
            return;
        }
        cotizacionContext.setFechaEdicionActual(cotizacion.getFecha());
        cotizacionContext.setTurnoEdicionActual(cotizacion.getTurno());
        cotizacionContext.setEstadoEdicionActual(cotizacion.getEstado());

        cargarCotizacionEnFormulario(cotizacion);
        cambiarModoInterfaz(true);

        boolean esBorrador = cotizacion.getEstado() == EstadoCotizacion.BORRADOR;
        boolean esVigenteFutura = cotizacion.getEstado() == EstadoCotizacion.VIGENTE
                && cotizacion.getFecha() != null && !cotizacion.getFecha().isBefore(LocalDate.now());
        boolean editable = esBorrador || esVigenteFutura;
        btnGuardarCambios.setDisable(!editable);
        setSoloLectura(!editable);
        if (!editable) {
            ocultarErrorTurnos(); // en una cotización de solo consulta no aplica elegir turno
        }
        // Confirmada y el evento es hoy (en curso): ya no se cambia de fecha ni de turno;
        // si no se realiza, se cancela. Lo demás (horario, notas, extras) sí se puede editar.
        if (cotizacion.getEstado() == EstadoCotizacion.VIGENTE && LocalDate.now().equals(cotizacion.getFecha())) {
            datePickerFecha.setDisable(true);
            comboTurnos.setDisable(true);
        }

        lblTituloCotizacion.setText("Cotización " + (cotizacion.getFolio() != null ? cotizacion.getFolio() : ""));
        btnVolver.setText("← Volver sin guardar");
        actualizarAccionesPorEstado(cotizacion);
    }

    /** Muestra solo lo que se puede hacer con la cotización en su estado actual. */
    private void actualizarAccionesPorEstado(CotizacionDTO cotizacion) {
        EstadoCotizacion estado = cotizacion.getEstado();
        boolean esBorrador = estado == EstadoCotizacion.BORRADOR;
        boolean cancelada = estado == EstadoCotizacion.CANCELADA;
        boolean fechaPasada = cotizacion.getFecha() != null && cotizacion.getFecha().isBefore(LocalDate.now());

        btnEliminarCotizacion.setDisable(!esBorrador);
        btnEliminarCotizacion.setVisible(esBorrador);
        btnEliminarCotizacion.setManaged(esBorrador);
        btnConfirmarEdicion.setVisible(esBorrador);
        btnConfirmarEdicion.setManaged(esBorrador);

        txtAbono.setDisable(cancelada);
        comboMetodoPago.setDisable(cancelada);
        btnRegistrarAbono.setDisable(cancelada);

        String mensaje;
        if (cancelada) {
            mensaje = "Cancelada. Solo se puede consultar.";
        } else if (esBorrador) {
            mensaje = fechaPasada
                    ? "La fecha ya pasó. Elige otra fecha para poder confirmarla."
                    : "Sin confirmar. Registra el anticipo para apartar la fecha.";
        } else {
            mensaje = fechaPasada
                    ? "Este evento ya pasó. Se puede consultar y registrar pagos."
                    : "Confirmada. Aquí puedes registrar pagos y generar el contrato.";
        }
        lblSubtituloCotizacion.setText(mensaje);
    }

    @FXML
    private void volver() {
        if (isModoEdicion()) {
            volverAlListado(FiltroCotizaciones.de(getEstadoEdicion(), getFechaEdicion()));
            return;
        }
        if (hayDatosCapturados()) {
            ButtonType salir = new ButtonType("Sí, salir", ButtonBar.ButtonData.OK_DONE);
            ButtonType seguir = new ButtonType("Seguir capturando", ButtonBar.ButtonData.CANCEL_CLOSE);
            Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION, "", salir, seguir);
            confirmar.setTitle("Salir de la cotización");
            confirmar.setHeaderText("¿Salir sin guardar?");
            confirmar.setContentText("Se perderá lo que capturaste en esta cotización.");
            if (confirmar.showAndWait().orElse(seguir) != salir) {
                return;
            }
        }
        cotizacionContext.limpiar();
        ViewSwitcher.cargarVista("SeleccionarFecha.fxml");
    }

    private boolean hayDatosCapturados() {
        return comboClientes.getValue() != null || comboPaquetes.getValue() != null
                || comboHoraInicio.getValue() != null || comboHoraFin.getValue() != null
                || !serviciosSeleccionados.isEmpty()
                || !textoVacio(txtNombreFestejado.getText()) || !textoVacio(txtTematica.getText())
                || !textoVacio(textAreaNotas.getText());
    }

    private static boolean textoVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    /** Sale de la cotización hacia donde se vino; si es nueva, a la lista de Cotizaciones. */
    private void volverAlListado(FiltroCotizaciones filtro) {
        String regreso = cotizacionContext.getVistaRegreso();
        cotizacionContext.limpiar();
        if (regreso != null && !regreso.equals("Cotizaciones.fxml")) {
            ViewSwitcher.cargarVista(regreso);
            return;
        }
        cotizacionContext.setFiltroLista(filtro);
        ViewSwitcher.cargarVista("Cotizaciones.fxml");
    }

    private void setSoloLectura(boolean soloLectura) {
        if (comboClientes != null) comboClientes.setDisable(soloLectura);
        if (comboPaquetes != null) comboPaquetes.setDisable(soloLectura);
        if (comboComida != null) comboComida.setDisable(soloLectura);
        if (comboPersonas != null) comboPersonas.setDisable(soloLectura);
        if (datePickerFecha != null) datePickerFecha.setDisable(soloLectura);
        if (comboTurnos != null) {
            comboTurnos.setDisable(soloLectura);
            // Si sale de solo lectura pero no hay fecha, debe seguir deshabilitado
            if (!soloLectura && datePickerFecha != null && datePickerFecha.getValue() == null) {
                comboTurnos.setDisable(true);
            }
        }
        if (comboHoraInicio != null) comboHoraInicio.setDisable(soloLectura);
        if (comboHoraFin != null) comboHoraFin.setDisable(soloLectura);
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
                             comboHoraInicio == null || comboHoraInicio.getValue() == null ||
                             comboHoraFin == null || comboHoraFin.getValue() == null ||
                             txtNombreFestejado == null || txtNombreFestejado.getText() == null || txtNombreFestejado.getText().trim().isEmpty() ||
                             // La temática es opcional.
                             faltaComidaOPersonas();
                             
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

                volverAlListado(FiltroCotizaciones.POR_CONFIRMAR);
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
        if (faltaComidaOPersonas()) {
            throw new CotizacionException("Elija la comida y el número de personas del paquete.");
        }
        if (fecha == null) {
            throw new CotizacionException("Debe seleccionar una fecha para el evento.");
        }
        if (turno == null) {
            throw new CotizacionException("Debe seleccionar un turno (Matutino o Vespertino).");
        }
        LocalTime horaInicio = comboHoraInicio.getValue();
        LocalTime horaFin = comboHoraFin.getValue();
        HorarioEvento.validar(horaInicio, horaFin);

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
        // El precio no se manda: el servidor lo calcula con el paquete, la comida y las personas.
        if (paqueteSeleccionado.isTablaPrecios()) {
            dto.setComidaId(comboComida.getValue().id());
            dto.setPersonas(comboPersonas.getValue());
        }
        dto.setFecha(fecha);
        dto.setTurno(turno);
        dto.setHoraInicio(horaInicio);
        dto.setHoraFin(horaFin);
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
                if (paquete == null) return "";
                return paquete.isActivo() ? paquete.getNombre() : paquete.getNombre() + " (ya no se ofrece)";
            }
            @Override
            public PaqueteDTO fromString(String string) { return null; }
        });

        // También se dispara al asignar el paquete por código (al abrir una cotización guardada).
        comboPaquetes.setOnAction(event -> aplicarPaquete(comboPaquetes.getValue(), null, null, null));
    }

    // ─── COMIDA Y PERSONAS (PAQUETES CON TABLA DE PRECIOS) ──────────────────
    private void configurarOpcionesPaquete() {
        comboPersonas.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer personas) {
                return personas == null ? "" : personas + " personas";
            }
            @Override
            public Integer fromString(String string) { return null; }
        });
        comboComida.valueProperty().addListener((obs, anterior, nueva) -> {
            if (aplicandoPaquete) return;
            llenarPersonas(comboPaquetes.getValue(), comboPersonas.getValue(), null);
            actualizarPrecioPaquete();
            validarFormularioCompleto();
        });
        comboPersonas.valueProperty().addListener((obs, anterior, nueva) -> {
            if (aplicandoPaquete) return;
            actualizarPrecioPaquete();
            validarFormularioCompleto();
        });
    }

    /**
     * Muestra el paquete elegido y, si su precio depende de la comida y las personas, llena
     * esos combos. Conserva la comida y las personas ya elegidas si el paquete nuevo las ofrece.
     *
     * @param comidaId     comida guardada que se debe seleccionar (al abrir una cotización), o null
     * @param comidaNombre su nombre, por si ya no está en la tabla del paquete
     * @param personas     personas guardadas que se deben seleccionar, o null
     */
    private void aplicarPaquete(PaqueteDTO paquete, Long comidaId, String comidaNombre, Integer personas) {
        aplicandoPaquete = true;
        try {
            Long comidaDeseada = comidaId != null ? comidaId
                : (comboComida.getValue() != null ? comboComida.getValue().id() : null);
            Integer personasDeseadas = personas != null ? personas : comboPersonas.getValue();
            boolean conTabla = paquete != null && paquete.isTablaPrecios();

            boxOpcionesPaquete.setVisible(conTabla);
            boxOpcionesPaquete.setManaged(conTabla);
            if (!conTabla) {
                comboComida.getItems().clear();
                comboComida.setValue(null);
                comboPersonas.getItems().clear();
                comboPersonas.setValue(null);
            } else {
                List<OpcionComida> opciones = new ArrayList<>();
                for (PaquetePrecioDTO p : paquete.getPrecios()) {
                    if (opciones.stream().noneMatch(o -> o.id().equals(p.getComidaId()))) {
                        opciones.add(new OpcionComida(p.getComidaId(), p.getComidaNombre()));
                    }
                }
                opciones.sort(java.util.Comparator.comparing(OpcionComida::nombre, String.CASE_INSENSITIVE_ORDER));
                if (comidaId != null && opciones.stream().noneMatch(o -> o.id().equals(comidaId))) {
                    opciones.add(new OpcionComida(comidaId, comidaNombre != null ? comidaNombre : "Comida " + comidaId));
                }
                comboComida.getItems().setAll(opciones);
                comboComida.setValue(opciones.stream()
                    .filter(o -> o.id().equals(comidaDeseada)).findFirst().orElse(null));
                llenarPersonas(paquete, personasDeseadas, personas);
            }

            if (paquete == null) {
                lblNombrePaquete.setText("Seleccione un paquete");
                lblDetallesPaquete.setText("Los detalles aparecerán aquí");
            } else {
                lblNombrePaquete.setText(paquete.isActivo() ? paquete.getNombre() : paquete.getNombre() + " (ya no se ofrece)");
                lblDetallesPaquete.setText(paquete.getDescripcion() != null && !paquete.getDescripcion().isBlank()
                    ? paquete.getDescripcion() : "Sin detalles adicionales");
            }
        } finally {
            aplicandoPaquete = false;
        }
        actualizarPrecioPaquete();
        validarFormularioCompleto();
    }

    /** Rangos de personas que el paquete ofrece para la comida elegida (o todos si aún no hay comida). */
    private void llenarPersonas(PaqueteDTO paquete, Integer deseadas, Integer guardadas) {
        boolean previo = aplicandoPaquete;
        aplicandoPaquete = true;
        try {
            java.util.TreeSet<Integer> rangos = new java.util.TreeSet<>();
            OpcionComida comida = comboComida.getValue();
            if (paquete != null && paquete.getPrecios() != null) {
                for (PaquetePrecioDTO p : paquete.getPrecios()) {
                    if (comida == null || comida.id().equals(p.getComidaId())) rangos.add(p.getPersonas());
                }
            }
            if (guardadas != null) rangos.add(guardadas);
            comboPersonas.getItems().setAll(rangos);
            comboPersonas.setValue(deseadas != null && rangos.contains(deseadas) ? deseadas : null);
        } finally {
            aplicandoPaquete = previo;
        }
    }

    /** Un paquete con tabla de precios necesita comida y número de personas. */
    private boolean faltaComidaOPersonas() {
        PaqueteDTO paquete = comboPaquetes == null ? null : comboPaquetes.getValue();
        return paquete != null && paquete.isTablaPrecios()
            && (comboComida.getValue() == null || comboPersonas.getValue() == null);
    }

    /**
     * Precio del paquete con la selección actual. Lo calcula el mismo caso de uso que usa el
     * servidor al guardar, con el descuento que toque por la fecha del evento; si la selección
     * es la de la cotización abierta, es su precio pactado. La tarjeta del paquete muestra el
     * precio de lista; el descuento va en el ticket, arriba del total.
     */
    private void actualizarPrecioPaquete() {
        if (aplicandoPaquete || comboPaquetes == null) return;
        PaqueteDTO paquete = comboPaquetes.getValue();
        precioActual = null;
        lblPrecioPaquete.setTooltip(null);

        if (paquete == null) {
            lblPrecioPaquete.setText("$0.00");
        } else {
            Long comidaId = comboComida.getValue() != null ? comboComida.getValue().id() : null;
            Integer personas = comboPersonas.getValue();
            if (!paquete.isTablaPrecios()) {
                comidaId = null;
                personas = null;
            }
            boolean esLaGuardada = seleccionGuardada != null
                    && seleccionGuardada.coincide(paquete.getId(), comidaId, personas);
            if (!esLaGuardada && paquete.isTablaPrecios() && (comidaId == null || personas == null)) {
                lblPrecioPaquete.setText("Desde " + TablaPreciosPaquete.dinero(paquete.getPrecioDesde()));
            } else {
                try {
                    SolicitudPrecioPaquete solicitud = new SolicitudPrecioPaquete(paquete.getId(), comidaId, personas,
                        datePickerFecha != null ? datePickerFecha.getValue() : null);
                    PrecioPaquete precio = calcularPrecioPaqueteUseCase.calcularParaCotizacion(
                        isModoEdicion() ? getCotizacionEnEdicionId() : null, solicitud);
                    precioActual = precio;
                    lblPrecioPaquete.setText(TablaPreciosPaquete.dinero(precio.precioLista()));
                } catch (CotizacionException e) {
                    lblPrecioPaquete.setText("—");
                    lblPrecioPaquete.setTooltip(new Tooltip(e.getMessage()));
                }
            }
        }
        actualizarTotalEstimado();
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
        // El total se calcula usando los servicios que están en la lista (y se pueden repetir)
        List<ServicioDTO> extras = new ArrayList<>(serviciosSeleccionados);

        double total = calcularTotalCotizacionUseCase.ejecutar(
            precioActual != null ? precioActual.precio() : 0, extras);

        if (lblTotalEstimado != null) {
            lblTotalEstimado.setText(String.format("$%,.2f", total));
        }
        actualizarTicket();
    }

    /** Renglones del ticket: el paquete a precio de lista y, si toca, su descuento antes del total. */
    private void actualizarTicket() {
        if (lblTicketPaquete == null) return;
        PaqueteDTO paquete = comboPaquetes != null ? comboPaquetes.getValue() : null;
        lblTicketPaquete.setText(paquete != null ? paquete.getNombre() : "Paquete");
        lblTicketPaqueteImporte.setText(precioActual != null ? TablaPreciosPaquete.dinero(precioActual.precioLista()) : "—");

        boolean conDescuento = precioActual != null && precioActual.tieneDescuento();
        boxTicketDescuento.setVisible(conDescuento);
        boxTicketDescuento.setManaged(conDescuento);
        if (conDescuento) {
            lblTicketDescuento.setText("Descuento «" + precioActual.descuento() + "»");
            lblTicketDescuentoImporte.setText("−" + TablaPreciosPaquete.dinero(precioActual.montoDescuento()));
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

    private static Label etiqueta(String texto, String claseCss) {
        Label lbl = new Label(texto);
        lbl.getStyleClass().add(claseCss);
        return lbl;
    }

    @FXML
    private void abrirDialogoAgregarServicio() {
        // Solo los de categoría Extra: los incluidos y las comidas van dentro de los paquetes.
        List<ServicioDTO> todosLosServicios = consultarCatalogoUseCase.obtenerServiciosExtra();

        if (todosLosServicios.isEmpty()) {
            mostrarError("No hay servicios extra disponibles en el catálogo.");
            return;
        }

        // Creamos el Dialog esperando una lista de servicios (para poder agregar varios según la 'cantidad')
        Dialog<List<ServicioDTO>> dialog = new Dialog<>();
        dialog.setTitle("Añadir Servicio Extra");

        ButtonType agregarButtonType = new ButtonType("Añadir Servicio Extra", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, agregarButtonType);

        // --- SECCIÓN 1: DATOS DEL COBRO ---
        ComboBox<ServicioDTO> comboServicio = new ComboBox<>(FXCollections.observableArrayList(todosLosServicios));
        comboServicio.setPromptText("Seleccione un servicio...");
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

        VBox campoServicio = ModalHelper.campo("SERVICIO DEL CATÁLOGO *", comboServicio);
        HBox.setHgrow(campoServicio, Priority.ALWAYS);
        VBox campoCantidad = ModalHelper.campo("CANTIDAD", comboCantidad);
        campoCantidad.setMinWidth(110);
        campoCantidad.setPrefWidth(110);
        HBox filaServicio = new HBox(12, campoServicio, campoCantidad);

        // Resumen del cobro: precio unitario y total según la cantidad
        Label lblPrecioUnitario = etiqueta("—", "resumen-cobro-valor");
        Label lblTotalTitulo = etiqueta("TOTAL", "label-pequeno");
        Label lblTotal = etiqueta("$0.00", "resumen-cobro-total");
        VBox bloquePrecio = new VBox(2, etiqueta("PRECIO UNITARIO", "label-pequeno"), lblPrecioUnitario);
        VBox bloqueTotal = new VBox(2, lblTotalTitulo, lblTotal);
        bloqueTotal.setAlignment(Pos.CENTER_RIGHT);
        Region espacioResumen = new Region();
        HBox.setHgrow(espacioResumen, Priority.ALWAYS);
        HBox resumenCobro = new HBox(bloquePrecio, espacioResumen, bloqueTotal);
        resumenCobro.setAlignment(Pos.CENTER_LEFT);
        resumenCobro.getStyleClass().add("resumen-cobro");

        VBox seccionCobro = new VBox(10, etiqueta("DATOS DEL COBRO", "titulo-seccion"), filaServicio, resumenCobro);

        // --- SECCIÓN 2: DETALLES OPERATIVOS (LOGÍSTICA) ---
        TextField txtEspecificaciones = ModalHelper.campoTexto("Ej. Colores, temática...");
        TextField txtDesglose = ModalHelper.campoTexto("Opcional");
        TextField txtUbicacion = ModalHelper.campoTexto("Ej. Junto a la pista");
        TextField txtResponsable = ModalHelper.campoTexto("Opcional");

        ComboBox<String> comboHora = new ComboBox<>();
        for(int i = 0; i <= 23; i++) {
            comboHora.getItems().add(String.format("%02d:00", i));
            comboHora.getItems().add(String.format("%02d:30", i));
        }
        comboHora.setPromptText("Seleccione hora");
        comboHora.setVisibleRowCount(8);

        // Dos columnas del mismo ancho; las especificaciones ocupan la fila completa
        GridPane gridLogistica = new GridPane();
        gridLogistica.setHgap(12);
        gridLogistica.setVgap(10);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints mitad = new ColumnConstraints();
            mitad.setPercentWidth(50);
            mitad.setHgrow(Priority.ALWAYS);
            gridLogistica.getColumnConstraints().add(mitad);
        }
        gridLogistica.add(ModalHelper.campo("ESPECIFICACIONES / TEMÁTICA", txtEspecificaciones), 0, 0, 2, 1);
        gridLogistica.add(ModalHelper.campo("DESGLOSE O VARIEDADES", txtDesglose), 0, 1);
        gridLogistica.add(ModalHelper.campo("UBICACIÓN EN SALÓN", txtUbicacion), 1, 1);
        gridLogistica.add(ModalHelper.campo("HORA DE ENTREGA / INICIO", comboHora), 0, 2);
        gridLogistica.add(ModalHelper.campo("RESPONSABLE ASIGNADO", txtResponsable), 1, 2);

        VBox seccionLogistica = new VBox(10, etiqueta("DETALLES OPERATIVOS (LOGÍSTICA)", "titulo-seccion"), gridLogistica);

        // Espaciado compacto para que quepa en pantallas de 1280x720
        VBox contenido = new VBox(14,
                ModalHelper.encabezado("Añadir Servicio Extra",
                        "Seleccione un servicio del catálogo y capture los detalles para logística."),
                seccionCobro, new Separator(), seccionLogistica);
        contenido.setPrefWidth(520);
        dialog.getDialogPane().setContent(contenido);
        ModalHelper.aplicarEstilo(dialog, btnAnadirServicio.getScene().getWindow());

        Button agregarBtn = (Button) dialog.getDialogPane().lookupButton(agregarButtonType);
        agregarBtn.setDisable(true); // Bloqueado hasta que seleccione un servicio

        // --- LÓGICA DE ACTUALIZACIÓN DE PRECIOS ---
        Runnable actualizarResumen = () -> {
            ServicioDTO s = comboServicio.getValue();
            int cant = comboCantidad.getValue() != null ? comboCantidad.getValue() : 1;
            lblTotalTitulo.setText(cant > 1 ? "TOTAL (" + cant + " UNIDADES)" : "TOTAL");
            if (s != null) {
                lblPrecioUnitario.setText(String.format("$%,.2f", s.getPrecio()));
                lblTotal.setText(String.format("$%,.2f", s.getPrecio() * cant));
            } else {
                lblPrecioUnitario.setText("—");
                lblTotal.setText("$0.00");
            }
            agregarBtn.setDisable(s == null);
        };
        comboServicio.valueProperty().addListener((obs, old, newVal) -> actualizarResumen.run());
        comboCantidad.valueProperty().addListener((obs, old, newVal) -> actualizarResumen.run());

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
    
    /**
     * Pide el monto del anticipo con el que se confirma la cotización.
     *
     * @param minimo   anticipo mínimo para confirmar.
     * @param sugerido monto con el que se llena el campo (lo que falta para el mínimo).
     * @return el texto capturado, o vacío si se cancela.
     */
    private java.util.Optional<String> pedirMontoAnticipo(double minimo, double sugerido) {
        TextField txtMonto = ModalHelper.campoTexto("0.00");
        txtMonto.setText(String.format(java.util.Locale.ROOT, "%.2f", sugerido));

        Dialog<String> dialogo = new Dialog<>();
        dialogo.setTitle("Confirmar Cotización");
        ButtonType continuarButtonType = new ButtonType("Continuar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, continuarButtonType);

        VBox contenido = new VBox(18,
                ModalHelper.encabezado("Confirmar Cotización",
                        String.format("Se requiere un anticipo mínimo de $%,.2f para confirmar y bloquear la fecha.", minimo)),
                ModalHelper.campo("MONTO A PAGAR ($) *", txtMonto));
        contenido.setPrefWidth(400);
        dialogo.getDialogPane().setContent(contenido);
        ModalHelper.aplicarEstilo(dialogo, btnConfirmarCotizacion.getScene().getWindow());
        dialogo.setResultConverter(boton -> boton == continuarButtonType ? txtMonto.getText() : null);
        dialogo.setOnShown(e -> Platform.runLater(() -> {
            txtMonto.requestFocus();
            txtMonto.selectAll();
        }));
        return dialogo.showAndWait();
    }

    /** Pregunta con qué método se recibe el pago; devuelve null si se cancela. */
    private MetodoPago pedirMetodoPago() {
        ComboBox<MetodoPago> comboMetodo = new ComboBox<>(FXCollections.observableArrayList(MetodoPago.values()));
        comboMetodo.setValue(MetodoPago.EFECTIVO);

        Dialog<MetodoPago> dialogo = new Dialog<>();
        dialogo.setTitle("Método de pago");
        ButtonType aceptarButtonType = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, aceptarButtonType);

        VBox contenido = new VBox(18,
                ModalHelper.encabezado("Método de pago", "¿Cómo se recibe el pago?"),
                ModalHelper.campo("MÉTODO DE PAGO", comboMetodo));
        contenido.setPrefWidth(360);
        dialogo.getDialogPane().setContent(contenido);
        ModalHelper.aplicarEstilo(dialogo, btnConfirmarCotizacion.getScene().getWindow());
        dialogo.setResultConverter(boton -> boton == aceptarButtonType ? comboMetodo.getValue() : null);
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
            // el contexto y en las acciones disponibles.
            if (cot != null) {
                cotizacionContext.setEstadoEdicionActual(cot.getEstado());
                actualizarAccionesPorEstado(cot);
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
        File destino = DialogoGuardarPdf.elegir(btnConfirmarCotizacion.getScene().getWindow(),
                "Guardar comprobante PDF", generarComprobanteUseCase.nombreArchivo(id));
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

        Long id = getCotizacionEnEdicionId();
        File destino = DialogoGuardarPdf.elegir(btnConfirmarCotizacion.getScene().getWindow(),
                "Guardar contrato PDF", generarContratoUseCase.nombreArchivo(id));
        if (destino == null) return;

        Espera.ejecutar(btnConfirmarCotizacion, "Generando el contrato…", () -> {
            byte[] pdf = generarContratoUseCase.generarContrato(id);
            try (FileOutputStream fos = new FileOutputStream(destino)) {
                fos.write(pdf);
            }
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(destino);
            return null;
        }, sinResultado -> { }, error -> mostrarError("No se pudo generar el contrato PDF."));
    }
    
    
}