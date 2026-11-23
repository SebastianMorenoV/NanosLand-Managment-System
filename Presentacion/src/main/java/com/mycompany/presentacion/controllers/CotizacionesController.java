package com.mycompany.presentacion.controllers;

import com.example.negocio.cotizacion.usecase.ListarCotizacionesUseCase;
import com.mycompany.common.dtos.CotizacionDTO;
import com.mycompany.presentacion.config.RegistroErrores;
import com.mycompany.presentacion.context.CotizacionContext;
import com.mycompany.presentacion.context.FiltroCotizaciones;
import com.mycompany.presentacion.utils.ViewSwitcher;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * Lista de cotizaciones, separada del formulario de captura. Una cotización se
 * abre desde aquí para editarla, registrar pagos o generar el contrato.
 */
@Controller
@Scope("prototype")
@RequiredArgsConstructor
public class CotizacionesController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ListarCotizacionesUseCase listarCotizacionesUseCase;
    private final CotizacionContext cotizacionContext;

    @FXML private ToggleGroup grupoFiltro;
    @FXML private ToggleButton btnPorConfirmar;
    @FXML private ToggleButton btnConfirmadas;
    @FXML private ToggleButton btnVencidas;
    @FXML private ToggleButton btnCanceladas;
    @FXML private TextField txtBuscar;
    @FXML private TableView<CotizacionDTO> tablaCotizaciones;
    @FXML private TableColumn<CotizacionDTO, LocalDate> colFecha;
    @FXML private TableColumn<CotizacionDTO, String> colTurno;
    @FXML private TableColumn<CotizacionDTO, String> colFolio;
    @FXML private TableColumn<CotizacionDTO, String> colCliente;
    @FXML private TableColumn<CotizacionDTO, String> colFestejado;
    @FXML private TableColumn<CotizacionDTO, Double> colTotal;
    @FXML private TableColumn<CotizacionDTO, Void> colAccion;

    private final ObservableList<CotizacionDTO> todas = FXCollections.observableArrayList();
    private FilteredList<CotizacionDTO> filtradas;
    private final Map<FiltroCotizaciones, ToggleButton> pestanas = new EnumMap<>(FiltroCotizaciones.class);
    private final Map<FiltroCotizaciones, String> nombres = new EnumMap<>(FiltroCotizaciones.class);

    @FXML
    public void initialize() {
        registrarPestana(FiltroCotizaciones.POR_CONFIRMAR, btnPorConfirmar);
        registrarPestana(FiltroCotizaciones.CONFIRMADAS, btnConfirmadas);
        registrarPestana(FiltroCotizaciones.VENCIDAS, btnVencidas);
        registrarPestana(FiltroCotizaciones.CANCELADAS, btnCanceladas);

        configurarTabla();

        filtradas = new FilteredList<>(todas, c -> false);
        SortedList<CotizacionDTO> ordenadas = new SortedList<>(filtradas);
        ordenadas.comparatorProperty().bind(tablaCotizaciones.comparatorProperty());
        tablaCotizaciones.setItems(ordenadas);

        grupoFiltro.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            // Siempre queda una pestaña elegida.
            if (nuevo == null && anterior != null) {
                anterior.setSelected(true);
                return;
            }
            aplicarFiltro();
        });
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> aplicarFiltro());

        // Al volver de guardar una cotización se muestra la pestaña donde quedó.
        pestanas.get(cotizacionContext.getFiltroLista()).setSelected(true);
        cotizacionContext.setFiltroLista(FiltroCotizaciones.POR_CONFIRMAR);

        tablaCotizaciones.setPlaceholder(new Label("Cargando..."));
        cargarCotizaciones();
    }

    private void registrarPestana(FiltroCotizaciones filtro, ToggleButton boton) {
        boton.setUserData(filtro);
        pestanas.put(filtro, boton);
        nombres.put(filtro, boton.getText());
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
        tablaCotizaciones.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tablaCotizaciones.setRowFactory(tv -> {
            TableRow<CotizacionDTO> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    abrir(fila.getItem());
                }
            });
            return fila;
        });

        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFecha.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate fecha, boolean vacio) {
                super.updateItem(fecha, vacio);
                setText(vacio || fecha == null ? null : FECHA.format(fecha));
                setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            }
        });
        colTurno.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTurno() == null ? ""
                : c.getValue().getTurno().name().charAt(0) + c.getValue().getTurno().name().substring(1).toLowerCase()));
        colFolio.setCellValueFactory(new PropertyValueFactory<>("folio"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
        colFestejado.setCellValueFactory(c -> {
            CotizacionDTO dto = c.getValue();
            String festejado = dto.getNombreFestejado() != null ? dto.getNombreFestejado() : "";
            String tematica = dto.getTematica() != null && !dto.getTematica().isBlank()
                    ? " (" + dto.getTematica() + ")" : "";
            return new SimpleStringProperty(festejado + tematica);
        });
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(Double total, boolean vacio) {
                super.updateItem(total, vacio);
                setText(vacio || total == null ? null : String.format("$%,.2f", total));
                setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            }
        });
        colAccion.setCellFactory(columna -> new TableCell<>() {
            private final Button abrir = new Button("Abrir");

            {
                abrir.getStyleClass().add("boton-azul-claro");
                abrir.setStyle("-fx-font-size: 11px; -fx-padding: 4 12 4 12;");
                abrir.setOnAction(e -> abrir(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean vacio) {
                super.updateItem(item, vacio);
                if (vacio) {
                    setGraphic(null);
                } else {
                    HBox contenedor = new HBox(abrir);
                    contenedor.setAlignment(Pos.CENTER);
                    setGraphic(contenedor);
                }
            }
        });
    }

    private void cargarCotizaciones() {
        Task<List<CotizacionDTO>> tarea = new Task<>() {
            @Override
            protected List<CotizacionDTO> call() {
                return listarCotizacionesUseCase.obtenerCotizaciones();
            }
        };
        tarea.setOnSucceeded(e -> {
            List<CotizacionDTO> lista = new ArrayList<>(tarea.getValue());
            lista.sort(CotizacionesController::compararPorCercania);
            todas.setAll(lista);
            actualizarConteos();
            aplicarFiltro();
        });
        tarea.setOnFailed(e -> {
            tarea.getException().printStackTrace();
            tablaCotizaciones.setPlaceholder(new Label("No se pudieron cargar las cotizaciones."));
            RegistroErrores.avisar("No se pudo cargar la lista de cotizaciones", tarea.getException());
        });
        Thread hilo = new Thread(tarea, "hilo-carga-cotizaciones");
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Primero lo que viene (lo más cercano arriba) y después lo pasado (lo más reciente arriba). */
    private static int compararPorCercania(CotizacionDTO a, CotizacionDTO b) {
        LocalDate hoy = LocalDate.now();
        LocalDate fa = a.getFecha() != null ? a.getFecha() : LocalDate.MIN;
        LocalDate fb = b.getFecha() != null ? b.getFecha() : LocalDate.MIN;
        boolean aFutura = !fa.isBefore(hoy);
        boolean bFutura = !fb.isBefore(hoy);
        if (aFutura != bFutura) {
            return aFutura ? -1 : 1;
        }
        return aFutura ? fa.compareTo(fb) : fb.compareTo(fa);
    }

    private void actualizarConteos() {
        Map<FiltroCotizaciones, Integer> conteos = new EnumMap<>(FiltroCotizaciones.class);
        for (CotizacionDTO c : todas) {
            conteos.merge(FiltroCotizaciones.de(c.getEstado(), c.getFecha()), 1, Integer::sum);
        }
        pestanas.forEach((filtro, boton) ->
                boton.setText(nombres.get(filtro) + " (" + conteos.getOrDefault(filtro, 0) + ")"));
    }

    private void aplicarFiltro() {
        FiltroCotizaciones filtro = (FiltroCotizaciones) grupoFiltro.getSelectedToggle().getUserData();
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        filtradas.setPredicate(c -> FiltroCotizaciones.de(c.getEstado(), c.getFecha()) == filtro
                && (texto.isEmpty() || contiene(c.getFolio(), texto) || contiene(c.getNombreCliente(), texto)
                    || contiene(c.getNombreFestejado(), texto) || contiene(c.getTematica(), texto)));
        tablaCotizaciones.setPlaceholder(new Label(texto.isEmpty()
                ? "No hay cotizaciones en esta pestaña." : "No se encontró ninguna cotización."));
    }

    private static boolean contiene(String valor, String texto) {
        return valor != null && valor.toLowerCase().contains(texto);
    }

    private void abrir(CotizacionDTO cotizacion) {
        if (cotizacion == null) {
            return;
        }
        cotizacionContext.abrir(cotizacion.getId(), "Cotizaciones.fxml");
        ViewSwitcher.cargarVista("Cotizacion.fxml");
    }

    @FXML
    private void nuevaCotizacion() {
        ViewSwitcher.cargarVista("SeleccionarFecha.fxml");
    }
}
