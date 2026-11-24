package com.mycompany.presentacion.controllers;

import com.example.negocio.descuento.usecase.GestionarDescuentoUseCase;
import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.DescuentoDTO;
import com.mycompany.common.util.DescripcionDescuento;
import com.mycompany.presentacion.config.RegistroErrores;
import com.mycompany.presentacion.utils.ModalHelper;
import com.mycompany.presentacion.utils.ViewSwitcher;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * Lista de descuentos automáticos a paquetes, separada en activos, vencidos y
 * desactivados. Solo la ve el dueño (el menú la oculta a los administradores).
 */
@Controller
@Scope("prototype")
@RequiredArgsConstructor
public class DescuentosController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final GestionarDescuentoUseCase gestionarDescuentoUseCase;

    @FXML private ToggleGroup grupoFiltro;
    @FXML private ToggleButton btnActivos;
    @FXML private ToggleButton btnVencidos;
    @FXML private ToggleButton btnDesactivados;
    @FXML private TextField txtBuscar;
    @FXML private TableView<DescuentoDTO> tablaDescuentos;
    @FXML private TableColumn<DescuentoDTO, String> colNombre;
    @FXML private TableColumn<DescuentoDTO, String> colDescuento;
    @FXML private TableColumn<DescuentoDTO, String> colPaquetes;
    @FXML private TableColumn<DescuentoDTO, String> colDias;
    @FXML private TableColumn<DescuentoDTO, String> colTemporada;
    @FXML private TableColumn<DescuentoDTO, String> colCotizarHasta;
    @FXML private Button btnEditar;
    @FXML private Button btnActivar;
    @FXML private Button btnDuplicar;
    @FXML private Button btnEliminar;
    @FXML private Label lblResumen;

    private final ObservableList<DescuentoDTO> todos = FXCollections.observableArrayList();
    private FilteredList<DescuentoDTO> filtrados;
    private final Map<DescuentoDTO.Estado, ToggleButton> pestanas = new EnumMap<>(DescuentoDTO.Estado.class);
    private final Map<DescuentoDTO.Estado, String> nombres = new EnumMap<>(DescuentoDTO.Estado.class);

    @FXML
    public void initialize() {
        registrarPestana(DescuentoDTO.Estado.ACTIVO, btnActivos);
        registrarPestana(DescuentoDTO.Estado.VENCIDO, btnVencidos);
        registrarPestana(DescuentoDTO.Estado.DESACTIVADO, btnDesactivados);

        configurarTabla();
        filtrados = new FilteredList<>(todos, d -> false);
        SortedList<DescuentoDTO> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tablaDescuentos.comparatorProperty());
        tablaDescuentos.setItems(ordenados);

        grupoFiltro.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            // Siempre queda una pestaña elegida.
            if (nuevo == null && anterior != null) {
                anterior.setSelected(true);
                return;
            }
            aplicarFiltro();
        });
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> aplicarFiltro());
        tablaDescuentos.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, nuevo) -> actualizarSeleccion(nuevo));

        btnActivos.setSelected(true);
        cargar(null);
    }

    private void registrarPestana(DescuentoDTO.Estado estado, ToggleButton boton) {
        boton.setUserData(estado);
        pestanas.put(estado, boton);
        nombres.put(estado, boton.getText());
        boton.selectedProperty().addListener((obs, antes, ahora) -> pintarPestana(boton));
        pintarPestana(boton);
    }

    private static void pintarPestana(ToggleButton boton) {
        boton.setStyle((boton.isSelected()
                ? "-fx-background-color: #1a82b8; -fx-text-fill: white;"
                : "-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50;")
                + " -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 14 8 14; -fx-cursor: hand;");
    }

    private void configurarTabla() {
        tablaDescuentos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tablaDescuentos.setRowFactory(tv -> {
            TableRow<DescuentoDTO> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    abrirModal(fila.getItem());
                }
            });
            return fila;
        });
        columna(colNombre, DescuentoDTO::getNombre);
        columna(colDescuento, d -> DescripcionDescuento.valor(d.getTipo(), d.getValor()));
        columna(colPaquetes, DescripcionDescuento::paquetes);
        columna(colDias, d -> DescripcionDescuento.dias(d.getDias()));
        columna(colTemporada, d -> DescripcionDescuento.temporada(d.getFechaInicio(), d.getFechaFin()));
        columna(colCotizarHasta, d -> d.getCotizarHasta() == null ? "Sin límite" : FECHA.format(d.getCotizarHasta()));
    }

    /** Columna de texto; el tooltip muestra el texto completo si no cabe. */
    private static void columna(TableColumn<DescuentoDTO, String> columna, Function<DescuentoDTO, String> texto) {
        columna.setCellValueFactory(c -> new SimpleStringProperty(texto.apply(c.getValue())));
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(vacio ? null : valor);
                setTooltip(vacio || valor == null || valor.isEmpty() ? null : new Tooltip(valor));
            }
        });
    }

    /** Recarga la lista; si se indica un descuento, muestra su pestaña y lo deja elegido. */
    private void cargar(Long seleccionar) {
        List<DescuentoDTO> lista;
        try {
            lista = gestionarDescuentoUseCase.obtenerTodos();
        } catch (RuntimeException e) {
            e.printStackTrace();
            tablaDescuentos.setPlaceholder(new Label("No se pudieron cargar los descuentos."));
            RegistroErrores.avisar("No se pudo cargar la lista de descuentos", e);
            return;
        }
        todos.setAll(lista);
        actualizarConteos();

        DescuentoDTO elegido = seleccionar == null ? null
                : todos.stream().filter(d -> seleccionar.equals(d.getId())).findFirst().orElse(null);
        if (elegido != null) {
            txtBuscar.clear();
            pestanas.get(elegido.getEstado()).setSelected(true);
        }
        aplicarFiltro();
        if (elegido != null) {
            tablaDescuentos.getSelectionModel().select(elegido);
            tablaDescuentos.scrollTo(elegido);
        }
    }

    private void actualizarConteos() {
        Map<DescuentoDTO.Estado, Integer> conteos = new EnumMap<>(DescuentoDTO.Estado.class);
        todos.forEach(d -> conteos.merge(d.getEstado(), 1, Integer::sum));
        pestanas.forEach((estado, boton) ->
                boton.setText(nombres.get(estado) + " (" + conteos.getOrDefault(estado, 0) + ")"));
    }

    private void aplicarFiltro() {
        DescuentoDTO.Estado estado = (DescuentoDTO.Estado) grupoFiltro.getSelectedToggle().getUserData();
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        filtrados.setPredicate(d -> d.getEstado() == estado
                && (texto.isEmpty() || contiene(d.getNombre(), texto)
                    || contiene(DescripcionDescuento.paquetes(d), texto)));
        tablaDescuentos.setPlaceholder(new Label(!texto.isEmpty() ? "No se encontró ningún descuento."
                : estado == DescuentoDTO.Estado.ACTIVO ? "No hay descuentos activos. Cree uno con «+ Nuevo descuento»."
                : "No hay descuentos en esta pestaña."));
        actualizarSeleccion(tablaDescuentos.getSelectionModel().getSelectedItem());
    }

    private static boolean contiene(String valor, String texto) {
        return valor != null && valor.toLowerCase().contains(texto);
    }

    private void actualizarSeleccion(DescuentoDTO elegido) {
        boolean hay = elegido != null;
        btnEditar.setDisable(!hay);
        btnActivar.setDisable(!hay);
        btnEliminar.setDisable(!hay);
        btnDuplicar.setDisable(!hay || (elegido.getFechaInicio() == null && elegido.getFechaFin() == null
                && elegido.getCotizarHasta() == null));
        btnActivar.setText(hay && !elegido.isActivo() ? "Activar" : "Desactivar");
        lblResumen.setText(hay ? DescripcionDescuento.resumen(elegido)
                : "Elija un descuento para ver cómo se aplica.");
    }

    private DescuentoDTO elegido() {
        return tablaDescuentos.getSelectionModel().getSelectedItem();
    }

    @FXML
    private void nuevo() {
        abrirModal(null);
    }

    @FXML
    private void editar() {
        if (elegido() != null) {
            abrirModal(elegido());
        }
    }

    @FXML
    private void cambiarActivo() {
        DescuentoDTO d = elegido();
        if (d == null) return;
        try {
            gestionarDescuentoUseCase.cambiarActivo(d.getId(), !d.isActivo());
            cargar(d.getId());
        } catch (CotizacionException e) {
            mostrarError(e.getMessage());
            cargar(null);
        }
    }

    @FXML
    private void duplicar() {
        DescuentoDTO d = elegido();
        if (d == null) return;
        try {
            DescuentoDTO copia = gestionarDescuentoUseCase.duplicarAlSiguienteAnio(d.getId());
            cargar(copia.getId());
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Descuento creado");
            ok.setHeaderText("Se creó «" + copia.getNombre() + "»");
            ok.setContentText(DescripcionDescuento.resumen(copia) + "\n\nRevíselo y edítelo si algo cambia este año.");
            ok.showAndWait();
        } catch (CotizacionException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    private void eliminar() {
        DescuentoDTO d = elegido();
        if (d == null) return;
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION);
        confirmar.setTitle("Eliminar descuento");
        confirmar.setHeaderText("¿Eliminar el descuento «" + d.getNombre() + "»?");
        confirmar.setContentText("Las cotizaciones que ya lo tienen conservan su precio. "
                + "Si solo quiere pausarlo, use «Desactivar».");
        Optional<ButtonType> respuesta = confirmar.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) return;
        try {
            gestionarDescuentoUseCase.eliminar(d.getId());
        } catch (CotizacionException e) {
            mostrarError(e.getMessage());
        }
        cargar(null);
    }

    private void abrirModal(DescuentoDTO editar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/presentacion/views/DescuentoModal.fxml"));
            loader.setControllerFactory(ViewSwitcher.getSpringContext()::getBean);
            Parent root = loader.load();
            DescuentoModalController controller = loader.getController();
            if (editar != null) {
                controller.setDescuentoAEditar(editar);
            }
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            ModalHelper.mostrarModal(root, editar == null ? "Nuevo descuento" : "Editar descuento", stage);
            if (controller.getGuardado() != null) {
                cargar(controller.getGuardado().getId());
            }
        } catch (IOException e) {
            e.printStackTrace();
            RegistroErrores.avisar("No se pudo abrir la ventana del descuento", e);
        }
    }

    private static void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Descuentos");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
