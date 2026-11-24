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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * Alta y edición de un descuento: tipo y valor, paquetes, días de la semana, temporada
 * y fecha límite para cotizar. Muestra en una frase cómo quedará la regla. Una instancia
 * por ventana.
 */
@Controller
@Scope("prototype")
@RequiredArgsConstructor
public class DescuentoModalController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter LECTURA_FECHA = DateTimeFormatter.ofPattern("d/M/uuuu");
    /** Precio de ejemplo para mostrar en el resumen cuánto queda un paquete. */
    private static final double PRECIO_EJEMPLO = 10_000;

    private final GestionarDescuentoUseCase gestionarDescuentoUseCase;
    private final ConsultarPaquetesUseCase consultarPaquetesUseCase;

    @FXML private Label lblTitulo;
    @FXML private TextField txtNombre;
    @FXML private CheckBox chkActivo;
    @FXML private RadioButton rbPorcentaje;
    @FXML private RadioButton rbMontoFijo;
    @FXML private Label lblValor;
    @FXML private TextField txtValor;
    @FXML private RadioButton rbTodosPaquetes;
    @FXML private RadioButton rbAlgunosPaquetes;
    @FXML private FlowPane boxPaquetes;
    @FXML private HBox boxDias;
    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFin;
    @FXML private DatePicker dpCotizarHasta;
    @FXML private Label lblResumen;

    private DescuentoDTO descuentoAEditar;
    /** El descuento guardado al cerrar, o {@code null} si se canceló. */
    @Getter
    private DescuentoDTO guardado;

    private final Map<DayOfWeek, CheckBox> dias = new EnumMap<>(DayOfWeek.class);
    /** Casilla y nombre de cada paquete que se puede elegir, por id. */
    private final Map<Long, CheckBox> paquetes = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        txtValor.setTextFormatter(new TextFormatter<>(change ->
            change.getControlNewText().matches("\\d{0,7}(\\.\\d{0,2})?") ? change : null));

        ToggleGroup grupoTipo = new ToggleGroup();
        rbPorcentaje.setToggleGroup(grupoTipo);
        rbMontoFijo.setToggleGroup(grupoTipo);
        grupoTipo.selectedToggleProperty().addListener((obs, anterior, nuevo) -> aplicarTipo());

        ToggleGroup grupoPaquetes = new ToggleGroup();
        rbTodosPaquetes.setToggleGroup(grupoPaquetes);
        rbAlgunosPaquetes.setToggleGroup(grupoPaquetes);
        grupoPaquetes.selectedToggleProperty().addListener((obs, anterior, nuevo) -> aplicarPaquetes());

        for (DayOfWeek dia : DayOfWeek.values()) {
            CheckBox casilla = new CheckBox(DescripcionDescuento.nombreCortoDia(dia));
            casilla.setSelected(true);
            casilla.selectedProperty().addListener((obs, antes, ahora) -> actualizarResumen());
            dias.put(dia, casilla);
            boxDias.getChildren().add(casilla);
        }

        List<PaqueteDTO> activos = new ArrayList<>(consultarPaquetesUseCase.obtenerPaquetesActivos());
        activos.sort(Comparator.comparing(PaqueteDTO::getNombre, String.CASE_INSENSITIVE_ORDER));
        activos.forEach(p -> agregarPaquete(p.getId(), p.getNombre()));

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
            dp.valueProperty().addListener((obs, antes, ahora) -> actualizarResumen());
        }

        txtNombre.textProperty().addListener((obs, antes, ahora) -> actualizarResumen());
        txtValor.textProperty().addListener((obs, antes, ahora) -> actualizarResumen());
        aplicarTipo();
        aplicarPaquetes();
    }

    /** Llena el formulario con un descuento guardado. */
    public void setDescuentoAEditar(DescuentoDTO descuento) {
        this.descuentoAEditar = descuento;
        lblTitulo.setText("Editar descuento");
        txtNombre.setText(descuento.getNombre());
        chkActivo.setSelected(descuento.isActivo());
        (descuento.getTipo() == TipoDescuento.MONTO_FIJO ? rbMontoFijo : rbPorcentaje).setSelected(true);
        txtValor.setText(formatoValor(descuento.getValor()));

        (descuento.isTodosLosPaquetes() ? rbTodosPaquetes : rbAlgunosPaquetes).setSelected(true);
        List<Long> ids = descuento.getPaqueteIds();
        for (int i = 0; i < ids.size(); i++) {
            // Un paquete dado de baja después sigue en la lista para poder quitarlo.
            if (!paquetes.containsKey(ids.get(i))) {
                agregarPaquete(ids.get(i), descuento.getPaqueteNombres().get(i));
            }
            paquetes.get(ids.get(i)).setSelected(true);
        }

        Set<DayOfWeek> guardados = descuento.getDias();
        dias.forEach((dia, casilla) -> casilla.setSelected(guardados == null || guardados.isEmpty() || guardados.contains(dia)));
        dpInicio.setValue(descuento.getFechaInicio());
        dpFin.setValue(descuento.getFechaFin());
        dpCotizarHasta.setValue(descuento.getCotizarHasta());
        aplicarPaquetes();
    }

    private void agregarPaquete(Long id, String nombre) {
        CheckBox casilla = new CheckBox(nombre);
        casilla.selectedProperty().addListener((obs, antes, ahora) -> actualizarResumen());
        paquetes.put(id, casilla);
        boxPaquetes.getChildren().add(casilla);
    }

    private void aplicarTipo() {
        boolean porcentaje = rbPorcentaje.isSelected();
        lblValor.setText(porcentaje ? "Porcentaje" : "Monto en pesos");
        txtValor.setPromptText(porcentaje ? "Ej: 15" : "Ej: 1000.00");
        actualizarResumen();
    }

    private void aplicarPaquetes() {
        boxPaquetes.setDisable(!rbAlgunosPaquetes.isSelected());
        actualizarResumen();
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
        dias.forEach((dia, casilla) -> casilla.setSelected(elegidos.contains(dia)));
    }

    /** La regla tal como está capturada, en una frase, con un ejemplo de cuánto queda un paquete. */
    private void actualizarResumen() {
        if (lblResumen == null || txtValor == null) return;
        DescuentoDTO dto = leerFormulario(false);
        String aviso = aviso(dto);
        if (aviso != null) {
            lblResumen.setText(aviso);
            return;
        }
        double monto = dto.getTipo() == TipoDescuento.MONTO_FIJO
                ? Math.min(dto.getValor(), PRECIO_EJEMPLO)
                : PRECIO_EJEMPLO * dto.getValor() / 100.0;
        lblResumen.setText(DescripcionDescuento.resumen(dto) + String.format(
                "%nEjemplo: un paquete de $%,.2f queda en $%,.2f.", PRECIO_EJEMPLO, PRECIO_EJEMPLO - monto));
    }

    /** Lo que falta capturar para que la regla tenga sentido, o {@code null} si está completa. */
    private String aviso(DescuentoDTO dto) {
        if (dto.getValor() <= 0) {
            return dto.getTipo() == TipoDescuento.PORCENTAJE
                    ? "Escriba el porcentaje de descuento." : "Escriba el monto de descuento.";
        }
        if (dto.getTipo() == TipoDescuento.PORCENTAJE && dto.getValor() > 100) {
            return "El porcentaje no puede pasar de 100.";
        }
        if (dto.getDias().isEmpty()) {
            return "Elija al menos un día de la semana.";
        }
        if (!dto.isTodosLosPaquetes() && dto.getPaqueteIds().isEmpty()) {
            return "Elija al menos un paquete.";
        }
        return null;
    }

    /**
     * Arma el DTO con lo capturado. Con {@code confirmarFechas} también toma el texto escrito
     * en las fechas que aún no se confirmó con Enter.
     *
     * @throws DateTimeParseException si {@code confirmarFechas} y una fecha está mal escrita
     */
    private DescuentoDTO leerFormulario(boolean confirmarFechas) {
        DescuentoDTO dto = new DescuentoDTO();
        dto.setNombre(txtNombre.getText() == null ? "" : txtNombre.getText().trim());
        dto.setActivo(chkActivo.isSelected());
        dto.setTipo(rbMontoFijo.isSelected() ? TipoDescuento.MONTO_FIJO : TipoDescuento.PORCENTAJE);
        dto.setValor(numero(txtValor.getText()));

        EnumSet<DayOfWeek> elegidos = EnumSet.noneOf(DayOfWeek.class);
        dias.forEach((dia, casilla) -> {
            if (casilla.isSelected()) elegidos.add(dia);
        });
        dto.setDias(elegidos);

        dto.setTodosLosPaquetes(rbTodosPaquetes.isSelected());
        List<Long> ids = new ArrayList<>();
        List<String> nombres = new ArrayList<>();
        if (!dto.isTodosLosPaquetes()) {
            paquetes.forEach((id, casilla) -> {
                if (casilla.isSelected()) {
                    ids.add(id);
                    nombres.add(casilla.getText());
                }
            });
        }
        dto.setPaqueteIds(ids);
        dto.setPaqueteNombres(nombres);

        dto.setFechaInicio(confirmarFechas ? confirmar(dpInicio) : dpInicio.getValue());
        dto.setFechaFin(confirmarFechas ? confirmar(dpFin) : dpFin.getValue());
        dto.setCotizarHasta(confirmarFechas ? confirmar(dpCotizarHasta) : dpCotizarHasta.getValue());
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
            mostrarError("Escriba las fechas como día/mes/año, por ejemplo 01/12/2026, o déjelas vacías.");
            return;
        }
        if (dto.getNombre().isEmpty()) {
            mostrarError("Escriba un nombre para el descuento.");
            return;
        }
        String aviso = aviso(dto);
        if (aviso != null) {
            mostrarError(aviso);
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

    private static String formatoValor(double valor) {
        return valor == Math.rint(valor) ? String.valueOf((long) valor) : String.format(Locale.ROOT, "%.2f", valor);
    }

    private static void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Descuento");
        alert.setHeaderText("Revise los datos");
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
