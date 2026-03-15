package com.mycompany.presentacion.controllers;


import com.example.negocio.agenda.usecase.ConsultarAgendaUseCase;
import com.mycompany.persistencia.dominio.Evento;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import com.mycompany.presentacion.utils.ViewSwitcher;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class SeleccionarFechaController {

    private final ConsultarAgendaUseCase consultarAgendaUseCase;

    @FXML private GridPane gridCalendario;
    @FXML private Label lblMesAnio;
    @FXML private Label lblFechaSeleccionada;
    @FXML private Button btnContinuar;

    private YearMonth mesActual = YearMonth.now();
    private LocalDate fechaSeleccionada = null;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            javafx.scene.Parent nodo = gridCalendario.getParent();
            int nivel = 0;
            while (nodo != null) {
                System.out.println("Nivel " + nivel + ": " + nodo.getClass().getSimpleName());
                if (nodo instanceof BorderPane rootPane) {
                    ViewSwitcher.setContenedorPrincipal(rootPane);
                    break;
                }
                nodo = nodo.getParent();
                nivel++;
            }
        });
        renderizarCalendario();
    }
    @FXML
    private void mesAnterior() {
        mesActual = mesActual.minusMonths(1);
        renderizarCalendario();
    }

    @FXML
    private void mesSiguiente() {
        mesActual = mesActual.plusMonths(1);
        renderizarCalendario();
    }

    private void renderizarCalendario() {
        String nombreMes = mesActual.getMonth()
                .getDisplayName(TextStyle.FULL, new Locale("es", "MX"));
        String titulo = nombreMes.substring(0, 1).toUpperCase()
                + nombreMes.substring(1).toLowerCase()
                + " " + mesActual.getYear();
        lblMesAnio.setText(titulo);

        gridCalendario.getChildren().removeIf(node -> {
            Integer rowIndex = GridPane.getRowIndex(node);
            return rowIndex != null && rowIndex > 0;
        });

        gridCalendario.getRowConstraints().clear();

        RowConstraints headerRow = new RowConstraints();
        headerRow.setVgrow(Priority.NEVER);
        gridCalendario.getRowConstraints().add(headerRow);

        List<Evento> eventos = consultarAgendaUseCase
                .obtenerMesCompleto(mesActual.getYear(), mesActual.getMonthValue());

        int primerDia = mesActual.atDay(1).getDayOfWeek().getValue();
        int totalDias = mesActual.lengthOfMonth();

        int col = primerDia - 1;
        int row = 1;
        int ultimaFila = 1;

        for (int dia = 1; dia <= totalDias; dia++) {
            LocalDate fecha = mesActual.atDay(dia);
            VBox celda = crearCelda(fecha, eventos);
            gridCalendario.add(celda, col, row);
            ultimaFila = row;
            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }

        for (int i = 0; i <= ultimaFila; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setFillHeight(true);
            gridCalendario.getRowConstraints().add(rc);
        }
    }

    private VBox crearCelda(LocalDate fecha, List<Evento> eventos) {
        long eventosDelDia = eventos.stream()
                .filter(e -> e.getFechaHoraInicio().toLocalDate().equals(fecha))
                .count();

        boolean esHoy = fecha.equals(LocalDate.now());
        boolean esPasado = fecha.isBefore(LocalDate.now());

        String estiloNumero;
        String estiloDisponibilidad;
        String textoDisponibilidad;
        String estiloCelda;

        if (esPasado) {
            estiloCelda = "celda-dia-llena";
            estiloNumero = "numero-dia-lleno";
            estiloDisponibilidad = "disponibilidad-lleno";
            textoDisponibilidad = "No disponible";
        } else if (eventosDelDia >= 2) {
            estiloCelda = "celda-dia-llena";
            estiloNumero = "numero-dia-lleno";
            estiloDisponibilidad = "disponibilidad-lleno";
            textoDisponibilidad = "● LLENO";
        } else if (eventosDelDia == 1) {
            estiloCelda = esHoy ? "celda-dia-hoy" : "celda-dia";
            estiloNumero = "numero-dia";
            estiloDisponibilidad = "disponibilidad-medio";
            textoDisponibilidad = "● 1 TURNO DISP.";
        } else {
            estiloCelda = esHoy ? "celda-dia-hoy" : "celda-dia";
            estiloNumero = "numero-dia";
            estiloDisponibilidad = "disponibilidad-libre";
            textoDisponibilidad = "● 2 TURNOS LIBRES";
        }

        Label lblNumero = new Label(String.valueOf(fecha.getDayOfMonth()));
        lblNumero.getStyleClass().add(estiloNumero);

        Label lblDisp = new Label(textoDisponibilidad);
        lblDisp.getStyleClass().add(estiloDisponibilidad);
        lblDisp.setWrapText(true);

        VBox celda = new VBox(4, lblNumero, lblDisp);
        celda.getStyleClass().add(estiloCelda);
        celda.setMaxWidth(Double.MAX_VALUE);
        celda.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(celda, Priority.ALWAYS);
        GridPane.setHgrow(celda, Priority.ALWAYS);

        if (eventosDelDia < 2 && !esPasado) {
            celda.setOnMouseClicked(e -> seleccionarFecha(fecha, celda, lblNumero, lblDisp));
        }

        return celda;
    }

    private void seleccionarFecha(LocalDate fecha, VBox celdaSeleccionada,
                                  Label lblNumero, Label lblDisp) {
        fechaSeleccionada = fecha;

        gridCalendario.getChildren().forEach(node -> {
            if (node instanceof VBox v) {
                if (v.getStyleClass().contains("celda-dia-seleccionada")) {
                    v.getStyleClass().remove("celda-dia-seleccionada");
                    v.getStyleClass().add("celda-dia");
                    v.getChildren().forEach(child -> {
                        if (child instanceof Label l) {
                            if (l.getStyleClass().contains("numero-dia-seleccionado")) {
                                l.getStyleClass().remove("numero-dia-seleccionado");
                                l.getStyleClass().add("numero-dia");
                            }
                            if (l.getStyleClass().contains("disponibilidad-seleccionada")) {
                                l.getStyleClass().remove("disponibilidad-seleccionada");
                                l.getStyleClass().add("disponibilidad-libre");
                            }
                        }
                    });
                }
            }
        });

        celdaSeleccionada.getStyleClass().remove("celda-dia");
        celdaSeleccionada.getStyleClass().remove("celda-dia-hoy");
        celdaSeleccionada.getStyleClass().add("celda-dia-seleccionada");

        lblNumero.getStyleClass().clear();
        lblNumero.getStyleClass().add("numero-dia-seleccionado");

        lblDisp.getStyleClass().clear();
        lblDisp.getStyleClass().add("disponibilidad-seleccionada");

        String nombreMes = fecha.getMonth()
                .getDisplayName(TextStyle.FULL, new Locale("es", "MX"));
        String fechaFormateada = fecha.getDayOfMonth()
                + " de " + nombreMes.substring(0, 1).toUpperCase()
                + nombreMes.substring(1).toLowerCase()
                + " de " + fecha.getYear();

        lblFechaSeleccionada.setText("Fecha seleccionada: " + fechaFormateada);
        lblFechaSeleccionada.setStyle("-fx-text-fill: #1a82b8; -fx-font-weight: bold; -fx-font-size: 12px;");
        btnContinuar.setDisable(false);
    }

    public LocalDate getFechaSeleccionada() {
        return fechaSeleccionada;
    }


    @FXML
    private void continuar() {
        if (fechaSeleccionada != null) {

            // Buscar el BorderPane en el momento del click
            if (ViewSwitcher.getContenedorPrincipal() == null) {
                javafx.scene.Parent nodo = btnContinuar.getParent();
                while (nodo != null) {
                    System.out.println("Nodo: " + nodo.getClass().getSimpleName() + " id=" + nodo.getId());
                    if (nodo instanceof BorderPane bp) {
                        ViewSwitcher.setContenedorPrincipal(bp);
                        break;
                    }
                    nodo = nodo.getParent();
                }
            }

            CotizacionController controller =
                    ViewSwitcher.cargarVistaConController("Cotizacion.fxml");
            if (controller != null) {
                controller.setFechaSeleccionada(fechaSeleccionada);
            }
        }
    }
    @FXML
    private void cancelar() {
        fechaSeleccionada = null;
        lblFechaSeleccionada.setText("Ninguna fecha seleccionada");
        lblFechaSeleccionada.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        btnContinuar.setDisable(true);
        renderizarCalendario();
    }
}