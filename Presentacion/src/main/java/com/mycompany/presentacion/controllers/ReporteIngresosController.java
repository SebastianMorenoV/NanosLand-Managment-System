package com.mycompany.presentacion.controllers;

import com.example.negocio.reporte.usecase.ExportarReporteUseCase;
import com.example.negocio.reporte.usecase.GenerarReporteIngresosUseCase;
import com.mycompany.common.dtos.IngresoDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.mycompany.presentacion.utils.Espera;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ReporteIngresosController {

    private final GenerarReporteIngresosUseCase generarReporteIngresosUseCase;
    private final ExportarReporteUseCase exportarReporteUseCase;

    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFin;
    @FXML private Button btnGenerar;
    @FXML private Button btnExportar;
    @FXML private Label lblIngresoTotal;
    @FXML private Label lblIngresoEventos;
    @FXML private Label lblIngresoVentas;
    @FXML private Label lblVentasProducto;

    @FXML private TableView<IngresoDTO> tablaIngresos;
    @FXML private TableColumn<IngresoDTO, LocalDateTime> colFecha;
    @FXML private TableColumn<IngresoDTO, String> colFolioPago;
    @FXML private TableColumn<IngresoDTO, String> colOrigen;
    @FXML private TableColumn<IngresoDTO, String> colCotizacion;
    @FXML private TableColumn<IngresoDTO, String> colCliente;
    @FXML private TableColumn<IngresoDTO, String> colMetodo;
    @FXML private TableColumn<IngresoDTO, Double> colCantidad;

    @FXML private Pagination paginacion;

    private static final int ITEMS_POR_PAGINA = 20;
    private final ObservableList<IngresoDTO> masterData = FXCollections.observableArrayList();
    private List<com.mycompany.common.dtos.VentaProductoDTO> ventasPorProducto = List.of();

    @FXML
    public void initialize() {
        // El botón del menú solo se muestra al dueño; se comprueba aquí también para que
        // la pantalla no dependa de que el botón esté oculto.
        if (!com.mycompany.presentacion.utils.ViewSwitcher.getSpringContext()
                .getBean(com.mycompany.presentacion.context.SesionContext.class).esDueno()) {
            throw new IllegalStateException("Esta pantalla solo está disponible para el dueño.");
        }
        if (tablaIngresos != null) {
            tablaIngresos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        }

        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colFecha.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(dtf.format(item));
                }
            }
        });

        colFolioPago.setCellValueFactory(new PropertyValueFactory<>("folioPago"));
        colOrigen.setCellValueFactory(new PropertyValueFactory<>("origen"));
        colCotizacion.setCellValueFactory(new PropertyValueFactory<>("folioCotizacion"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("clienteNombre"));
        colMetodo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colCantidad.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("$%,.2f", item));
                }
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });

        paginacion.setPageCount(1);
        paginacion.setPageFactory(this::crearPagina);

        // Por defecto, mostrar los ingresos del mes actual
        dpInicio.setValue(LocalDate.now().withDayOfMonth(1));
        dpFin.setValue(LocalDate.now());

        generarReporte();
    }

    private Node crearPagina(int pageIndex) {
        int fromIndex = pageIndex * ITEMS_POR_PAGINA;
        int toIndex = Math.min(fromIndex + ITEMS_POR_PAGINA, masterData.size());
        
        if (fromIndex < masterData.size() && fromIndex <= toIndex) {
            tablaIngresos.setItems(FXCollections.observableArrayList(masterData.subList(fromIndex, toIndex)));
        } else {
            tablaIngresos.setItems(FXCollections.observableArrayList());
        }
        return tablaIngresos;
    }

    @FXML
    private void generarReporte() {
        LocalDate inicio = dpInicio.getValue();
        LocalDate fin = dpFin.getValue();

        try {
            List<IngresoDTO> ingresos = generarReporteIngresosUseCase.generarReporteIngresos(inicio, fin);
            masterData.clear();
            if (ingresos != null) {
                masterData.addAll(ingresos);
            }
            
            double total = masterData.stream().mapToDouble(IngresoDTO::getCantidad).sum();
            lblIngresoTotal.setText(String.format("$%,.2f", total));

            // Las ventas extra se muestran aparte: no son ganancia de ningún evento.
            double ventasExtra = masterData.stream().filter(IngresoDTO::isVentaExtra)
                    .mapToDouble(IngresoDTO::getCantidad).sum();
            lblIngresoEventos.setText(String.format("$%,.2f", total - ventasExtra));
            lblIngresoVentas.setText(String.format("$%,.2f", ventasExtra));
            ventasPorProducto = generarReporteIngresosUseCase.generarVentasPorProducto(inicio, fin);
            String detalleVentas = ventasPorProducto.stream()
                    .map(v -> v.getCantidad() + " " + v.getNombre() + String.format(" ($%,.2f)", v.getTotal()))
                    .collect(java.util.stream.Collectors.joining(", "));
            lblVentasProducto.setText("Vendido: " + detalleVentas);
            lblVentasProducto.setVisible(!ventasPorProducto.isEmpty());
            lblVentasProducto.setManaged(!ventasPorProducto.isEmpty());
            
            btnExportar.setDisable(masterData.isEmpty());

            int pageCount = (int) Math.ceil((double) masterData.size() / ITEMS_POR_PAGINA);
            paginacion.setPageCount(pageCount == 0 ? 1 : pageCount);
            paginacion.setCurrentPageIndex(0);
            crearPagina(0);

        } catch (com.example.negocio.exception.CotizacionException e) {
            // Dato inválido (por ejemplo, fecha de inicio posterior a la final): se explica al usuario.
            mostrarAlertaError("Fechas inválidas", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlertaError("Error", "No se pudo generar el reporte de ingresos.");
        }
    }

    @FXML
    private void exportarPDF() {
        if (masterData.isEmpty()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte de Ingresos PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF (*.pdf)", "*.pdf"));
        fileChooser.setInitialFileName("Reporte_Ingresos_" + LocalDate.now() + ".pdf");

        Stage stage = (Stage) btnExportar.getScene().getWindow();
        File archivo = fileChooser.showSaveDialog(stage);

        if (archivo != null) {
            List<IngresoDTO> datos = new ArrayList<>(masterData);
            List<com.mycompany.common.dtos.VentaProductoDTO> ventas = new ArrayList<>(ventasPorProducto);
            LocalDate inicio = dpInicio.getValue();
            LocalDate fin = dpFin.getValue();
            Espera.ejecutar(btnExportar, "Generando el PDF…", () -> {
                try (InputStream logoStream = getClass().getResourceAsStream("/Logo Nanos.png")) {
                    exportarReporteUseCase.exportarIngresosPDF(datos, ventas, logoStream, archivo.getAbsolutePath(), inicio, fin);
                }
                return null;
            }, sinResultado -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Éxito");
                alert.setHeaderText(null);
                alert.setContentText("Reporte de ingresos exportado correctamente a:\n" + archivo.getAbsolutePath());
                alert.showAndWait();
            }, error -> mostrarAlertaError("Error", "No se pudo exportar el PDF: " + error.getMessage()));
        }
    }

    private void mostrarAlertaError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
