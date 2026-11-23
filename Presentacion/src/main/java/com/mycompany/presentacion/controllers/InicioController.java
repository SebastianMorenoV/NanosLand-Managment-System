package com.mycompany.presentacion.controllers;

import com.example.negocio.inicio.usecase.GenerarResumenInicioUseCase;
import com.mycompany.common.dtos.ResumenInicioDTO;
import com.mycompany.common.dtos.ResumenInicioDTO.TipoPendiente;
import com.mycompany.presentacion.config.RegistroErrores;
import com.mycompany.presentacion.context.CotizacionContext;
import com.mycompany.presentacion.context.SesionContext;
import com.mycompany.presentacion.utils.ViewSwitcher;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/** Pantalla de Inicio: lo que hay hoy, lo que viene y lo que requiere atención. */
@Controller
@Scope("prototype")
@RequiredArgsConstructor
public class InicioController {

    private static final Locale ES = new Locale("es", "MX");

    private final GenerarResumenInicioUseCase generarResumenInicioUseCase;
    private final SesionContext sesionContext;
    private final CotizacionContext cotizacionContext;

    @FXML private Label lblFecha;
    @FXML private Label lblEventosHoy;
    @FXML private Label lblEventosProximos;
    @FXML private Label lblPorConfirmar;
    @FXML private Label lblPorContactar;
    @FXML private HBox boxDinero;
    @FXML private Label lblIngresosMes;
    @FXML private Label lblPorCobrar;
    @FXML private Label lblSinEventos;
    @FXML private Label lblSinPendientes;
    @FXML private VBox listaEventos;
    @FXML private VBox listaPendientes;

    @FXML
    public void initialize() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES));
        lblFecha.setText("Hoy es " + fecha);

        boolean esDueno = sesionContext.esDueno();
        boxDinero.setVisible(esDueno);
        boxDinero.setManaged(esDueno);

        // En segundo plano: la pantalla aparece de inmediato y los datos llegan después.
        Task<ResumenInicioDTO> tarea = new Task<>() {
            @Override
            protected ResumenInicioDTO call() {
                return generarResumenInicioUseCase.generar(esDueno);
            }
        };
        tarea.setOnSucceeded(e -> mostrar(tarea.getValue()));
        tarea.setOnFailed(e -> {
            tarea.getException().printStackTrace();
            RegistroErrores.avisar("No se pudieron cargar los datos de Inicio", tarea.getException());
        });
        Thread hilo = new Thread(tarea, "hilo-inicio");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrar(ResumenInicioDTO resumen) {
        lblEventosHoy.setText(String.valueOf(resumen.getEventosHoy()));
        lblEventosProximos.setText(String.valueOf(resumen.getEventosProximos()));
        lblPorConfirmar.setText(String.valueOf(resumen.getCotizacionesPorConfirmar()));
        lblPorContactar.setText(String.valueOf(resumen.getServiciosPorContactar()));
        if (resumen.getIngresosMes() != null) {
            lblIngresosMes.setText(String.format("$%,.2f", resumen.getIngresosMes()));
        }
        if (resumen.getPorCobrar() != null) {
            lblPorCobrar.setText(String.format("$%,.2f", resumen.getPorCobrar()));
        }

        listaEventos.getChildren().clear();
        for (ResumenInicioDTO.EventoProximo evento : resumen.getEventos()) {
            listaEventos.getChildren().add(filaEvento(evento));
        }
        lblSinEventos.setVisible(resumen.getEventos().isEmpty());
        lblSinEventos.setManaged(resumen.getEventos().isEmpty());

        listaPendientes.getChildren().clear();
        for (ResumenInicioDTO.Pendiente pendiente : resumen.getPendientes()) {
            listaPendientes.getChildren().add(filaPendiente(pendiente));
        }
        lblSinPendientes.setVisible(resumen.getPendientes().isEmpty());
        lblSinPendientes.setManaged(resumen.getPendientes().isEmpty());
    }

    private HBox filaEvento(ResumenInicioDTO.EventoProximo evento) {
        Label dia = etiquetaDia(evento.getFecha());
        Label horario = new Label(evento.getHorario());
        horario.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label descripcion = new Label(evento.getDescripcion());
        descripcion.setWrapText(true);
        descripcion.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        VBox texto = new VBox(1, horario, descripcion);
        HBox.setHgrow(texto, Priority.ALWAYS);
        texto.setMaxWidth(Double.MAX_VALUE);

        Button abrir = botonFila("Abrir");
        abrir.setOnAction(e -> abrirCotizacion(evento.getCotizacionId()));
        return fila(dia, texto, abrir);
    }

    private HBox filaPendiente(ResumenInicioDTO.Pendiente pendiente) {
        Label dia = etiquetaDia(pendiente.getFecha());
        String color = pendiente.getTipo() == TipoPendiente.COBRAR ? "#e74c3c"
                : pendiente.getTipo() == TipoPendiente.CONFIRMAR ? "#f39c12" : "#1a82b8";
        Label titulo = new Label(pendiente.getTitulo());
        titulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label detalle = new Label(pendiente.getDetalle());
        detalle.setWrapText(true);
        detalle.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        VBox texto = new VBox(1, titulo, detalle);
        HBox.setHgrow(texto, Priority.ALWAYS);
        texto.setMaxWidth(Double.MAX_VALUE);

        Button accion;
        if (pendiente.getTipo() == TipoPendiente.CONTACTAR) {
            accion = botonFila("Ver");
            accion.setOnAction(e -> irLogistica());
        } else {
            accion = botonFila(pendiente.getTipo() == TipoPendiente.COBRAR ? "Cobrar" : "Confirmar");
            accion.setOnAction(e -> abrirCotizacion(pendiente.getCotizacionId()));
        }
        return fila(dia, texto, accion);
    }

    private static HBox fila(Label dia, VBox texto, Button accion) {
        HBox fila = new HBox(10, dia, texto, accion);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setPadding(new Insets(5, 8, 5, 0));
        return fila;
    }

    private static Button botonFila(String texto) {
        Button boton = new Button(texto);
        boton.getStyleClass().add("boton-azul-claro");
        boton.setMinWidth(82);
        boton.setStyle("-fx-font-size: 12px; -fx-padding: 5 10 5 10;");
        return boton;
    }

    private static Label etiquetaDia(LocalDate fecha) {
        LocalDate hoy = LocalDate.now();
        String texto = fecha.equals(hoy) ? "Hoy" : fecha.equals(hoy.plusDays(1)) ? "Mañana"
                : fecha.format(DateTimeFormatter.ofPattern("EEE d MMM", ES)).replace(".", "");
        Label dia = new Label(texto);
        dia.setMinWidth(82);
        dia.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: "
                + (fecha.equals(hoy) ? "#1a82b8" : "#7f8c8d") + ";");
        return dia;
    }

    private void abrirCotizacion(Long cotizacionId) {
        cotizacionContext.abrir(cotizacionId, "Inicio.fxml");
        ViewSwitcher.cargarVista("Cotizacion.fxml");
    }

    @FXML
    private void nuevaCotizacion() {
        ViewSwitcher.cargarVista("SeleccionarFecha.fxml");
    }

    @FXML
    private void irVentas() {
        ViewSwitcher.cargarVista("Ventas.fxml");
    }

    @FXML
    private void irEventos() {
        ViewSwitcher.cargarVista("Eventos.fxml");
    }

    @FXML
    private void irCotizaciones() {
        ViewSwitcher.cargarVista("Cotizaciones.fxml");
    }

    @FXML
    private void irLogistica() {
        ViewSwitcher.cargarVista("Logistica.fxml");
    }
}
