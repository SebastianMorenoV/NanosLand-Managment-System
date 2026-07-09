package com.mycompany.presentacion.controllers;

import com.example.negocio.reporte.usecase.ExportarReporteUseCase;
import com.example.negocio.reporte.usecase.GenerarReporteAdeudosUseCase;
import com.mycompany.common.dtos.AdeudoDTO;
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
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ReporteAdeudosController {

    private final GenerarReporteAdeudosUseCase generarReporteAdeudosUseCase;
    private final ExportarReporteUseCase exportarReporteUseCase;

    @FXML private TextField txtBuscarCliente;
    @FXML private Button btnExportar;
    @FXML private Label lblTotalDeuda;
    @FXML private Label lblCantidadDeudores;

    @FXML private TableView<AdeudoDTO> tablaAdeudos;
    @FXML private TableColumn<AdeudoDTO, String> colFolio;
    @FXML private TableColumn<AdeudoDTO, String> colCliente;
    @FXML private TableColumn<AdeudoDTO, String> colTelefono;
    @FXML private TableColumn<AdeudoDTO, LocalDate> colFecha;
    @FXML private TableColumn<AdeudoDTO, Double> colGranTotal;
    @FXML private TableColumn<AdeudoDTO, Double> colPagado;
    @FXML private TableColumn<AdeudoDTO, Double> colDeuda;

    @FXML private Pagination paginacion;

    private static final int ITEMS_POR_PAGINA = 20;
    private final ObservableList<AdeudoDTO> masterData = FXCollections.observableArrayList();
    private final ObservableList<AdeudoDTO> filteredData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (tablaAdeudos != null) {
            tablaAdeudos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        }

        colFolio.setCellValueFactory(new PropertyValueFactory<>("folioCotizacion"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("clienteNombre"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("clienteTelefono"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaEvento"));
        colGranTotal.setCellValueFactory(new PropertyValueFactory<>("granTotal"));
        colPagado.setCellValueFactory(new PropertyValueFactory<>("totalPagado"));
        colDeuda.setCellValueFactory(new PropertyValueFactory<>("saldoPendiente"));

        paginacion.setPageCount(1);
        paginacion.setPageFactory(this::crearPagina);

        txtBuscarCliente.textProperty().addListener((obs, oldVal, newVal) -> filtrarDatos());

        cargarDatos();
    }

    private Node crearPagina(int pageIndex) {
        int fromIndex = pageIndex * ITEMS_POR_PAGINA;
        int toIndex = Math.min(fromIndex + ITEMS_POR_PAGINA, filteredData.size());
        
        if (fromIndex < filteredData.size() && fromIndex <= toIndex) {
            tablaAdeudos.setItems(FXCollections.observableArrayList(filteredData.subList(fromIndex, toIndex)));
        } else {
            tablaAdeudos.setItems(FXCollections.observableArrayList());
        }
        return tablaAdeudos;
    }

    private void cargarDatos() {
        try {
            List<AdeudoDTO> adeudos = generarReporteAdeudosUseCase.generarReporteAdeudos();
            masterData.clear();
            if (adeudos != null) {
                masterData.addAll(adeudos);
            }
            filtrarDatos();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlertaError("Error", "No se pudo cargar el reporte de adeudos.");
        }
    }

    private void filtrarDatos() {
        String filtro = txtBuscarCliente.getText().toLowerCase();
        filteredData.clear();

        double totalDeuda = 0.0;
        for (AdeudoDTO a : masterData) {
            if (filtro.isEmpty() || (a.getClienteNombre() != null && a.getClienteNombre().toLowerCase().contains(filtro))) {
                filteredData.add(a);
                totalDeuda += a.getSaldoPendiente();
            }
        }

        lblCantidadDeudores.setText(String.valueOf(filteredData.size()));
        lblTotalDeuda.setText(String.format("$%,.2f", totalDeuda));

        btnExportar.setDisable(filteredData.isEmpty());

        int pageCount = (int) Math.ceil((double) filteredData.size() / ITEMS_POR_PAGINA);
        paginacion.setPageCount(pageCount == 0 ? 1 : pageCount);
        paginacion.setCurrentPageIndex(0);
        crearPagina(0);
    }

    @FXML
    private void exportarPDF() {
        if (filteredData.isEmpty()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte Adeudos PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF (*.pdf)", "*.pdf"));
        fileChooser.setInitialFileName("Reporte_Adeudos_" + LocalDate.now() + ".pdf");

        Stage stage = (Stage) btnExportar.getScene().getWindow();
        File archivo = fileChooser.showSaveDialog(stage);

        if (archivo != null) {
            try (InputStream logoStream = getClass().getResourceAsStream("/Logo Nanos.png")) {
                exportarReporteUseCase.exportarAdeudosPDF(filteredData, logoStream, archivo.getAbsolutePath());
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Éxito");
                alert.setHeaderText(null);
                alert.setContentText("Reporte exportado correctamente a:\n" + archivo.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
                mostrarAlertaError("Error", "No se pudo exportar el PDF: " + e.getMessage());
            }
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
