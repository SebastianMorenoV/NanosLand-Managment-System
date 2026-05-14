package com.mycompany.presentacion.controllers;

import com.example.negocio.reporte.usecase.GenerarReporteEventosUseCase;
import com.mycompany.common.dtos.EventoDTO;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.TurnoEvento;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
public class ReportesController {

    private final GenerarReporteEventosUseCase generarReporteEventosUseCase;
    private final com.example.negocio.reporte.usecase.ExportarReporteUseCase exportarReporteUseCase;

    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFin;
    @FXML private ComboBox<TurnoEvento> cmbTurno;
    @FXML private ComboBox<EstadoCotizacion> cmbEstado;
    @FXML private Button btnExportar;
    
    @FXML private TableView<EventoDTO> tablaReporte;
    @FXML private TableColumn<EventoDTO, LocalDate> colFecha;
    @FXML private TableColumn<EventoDTO, TurnoEvento> colTurno;
    @FXML private TableColumn<EventoDTO, String> colCliente;
    @FXML private TableColumn<EventoDTO, String> colPaquete;
    @FXML private TableColumn<EventoDTO, EstadoCotizacion> colEstado;
    @FXML private TableColumn<EventoDTO, Double> colTotal;

    private final ObservableList<EventoDTO> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (tablaReporte != null) {
            tablaReporte.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        }

        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colTurno.setCellValueFactory(new PropertyValueFactory<>("turno"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("clienteNombre"));
        colPaquete.setCellValueFactory(new PropertyValueFactory<>("paqueteNombre"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoCotizacion"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalCotizacion"));

        cmbTurno.setItems(FXCollections.observableArrayList(TurnoEvento.values()));
        cmbEstado.setItems(FXCollections.observableArrayList(EstadoCotizacion.values()));

        tablaReporte.setItems(masterData);
    }

    @FXML
    private void generarReporte() {
        LocalDate inicio = dpInicio.getValue();
        LocalDate fin = dpFin.getValue();
        TurnoEvento turno = cmbTurno.getValue();
        EstadoCotizacion estado = cmbEstado.getValue();

        try {
            List<EventoDTO> resultados = generarReporteEventosUseCase.generarReporte(inicio, fin, turno, estado);
            masterData.clear();
            if (resultados != null) {
                masterData.addAll(resultados);
            }
            
            btnExportar.setDisable(masterData.isEmpty());
            
            if (masterData.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Reporte");
                alert.setHeaderText(null);
                alert.setContentText("No se encontraron eventos para los filtros seleccionados.");
                alert.showAndWait();
            }
        } catch (IllegalArgumentException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Filtro Inválido");
            alert.setHeaderText(null);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se pudo generar el reporte");
            alert.setContentText("Ocurrió un error inesperado. Consulte los registros del sistema.");
            alert.showAndWait();
        }
    }

    @FXML
    private void exportarPDF() {
        if (masterData.isEmpty()) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF (*.pdf)", "*.pdf"));
        fileChooser.setInitialFileName("Reporte_Eventos_" + LocalDate.now() + ".pdf");

        Stage stage = (Stage) btnExportar.getScene().getWindow();
        File archivo = fileChooser.showSaveDialog(stage);

        if (archivo != null) {
            try {
                InputStream reporteStream = getClass().getResourceAsStream("/com/mycompany/presentacion/reports/ReporteEventos.jrxml");
                exportarReporteUseCase.exportarPDF(masterData, reporteStream, archivo.getAbsolutePath(), dpInicio.getValue(), dpFin.getValue());
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Éxito");
                alert.setHeaderText(null);
                alert.setContentText("Reporte exportado correctamente a:\n" + archivo.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("No se pudo exportar el PDF");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
    }
}
