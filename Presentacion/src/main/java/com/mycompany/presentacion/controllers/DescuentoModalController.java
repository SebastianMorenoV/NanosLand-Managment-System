package com.mycompany.presentacion.controllers;

import com.example.negocio.descuento.usecase.GestionarDescuentoUseCase;
import com.example.negocio.exception.CotizacionException;
import com.example.negocio.paquete.usecase.ConsultarPaquetesUseCase;
import com.mycompany.common.dtos.DescuentoDTO;
import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.util.DescripcionDescuento;
import com.mycompany.persistencia.enums.TipoDescuento;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * Alta y edición de un descuento, como preguntas numeradas que se contestan con clics:
 * cuánto, a qué paquetes, qué días, en qué fechas y hasta cuándo se cotiza. A la derecha
 * muestra cómo queda un paquete de ejemplo en el ticket de la cotización. Una instancia
 * por ventana.
 */
@Controller
@Scope("prototype")
@RequiredArgsConstructor
public class DescuentoModalController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter LECTURA_FECHA = DateTimeFormatter.ofPattern("d/M/uuuu");
    private static final String[] NOMBRES_DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    private final GestionarDescuentoUseCase gestionarDescuentoUseCase;
    private final ConsultarPaquetesUseCase consultarPaquetesUseCase;

    @FXML private Label lblTitulo;
    @FXML private TextField txtNombre;
    @FXML private ToggleButton opPorcentaje;
    @FXML private ToggleButton opMontoFijo;
    @FXML private Label lblUnidadAntes;
    @FXML private TextField txtValor;
    @FXML private Label lblUnidadDespues;
    @FXML private ToggleButton opTodosPaquetes;
    @FXML private ToggleButton opAlgunosPaquetes;
    @FXML private FlowPane boxPaquetes;
    @FXML private HBox boxDias;
    @FXML private ToggleButton opTodoElAnio;
    @FXML private ToggleButton opTemporada;
    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFin;
    @FXML private ToggleButton opSinLimite;
    @FXML private ToggleButton opHastaFecha;
    @FXML private DatePicker dpCotizarHasta;

    @FXML private ComboBox<PaqueteDTO> cmbEjemplo;
    @FXML private Label lblEjemploPaquete;
    @FXML private Label lblEjemploPrecio;
    @FXML private HBox boxEjemploDescuento;
    @FXML private Label lblEjemploDescuento;
    @FXML private Label lblEjemploMonto;
    @FXML private Label lblEjemploTotal;
    @FXML private Label lblEjemploNota;
    @FXML private Label lblResumen;
    @FXML private Label lblAviso;

    private DescuentoDTO descuentoAEditar;
    /** El descuento guardado al cerrar, o {@code null} si se canceló. */
    @Getter
    private DescuentoDTO guardado;

    private final Map<DayOfWeek, ToggleButton> dias = new EnumMap<>(DayOfWeek.class);
    /** Pastilla de cada paquete, por id. */
    private final Map<Long, ToggleButton> paquetes = new LinkedHashMap<>();
    /** Paquetes elegidos en "Solo algunos" (se recuerdan aunque se cambie a "Todos"). */
    private final Set<Long> paquetesElegidos = new HashSet<>();
    /** Mientras se pintan las pastillas por código no se toman como elección del usuario. */
    private boolean pintandoPaquetes;

    @FXML
    public void initialize() {
        txtValor.getStyleClass().add("campo-cantidad");
        txtValor.setTextFormatter(new TextFormatter<>(change ->
            change.getControlNewText().matches("\\d{0,7}(\\.\\d{0,2})?") ? change : null));

        grupo(opPorcentaje, opMontoFijo).selectedToggleProperty().addListener((obs, a, b) -> aplicarTipo());
        grupo(opTodosPaquetes, opAlgunosPaquetes).selectedToggleProperty().addListener((obs, a, b) -> aplicarPaquetes());
        grupo(opTodoElAnio, opTemporada).selectedToggleProperty().addListener((obs, a, b) -> aplicarFechas());
        grupo(opSinLimite, opHastaFecha).selectedToggleProperty().addListener((obs, a, b) -> aplicarFechas());

        for (DayOfWeek dia : DayOfWeek.values()) {
            ToggleButton boton = new ToggleButton(DescripcionDescuento.nombreCortoDia(dia));
            boton.getStyleClass().addAll("opcion-pastilla", "opcion-dia");
            boton.setTooltip(new Tooltip(NOMBRES_DIAS[dia.getValue() - 1]));
            boton.setSelected(true);
            boton.selectedProperty().addListener((obs, antes, ahora) -> actualizarVistaPrevia());
            dias.put(dia, boton);
            boxDias.getChildren().add(boton);
        }

        List<PaqueteDTO> activos = new ArrayList<>(consultarPaquetesUseCase.obtenerPaquetesActivos());
        activos.sort(Comparator.comparing(PaqueteDTO::getNombre, String.CASE_INSENSITIVE_ORDER));
        activos.forEach(p -> agregarPaquete(p.getId(), p.getNombre()));

        cmbEjemplo.getItems().setAll(activos);
        cmbEjemplo.setConverter(new StringConverter<>() {
            @Override public String toString(PaqueteDTO p) { return p == null ? "" : p.getNombre(); }
            @Override public PaqueteDTO fromString(String s) { return null; }
        });
        if (!activos.isEmpty()) {
            cmbEjemplo.setValue(activos.get(0));
        }
        cmbEjemplo.valueProperty().addListener((obs, antes, ahora) -> actualizarVistaPrevia());

        for (DatePicker dp : List.of(dpInicio, dpFin, dpCotizarHasta)) {
            dp.setConverter(new StringConverter<>() {
                @Override public String toString(LocalDate fecha) { return fecha == null ? "" : FECHA.format(fecha); }
                @Override public LocalDate fromString(String texto) {
                    // Mal escrita se queda la que tenía; al guardar se avisa (ver confirmar).
                    try {
                        return leerFecha(texto);
                    } catch (DateTimeParseException e) {
                        return dp.getValue();
                    }
                }
            });
            dp.valueProperty().addListener((obs, antes, ahora) -> actualizarVistaPrevia());
            dp.setTooltip(new Tooltip("Elíjala en el calendario o escríbala como 01/12/2026"));
        }

        txtNombre.textProperty().addListener((obs, antes, ahora) -> actualizarVistaPrevia());
        txtValor.textProperty().addListener((obs, antes, ahora) -> actualizarVistaPrevia());
        aplicarTipo();
        aplicarPaquetes();
        aplicarFechas();
    }

    /** Agrupa opciones excluyentes; siempre queda una elegida (un clic en la elegida no la apaga). */
    private static ToggleGroup grupo(ToggleButton... opciones) {
        ToggleGroup grupo = new ToggleGroup();
        for (ToggleButton opcion : opciones) {
            opcion.setToggleGroup(grupo);
        }
        grupo.selectedToggleProperty().addListener((obs, anterior, nueva) -> {
            if (nueva == null && anterior != null) {
                anterior.setSelected(true);
            }
        });
        return grupo;
    }

    /** Llena el formulario con un descuento guardado. */
    public void setDescuentoAEditar(DescuentoDTO descuento) {
        this.descuentoAEditar = descuento;
        lblTitulo.setText("Editar descuento");
        txtNombre.setText(descuento.getNombre());
        (descuento.getTipo() == TipoDescuento.MONTO_FIJO ? opMontoFijo : opPorcentaje).setSelected(true);
        txtValor.setText(formatoValor(descuento.getValor()));

        List<Long> ids = descuento.getPaqueteIds();
        for (int i = 0; i < ids.size(); i++) {
            // Un paquete dado de baja después sigue en la lista para poder quitarlo.
            if (!paquetes.containsKey(ids.get(i))) {
                agregarPaquete(ids.get(i), descuento.getPaqueteNombres().get(i));
            }
        }
        paquetesElegidos.clear();
        paquetesElegidos.addAll(ids);
        (descuento.isTodosLosPaquetes() ? opTodosPaquetes : opAlgunosPaquetes).setSelected(true);
        aplicarPaquetes();
        cmbEjemplo.getItems().stream()
                .filter(p -> descuento.isTodosLosPaquetes() || ids.contains(p.getId()))
                .findFirst()
                .ifPresent(cmbEjemplo::setValue);

        Set<DayOfWeek> guardados = descuento.getDias();
        dias.forEach((dia, boton) -> boton.setSelected(guardados == null || guardados.isEmpty() || guardados.contains(dia)));

        dpInicio.setValue(descuento.getFechaInicio());
        dpFin.setValue(descuento.getFechaFin());
        dpCotizarHasta.setValue(descuento.getCotizarHasta());
        (descuento.getFechaInicio() != null || descuento.getFechaFin() != null ? opTemporada : opTodoElAnio).setSelected(true);
        (descuento.getCotizarHasta() != null ? opHastaFecha : opSinLimite).setSelected(true);
        aplicarFechas();
    }

    private void agregarPaquete(Long id, String nombre) {
        ToggleButton boton = new ToggleButton(nombre);
        boton.getStyleClass().add("opcion-pastilla");
        boton.selectedProperty().addListener((obs, antes, ahora) -> {
            if (pintandoPaquetes) return;
            if (ahora) paquetesElegidos.add(id); else paquetesElegidos.remove(id);
            actualizarVistaPrevia();
        });
        paquetes.put(id, boton);
        boxPaquetes.getChildren().add(boton);
    }

    private void aplicarTipo() {
        boolean porcentaje = opPorcentaje.isSelected();
        lblUnidadAntes.setVisible(!porcentaje);
        lblUnidadAntes.setManaged(!porcentaje);
        lblUnidadDespues.setVisible(porcentaje);
        lblUnidadDespues.setManaged(porcentaje);
        txtValor.setPromptText(porcentaje ? "15" : "1000");
        actualizarVistaPrevia();
    }

    /**
     * Con "Todos los paquetes" las pastillas se ven todas marcadas y no se pueden tocar;
     * con "Solo algunos" muestran lo que se eligió.
     */
    private void aplicarPaquetes() {
        boolean todos = opTodosPaquetes.isSelected();
        pintandoPaquetes = true;
        try {
            paquetes.forEach((id, boton) -> {
                boton.setSelected(todos || paquetesElegidos.contains(id));
                boton.setDisable(todos);
            });
        } finally {
            pintandoPaquetes = false;
        }
        actualizarVistaPrevia();
    }

    private void aplicarFechas() {
        dpInicio.setDisable(!opTemporada.isSelected());
        dpFin.setDisable(!opTemporada.isSelected());
        dpCotizarHasta.setDisable(!opHastaFecha.isSelected());
        actualizarVistaPrevia();
    }

    @FXML
    private void diasTodos() {
        marcarDias(EnumSet.allOf(DayOfWeek.class));
    }

    @FXML
    private void diasEntreSemana() {
        marcarDias(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.THURSDAY));
    }

    @FXML
    private void diasFinDeSemana() {
        marcarDias(EnumSet.range(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY));
    }

    private void marcarDias(Set<DayOfWeek> elegidos) {
        dias.forEach((dia, boton) -> boton.setSelected(elegidos.contains(dia)));
    }

    /** Ticket de ejemplo, la regla en una frase y lo que falta contestar. */
    private void actualizarVistaPrevia() {
        if (lblResumen == null || cmbEjemplo == null || lblAviso == null) return;
        DescuentoDTO dto = leerFormulario(false);
        List<String> faltan = faltantes(dto);

        lblAviso.setText(faltan.isEmpty() ? "" : "Falta: " + DescripcionDescuento.lista(faltan) + ".");
        lblAviso.setVisible(!faltan.isEmpty());
        lblAviso.setManaged(!faltan.isEmpty());
        boolean valorValido = dto.getValor() > 0
                && !(dto.getTipo() == TipoDescuento.PORCENTAJE && dto.getValor() > 100);
        lblResumen.setText(valorValido && !dto.getDias().isEmpty()
                && (dto.isTodosLosPaquetes() || !dto.getPaqueteIds().isEmpty())
                ? DescripcionDescuento.resumen(dto) : "Conteste las preguntas para ver el resumen.");

        PaqueteDTO ejemplo = cmbEjemplo.getValue();
        if (ejemplo == null) {
            lblEjemploPaquete.setText("Sin paquetes en el catálogo");
            lblEjemploPrecio.setText("");
            lblEjemploTotal.setText("");
            mostrarDescuentoEjemplo(false);
            lblEjemploNota.setText("");
            return;
        }
        double precio = ejemplo.getPrecioDesde();
        boolean aplica = dto.isTodosLosPaquetes() || dto.getPaqueteIds().contains(ejemplo.getId());
        double monto = !valorValido || !aplica ? 0
                : dto.getTipo() == TipoDescuento.MONTO_FIJO
                    ? Math.min(dto.getValor(), precio)
                    : Math.round(precio * dto.getValor()) / 100.0;

        lblEjemploPaquete.setText(ejemplo.getNombre());
        lblEjemploPrecio.setText(dinero(precio));
        mostrarDescuentoEjemplo(monto > 0);
        String nombre = dto.getNombre().isEmpty() ? "Descuento" : "Descuento «" + dto.getNombre() + "»";
        lblEjemploDescuento.setText(nombre);
        lblEjemploMonto.setText("−" + dinero(monto));
        lblEjemploTotal.setText(dinero(precio - monto));

        List<String> notas = new ArrayList<>();
        if (!aplica) {
            notas.add("Este paquete no lleva el descuento.");
        }
        if (ejemplo.isTablaPrecios()) {
            notas.add("Con el precio más bajo de su tabla.");
        }
        lblEjemploNota.setText(String.join(" ", notas));
        lblEjemploNota.setVisible(!notas.isEmpty());
        lblEjemploNota.setManaged(!notas.isEmpty());
    }

    private void mostrarDescuentoEjemplo(boolean visible) {
        boxEjemploDescuento.setVisible(visible);
        boxEjemploDescuento.setManaged(visible);
    }

    /** Lo que falta contestar, en palabras; vacío si todo está completo. */
    private List<String> faltantes(DescuentoDTO dto) {
        List<String> faltan = new ArrayList<>();
        if (dto.getNombre().isEmpty()) {
            faltan.add("el nombre");
        }
        if (dto.getValor() <= 0) {
            faltan.add(dto.getTipo() == TipoDescuento.PORCENTAJE ? "el porcentaje" : "la cantidad");
        } else if (dto.getTipo() == TipoDescuento.PORCENTAJE && dto.getValor() > 100) {
            faltan.add("un porcentaje de 100 o menos");
        }
        if (!dto.isTodosLosPaquetes() && dto.getPaqueteIds().isEmpty()) {
            faltan.add("elegir al menos un paquete");
        }
        if (dto.getDias().isEmpty()) {
            faltan.add("elegir al menos un día");
        }
        if (opTemporada.isSelected() && dpInicio.getValue() == null && dpFin.getValue() == null) {
            faltan.add("las fechas de la temporada");
        }
        if (opHastaFecha.isSelected() && dpCotizarHasta.getValue() == null) {
            faltan.add("el último día para cotizar");
        }
        return faltan;
    }

    /**
     * Arma el DTO con lo contestado. Con {@code confirmarFechas} también toma el texto escrito
     * en las fechas que aún no se confirmó con Enter.
     *
     * @throws DateTimeParseException si {@code confirmarFechas} y una fecha está mal escrita
     */
    private DescuentoDTO leerFormulario(boolean confirmarFechas) {
        DescuentoDTO dto = new DescuentoDTO();
        dto.setNombre(txtNombre.getText() == null ? "" : txtNombre.getText().trim());
        // El interruptor se maneja desde la lista: uno nuevo nace activo y uno editado conserva su estado.
        dto.setActivo(descuentoAEditar == null || descuentoAEditar.isActivo());
        dto.setTipo(opMontoFijo.isSelected() ? TipoDescuento.MONTO_FIJO : TipoDescuento.PORCENTAJE);
        dto.setValor(numero(txtValor.getText()));

        EnumSet<DayOfWeek> elegidos = EnumSet.noneOf(DayOfWeek.class);
        dias.forEach((dia, boton) -> {
            if (boton.isSelected()) elegidos.add(dia);
        });
        dto.setDias(elegidos);

        dto.setTodosLosPaquetes(opTodosPaquetes.isSelected());
        List<Long> ids = new ArrayList<>();
        List<String> nombres = new ArrayList<>();
        if (!dto.isTodosLosPaquetes()) {
            paquetes.forEach((id, boton) -> {
                if (paquetesElegidos.contains(id)) {
                    ids.add(id);
                    nombres.add(boton.getText());
                }
            });
        }
        dto.setPaqueteIds(ids);
        dto.setPaqueteNombres(nombres);

        if (opTemporada.isSelected()) {
            dto.setFechaInicio(confirmarFechas ? confirmar(dpInicio) : dpInicio.getValue());
            dto.setFechaFin(confirmarFechas ? confirmar(dpFin) : dpFin.getValue());
        }
        if (opHastaFecha.isSelected()) {
            dto.setCotizarHasta(confirmarFechas ? confirmar(dpCotizarHasta) : dpCotizarHasta.getValue());
        }
        return dto;
    }

    /**
     * Toma la fecha escrita aunque no se haya confirmado; vacío es "sin fecha".
     *
     * @throws DateTimeParseException si está mal escrita
     */
    private static LocalDate confirmar(DatePicker dp) {
        LocalDate fecha = leerFecha(dp.getEditor().getText());
        dp.setValue(fecha);
        return fecha;
    }

    /** Acepta 01/12/2026 y 1/12/2026. */
    private static LocalDate leerFecha(String texto) {
        return texto == null || texto.isBlank() ? null : LocalDate.parse(texto.trim(), LECTURA_FECHA);
    }

    @FXML
    private void guardar() {
        DescuentoDTO dto;
        try {
            dto = leerFormulario(true);
        } catch (DateTimeParseException e) {
            mostrarError("Escriba las fechas como día/mes/año, por ejemplo 01/12/2026, o elíjalas en el calendario.");
            return;
        }
        List<String> faltan = faltantes(dto);
        if (!faltan.isEmpty()) {
            mostrarError("Falta: " + DescripcionDescuento.lista(faltan) + ".");
            return;
        }
        try {
            guardado = descuentoAEditar == null
                    ? gestionarDescuentoUseCase.crear(dto)
                    : gestionarDescuentoUseCase.actualizar(descuentoAEditar.getId(), dto);
            cerrarModal();
        } catch (CotizacionException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void cerrarModal() {
        ((Stage) txtNombre.getScene().getWindow()).close();
    }

    private static double numero(String texto) {
        if (texto == null || texto.isBlank() || texto.equals(".")) return 0;
        try {
            return Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String dinero(double importe) {
        return String.format("$%,.2f", importe);
    }

    private static String formatoValor(double valor) {
        return valor == Math.rint(valor) ? String.valueOf((long) valor) : String.format(Locale.ROOT, "%.2f", valor);
    }

    private static void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Descuento");
        alert.setHeaderText("Revise el descuento");
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
