package com.mycompany.presentacion.controllers;

import com.example.negocio.logistica.usecase.GenerarReporteLogisticaSemanalUseCase;
import com.example.negocio.reporte.usecase.ExportarReporteUseCase;
import com.mycompany.common.dtos.ReporteLogisticaSemanalDTO;
import com.mycompany.presentacion.config.RegistroErrores;
import com.mycompany.presentacion.utils.Espera;
import java.io.File;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

/**
 * Ventana de logística de la semana: una lista sencilla de qué hay que preparar
 * cada día, pensada para quien no está acostumbrado a usar sistemas.
 */
@Controller
@RequiredArgsConstructor
public class ReporteLogisticaSemanalController {

    private final GenerarReporteLogisticaSemanalUseCase generarReporteUseCase;
    private final ExportarReporteUseCase exportarReporteUseCase;

    @FXML private Label lblSemana;
    @FXML private VBox contenido;
    @FXML private Button btnExportar;
    @FXML private Button btnEstaSemana;

    /** Cualquier día de la semana mostrada. */
    private LocalDate fechaSemana;
    private ReporteLogisticaSemanalDTO reporte;

    @FXML
    public void initialize() {
        // El controlador se reutiliza entre aperturas: siempre empieza en la semana actual.
        mostrarSemana(LocalDate.now());
    }

    @FXML
    private void semanaAnterior() {
        mostrarSemana(fechaSemana.minusWeeks(1));
    }

    @FXML
    private void semanaSiguiente() {
        mostrarSemana(fechaSemana.plusWeeks(1));
    }

    @FXML
    private void semanaActual() {
        mostrarSemana(LocalDate.now());
    }

    private void mostrarSemana(LocalDate fecha) {
        try {
            ReporteLogisticaSemanalDTO nuevo = generarReporteUseCase.generar(fecha);
            fechaSemana = fecha;
            reporte = nuevo;
        } catch (Exception e) {
            e.printStackTrace();
            RegistroErrores.avisar("No se pudo cargar la logística de la semana", e);
            return;
        }
        lblSemana.setText(reporte.getRango());

        // "Volver a esta semana" solo aparece cuando se está viendo otra.
        boolean esEstaSemana = reporte.getInicio()
                .equals(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        btnEstaSemana.setVisible(!esEstaSemana);
        btnEstaSemana.setManaged(!esEstaSemana);

        contenido.getChildren().clear();
        for (ReporteLogisticaSemanalDTO.Dia dia : reporte.getDias()) {
            contenido.getChildren().add(tarjetaDia(dia));
        }
    }

    private VBox tarjetaDia(ReporteLogisticaSemanalDTO.Dia dia) {
        boolean sinEventos = dia.getEventos().isEmpty();
        boolean esHoy = dia.getFecha().equals(LocalDate.now());
        String colorTexto = sinEventos ? "#7f8c8d" : "white";
        String colorFondo = esHoy ? "#1a82b8" : sinEventos ? "#ecf0f1" : "#2c3e50";
        if (esHoy) {
            colorTexto = "white";
        }

        String nombreDia = dia.getNombre();
        Label lblDia = new Label(esHoy
                ? "Hoy, " + nombreDia.substring(0, 1).toLowerCase() + nombreDia.substring(1)
                : nombreDia);
        lblDia.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + colorTexto + ";");
        int n = dia.getEventos().size();
        Label lblCuenta = new Label(sinEventos ? "Sin eventos" : n + (n == 1 ? " evento" : " eventos"));
        lblCuenta.setStyle("-fx-font-size: 13px; -fx-text-fill: " + colorTexto + ";");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox barra = new HBox(lblDia, espacio, lblCuenta);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(9, 14, 9, 14));
        barra.setStyle("-fx-background-color: " + colorFondo + "; -fx-background-radius: 8;");

        VBox tarjeta = new VBox(barra);
        if (sinEventos) {
            return tarjeta;
        }

        VBox cuerpo = new VBox(14);
        cuerpo.setPadding(new Insets(12, 14, 14, 14));
        // Con un solo evento el total repetiría la misma lista.
        if (n > 1) {
            Label total = new Label("Total del día: " + dia.getResumenTexto());
            total.setWrapText(true);
            total.setStyle("-fx-background-color: #ebf5fb; -fx-text-fill: #1a82b8; -fx-font-weight: bold;"
                    + " -fx-font-size: 13px; -fx-padding: 7 10 7 10; -fx-background-radius: 6;");
            total.setMaxWidth(Double.MAX_VALUE);
            cuerpo.getChildren().add(total);
        }
        for (ReporteLogisticaSemanalDTO.EventoLogistica evento : dia.getEventos()) {
            cuerpo.getChildren().add(bloqueEvento(evento));
        }
        tarjeta.getChildren().add(cuerpo);
        tarjeta.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        return tarjeta;
    }

    private VBox bloqueEvento(ReporteLogisticaSemanalDTO.EventoLogistica evento) {
        Label horario = new Label(evento.getHorario());
        horario.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a82b8;");
        Label descripcion = new Label(evento.getDescripcion());
        descripcion.setWrapText(true);
        descripcion.setStyle("-fx-font-size: 13px; -fx-text-fill: #2c3e50;");

        VBox bloque = new VBox(4, horario, descripcion);
        if (evento.getNotas() != null && !evento.getNotas().isBlank()) {
            Label notas = new Label("Importante: " + evento.getNotas().trim());
            notas.setWrapText(true);
            notas.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #c0392b;");
            bloque.getChildren().add(notas);
        }

        VBox lista = new VBox(6);
        lista.setPadding(new Insets(6, 0, 0, 0));
        if (evento.getServicios().isEmpty()) {
            Label sin = new Label("No tiene servicios registrados.");
            sin.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 13px;");
            lista.getChildren().add(sin);
        }
        for (ReporteLogisticaSemanalDTO.Servicio s : evento.getServicios()) {
            lista.getChildren().add(filaServicio(s));
        }
        bloque.getChildren().add(lista);
        return bloque;
    }

    private HBox filaServicio(ReporteLogisticaSemanalDTO.Servicio s) {
        Label casilla = new Label(s.isListo() ? "✓" : "");
        casilla.setAlignment(Pos.CENTER);
        casilla.setMinSize(20, 20);
        casilla.setMaxSize(20, 20);
        casilla.setStyle(s.isListo()
                ? "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4;"
                : "-fx-border-color: #95a5a6; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label nombre = new Label(s.getCantidad() + "  " + s.getEtiqueta());
        nombre.setMinWidth(Region.USE_PREF_SIZE);
        nombre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox fila = new HBox(10, casilla, nombre);
        fila.setAlignment(Pos.CENTER_LEFT);
        if (!s.getDetalle().isEmpty()) {
            Label detalle = new Label(s.getDetalle());
            detalle.setWrapText(true);
            detalle.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");
            HBox.setHgrow(detalle, Priority.ALWAYS);
            fila.getChildren().add(detalle);
        }
        return fila;
    }

    @FXML
    private void exportarPDF() {
        if (reporte == null) {
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar logística de la semana");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF (*.pdf)", "*.pdf"));
        fileChooser.setInitialFileName("Logistica_semana_" + reporte.getInicio() + ".pdf");
        com.mycompany.presentacion.utils.DialogoGuardarPdf.abrirEnDocumentos(fileChooser);

        File archivo = fileChooser.showSaveDialog(btnExportar.getScene().getWindow());
        if (archivo == null) {
            return;
        }
        ReporteLogisticaSemanalDTO datos = reporte;
        Espera.ejecutar(btnExportar, "Generando el PDF…", () -> {
            try (InputStream logoStream = getClass().getResourceAsStream("/Logo Nanos.png")) {
                exportarReporteUseCase.exportarLogisticaSemanalPDF(datos, logoStream, archivo.getAbsolutePath());
            }
            // Se abre de una vez, listo para ver o imprimir.
            try {
                java.awt.Desktop.getDesktop().open(archivo);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }, abierto -> {
            if (!abierto) {
                mostrarAviso(Alert.AlertType.INFORMATION, "El PDF se guardó en:\n" + archivo.getAbsolutePath());
            }
        }, error -> mostrarAviso(Alert.AlertType.ERROR,
                "No se pudo guardar el PDF. Si ese archivo está abierto, ciérralo e intenta de nuevo."));
    }

    private void mostrarAviso(Alert.AlertType tipo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle("Logística de la semana");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    private void cerrar() {
        ((Stage) contenido.getScene().getWindow()).close();
    }
}
