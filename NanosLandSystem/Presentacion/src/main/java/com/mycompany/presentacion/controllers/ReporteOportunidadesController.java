package com.mycompany.presentacion.controllers;

import com.example.negocio.reporte.usecase.ExportarReporteUseCase;
import com.example.negocio.reporte.usecase.GenerarReporteOportunidadesUseCase;
import com.mycompany.common.dtos.OportunidadDTO;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ReporteOportunidadesController {

    private final GenerarReporteOportunidadesUseCase generarReporteOportunidadesUseCase;
    private final ExportarReporteUseCase exportarReporteUseCase;

    @FXML private ComboBox<Integer> cmbMes;
    @FXML private ComboBox<Integer> cmbAnio;
    @FXML private Button btnGenerar;
    @FXML private Button btnExportar;
    @FXML private Label lblCantidadLeads;

    @FXML private TableView<OportunidadDTO> tablaOportunidades;
    @FXML private TableColumn<OportunidadDTO, String> colCliente;
    @FXML private TableColumn<OportunidadDTO, String> colTelefono;
    @FXML private TableColumn<OportunidadDTO, LocalDate> colFechaPasada;
    @FXML private TableColumn<OportunidadDTO, String> colPaquete;
    @FXML private TableColumn<OportunidadDTO, Double> colGasto;

    @FXML private Pagination paginacion;

    private static final int ITEMS_POR_PAGINA = 20;
    private final ObservableList<OportunidadDTO> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (tablaOportunidades != null) {
            tablaOportunidades.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        }

        colCliente.setCellValueFactory(new PropertyValueFactory<>("clienteNombre"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("clienteTelefono"));
        
        colFechaPasada.setCellValueFactory(new PropertyValueFactory<>("fechaEventoPasado"));
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        colFechaPasada.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(dtf.format(item));
                }
            }
        });
        
        colPaquete.setCellValueFactory(new PropertyValueFactory<>("nombrePaquete"));
        colGasto.setCellValueFactory(new PropertyValueFactory<>("montoGastado"));

        paginacion.setPageCount(1);
        paginacion.setPageFactory(this::crearPagina);

        // Inicializar Combos (Por defecto buscar el mes actual del año pasado)
        LocalDate hoy = LocalDate.now();
        for (int i = 1; i <= 12; i++) {
            cmbMes.getItems().add(i);
        }
        for (int i = hoy.getYear() - 5; i <= hoy.getYear(); i++) {
            cmbAnio.getItems().add(i);
        }
        
        cmbMes.setValue(hoy.getMonthValue());
        cmbAnio.setValue(hoy.getYear() - 1); // Hace 1 año

        generarReporte();
    }

    private Node crearPagina(int pageIndex) {
        int fromIndex = pageIndex * ITEMS_POR_PAGINA;
        int toIndex = Math.min(fromIndex + ITEMS_POR_PAGINA, masterData.size());
        
        if (fromIndex < masterData.size() && fromIndex <= toIndex) {
            tablaOportunidades.setItems(FXCollections.observableArrayList(masterData.subList(fromIndex, toIndex)));
        } else {
            tablaOportunidades.setItems(FXCollections.observableArrayList());
        }
        return tablaOportunidades;
    }

    @FXML
    private void generarReporte() {
        Integer mes = cmbMes.getValue();
        Integer anio = cmbAnio.getValue();

        if (mes == null || anio == null) return;

        try {
            List<OportunidadDTO> oportunidades = generarReporteOportunidadesUseCase.generarReporte(anio, mes);
            masterData.clear();
            if (oportunidades != null) {
                masterData.addAll(oportunidades);
            }
            
            lblCantidadLeads.setText(String.valueOf(masterData.size()));
            
            btnExportar.setDisable(masterData.isEmpty());

            int pageCount = (int) Math.ceil((double) masterData.size() / ITEMS_POR_PAGINA);
            paginacion.setPageCount(pageCount == 0 ? 1 : pageCount);
            paginacion.setCurrentPageIndex(0);
            crearPagina(0);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlertaError("Error", "No se pudo generar el reporte de oportunidades.");
        }
    }

    @FXML
    private void exportarPDF() {
        if (masterData.isEmpty()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte Oportunidades PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF (*.pdf)", "*.pdf"));
        fileChooser.setInitialFileName("Oportunidades_Venta_" + cmbMes.getValue() + "_" + cmbAnio.getValue() + ".pdf");

        Stage stage = (Stage) btnExportar.getScene().getWindow();
        File archivo = fileChooser.showSaveDialog(stage);

        if (archivo != null) {
            try (InputStream logoStream = getClass().getResourceAsStream("/Logo Nanos.png")) {
                String titulo = "Eventos Finalizados en " + getMesNombre(cmbMes.getValue()) + " de " + cmbAnio.getValue();
                exportarReporteUseCase.exportarOportunidadesPDF(masterData, logoStream, archivo.getAbsolutePath(), titulo);
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Éxito");
                alert.setHeaderText(null);
                alert.setContentText("Reporte de oportunidades exportado correctamente a:\n" + archivo.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
                mostrarAlertaError("Error", "No se pudo exportar el PDF: " + e.getMessage());
            }
        }
    }
    
    private String getMesNombre(int mes) {
        String[] meses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        if (mes >= 1 && mes <= 12) return meses[mes-1];
        return "";
    }

    private void mostrarAlertaError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
