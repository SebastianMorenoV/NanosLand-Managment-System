package com.mycompany.presentacion.controllers;

import com.example.negocio.agenda.usecase.ConsultarAgendaUseCase;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.presentacion.context.CotizacionContext;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import com.mycompany.presentacion.utils.ViewSwitcher;
import javafx.concurrent.Task;
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
    private final CotizacionContext cotizacionContext;

    @FXML
    private GridPane gridCalendario;
    @FXML
    private Label lblMesAnio;
    @FXML
    private Label lblFechaSeleccionada;
    @FXML
    private Button btnContinuar;

    @FXML
    private Button btnMesAnterior;

    private YearMonth mesActual = YearMonth.now();
    private LocalDate fechaSeleccionada = null;

    @FXML
    public void initialize() {
        // 1. Reiniciar el calendario al mes actual
        mesActual = YearMonth.now();

        // 2. Limpiar cualquier fecha que se haya quedado seleccionada anteriormente
        fechaSeleccionada = null;

        // 3. Restaurar los textos y botones visuales a su estado inicial
        lblFechaSeleccionada.setText("Ninguna fecha seleccionada");
        lblFechaSeleccionada.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        if (btnContinuar != null) {
            btnContinuar.setDisable(true);
        }

        // 4. Renderizar el calendario (esto disparará consultarAgendaUseCase.obtenerMesCompleto y actualizará los turnos)
        renderizarCalendario();
    }

    @FXML
    private void mesAnterior() {
        // Solo permite restar un mes si el mes actual del calendario es posterior al mes real en curso
        if (mesActual.isAfter(YearMonth.now())) {
            mesActual = mesActual.minusMonths(1);
            renderizarCalendario();
        }
    }

    @FXML
    private void mesSiguiente() {
        mesActual = mesActual.plusMonths(1);
        renderizarCalendario();
    }

    /**
     * Fix #8 — renderizarCalendario asincrónico.
     * La query a BD (consultarAgendaUseCase.obtenerMesCompleto) se mueve a un
     * Task de fondo. El usuario ya no verá la UI congelada al navegar meses.
     * El método actualiza la cabecera inmediatamente (sin BD) y luego espera
     * el resultado asincrónico para poblar las celdas del calendario.
     */
    private void renderizarCalendario() {
        // Actualizar cabecera inmediatamente (no requiere BD)
        String nombreMes = mesActual.getMonth()
                .getDisplayName(TextStyle.FULL, new Locale("es", "MX"));
        String titulo = nombreMes.substring(0, 1).toUpperCase()
                + nombreMes.substring(1).toLowerCase()
                + " " + mesActual.getYear();
        lblMesAnio.setText(titulo);

        // Deshabilitar navegación mientras carga para evitar peticiones duplicadas
        if (btnMesAnterior != null) btnMesAnterior.setDisable(true);

        // Fix #8: ejecutar la query de BD en hilo de fondo
        final YearMonth mesAConstruir = mesActual;  // captura inmutable para el Task
        Task<List<Evento>> task = new Task<>() {
            @Override
            protected List<Evento> call() {
                return consultarAgendaUseCase.obtenerMesCompleto(
                        mesAConstruir.getYear(), mesAConstruir.getMonthValue());
            }
        };

        task.setOnSucceeded(e -> {
            // Poblar celdas en el FX Thread (setOnSucceeded ya corre en FX Thread)
            List<Evento> eventos = task.getValue();
            poblarCeldasCalendario(mesAConstruir, eventos);
            // Restaurar botón de navegación
            if (btnMesAnterior != null) {
                btnMesAnterior.setDisable(!mesAConstruir.isAfter(YearMonth.now()));
            }
        });
        task.setOnFailed(e -> {
            System.err.println("[SeleccionarFecha] Error cargando agenda: " + task.getException().getMessage());
            if (btnMesAnterior != null) {
                btnMesAnterior.setDisable(!mesActual.isAfter(YearMonth.now()));
            }
        });
        new Thread(task, "hilo-carga-agenda").start();
    }

    /**
     * Pobla las celdas del GridPane con los datos ya cargados.
     * SIEMPRE se llama desde el FX Thread (via Task.setOnSucceeded).
     */
    private void poblarCeldasCalendario(YearMonth mes, List<Evento> eventos) {
        gridCalendario.getChildren().removeIf(node -> {
            Integer rowIndex = GridPane.getRowIndex(node);
            return rowIndex != null && rowIndex > 0;
        });

        gridCalendario.getRowConstraints().clear();

        RowConstraints headerRow = new RowConstraints();
        headerRow.setVgrow(Priority.NEVER);
        gridCalendario.getRowConstraints().add(headerRow);

        int primerDia = mes.atDay(1).getDayOfWeek().getValue();
        int totalDias = mes.lengthOfMonth();

        int col = primerDia - 1;
        int row = 1;
        int ultimaFila = 1;

        for (int dia = 1; dia <= totalDias; dia++) {
            LocalDate fecha = mes.atDay(dia);
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
        List<Evento> eventosDelDia = eventos.stream()
                .filter(e -> e.getFecha() != null && e.getFecha().equals(fecha))
                .toList();

        int turnosDisponibles = 0;

        boolean matutinoOcupado = eventosDelDia.stream().anyMatch(e -> e.getTurno() == TurnoEvento.MATUTINO);
        if (!matutinoOcupado) {
            turnosDisponibles++;
        }

        boolean vespertinoOcupado = eventosDelDia.stream().anyMatch(e -> e.getTurno() == TurnoEvento.VESPERTINO);
        if (!vespertinoOcupado) {
            turnosDisponibles++;
        }

        boolean esHoy = fecha.equals(LocalDate.now());
        boolean esPasado = fecha.isBefore(LocalDate.now());

        String estiloNumero;
        String estiloDisponibilidad;
        String textoDisponibilidad;
        String estiloCelda;

        if (esPasado) {
            estiloCelda = "celda-dia-nodisponible";
            estiloNumero = "numero-dia-no";
            estiloDisponibilidad = "disponibilidad-no";
            textoDisponibilidad = "No disponible";
        } else if (turnosDisponibles == 0) {
            estiloCelda = "celda-dia-llena";
            estiloNumero = "numero-dia-lleno";
            estiloDisponibilidad = "disponibilidad-lleno";
            textoDisponibilidad = "● LLENO";
        } else if (turnosDisponibles == 1) {
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

        if (turnosDisponibles > 0 && !esPasado) {
            celda.setOnMouseClicked(e -> {
                seleccionarFecha(fecha, celda, lblNumero, lblDisp);
                if (e.getClickCount() == 2) {
                    continuar();
                }
            });
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
            cotizacionContext.setFechaSeleccionada(fechaSeleccionada);
            ViewSwitcher.cargarVista("Cotizacion.fxml");
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
