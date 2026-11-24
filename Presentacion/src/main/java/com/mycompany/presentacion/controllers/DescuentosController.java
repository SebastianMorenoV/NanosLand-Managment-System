package com.mycompany.presentacion.controllers;

import com.example.negocio.descuento.usecase.GestionarDescuentoUseCase;
import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.DescuentoDTO;
import com.mycompany.common.util.DescripcionDescuento;
import com.mycompany.persistencia.enums.TipoDescuento;
import com.mycompany.presentacion.config.RegistroErrores;
import com.mycompany.presentacion.utils.ModalHelper;
import com.mycompany.presentacion.utils.ViewSwitcher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * Lista de descuentos automáticos a paquetes, como tarjetas con la regla en palabras y sus
 * botones, separada en activos, pausados y vencidos. Solo la ve el dueño (el menú la oculta
 * a los administradores).
 */
@Controller
@Scope("prototype")
@RequiredArgsConstructor
public class DescuentosController {

    private final GestionarDescuentoUseCase gestionarDescuentoUseCase;

    @FXML private ToggleGroup grupoFiltro;
    @FXML private ToggleButton btnActivos;
    @FXML private ToggleButton btnPausados;
    @FXML private ToggleButton btnVencidos;
    @FXML private TextField txtBuscar;
    @FXML private VBox listaDescuentos;

    private final List<DescuentoDTO> todos = new ArrayList<>();
    private final Map<DescuentoDTO.Estado, ToggleButton> pestanas = new EnumMap<>(DescuentoDTO.Estado.class);
    private final Map<DescuentoDTO.Estado, String> nombres = new EnumMap<>(DescuentoDTO.Estado.class);

    @FXML
    public void initialize() {
        registrarPestana(DescuentoDTO.Estado.ACTIVO, btnActivos);
        registrarPestana(DescuentoDTO.Estado.DESACTIVADO, btnPausados);
        registrarPestana(DescuentoDTO.Estado.VENCIDO, btnVencidos);

        grupoFiltro.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            // Siempre queda una pestaña elegida.
            if (nuevo == null && anterior != null) {
                anterior.setSelected(true);
                return;
            }
            mostrar();
        });
        txtBuscar.textProperty().addListener((obs, anterior, nuevo) -> mostrar());

        btnActivos.setSelected(true);
        cargar(null);
    }

    private void registrarPestana(DescuentoDTO.Estado estado, ToggleButton boton) {
        boton.setUserData(estado);
        pestanas.put(estado, boton);
        nombres.put(estado, boton.getText());
    }

    /** Recarga la lista; si se indica un descuento, abre la pestaña donde quedó. */
    private void cargar(Long mostrarDescuento) {
        try {
            todos.clear();
            todos.addAll(gestionarDescuentoUseCase.obtenerTodos());
        } catch (RuntimeException e) {
            e.printStackTrace();
            listaDescuentos.getChildren().setAll(vacia("No se pudieron cargar los descuentos."));
            RegistroErrores.avisar("No se pudo cargar la lista de descuentos", e);
            return;
        }
        Map<DescuentoDTO.Estado, Integer> conteos = new EnumMap<>(DescuentoDTO.Estado.class);
        todos.forEach(d -> conteos.merge(d.getEstado(), 1, Integer::sum));
        pestanas.forEach((estado, boton) ->
                boton.setText(nombres.get(estado) + " (" + conteos.getOrDefault(estado, 0) + ")"));

        todos.stream().filter(d -> d.getId().equals(mostrarDescuento)).findFirst().ifPresent(d -> {
            txtBuscar.clear();
            pestanas.get(d.getEstado()).setSelected(true);
        });
        mostrar();
    }

    private void mostrar() {
        DescuentoDTO.Estado estado = (DescuentoDTO.Estado) grupoFiltro.getSelectedToggle().getUserData();
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        List<Node> tarjetas = new ArrayList<>();
        for (DescuentoDTO d : todos) {
            if (d.getEstado() == estado && (texto.isEmpty() || contiene(d.getNombre(), texto)
                    || contiene(DescripcionDescuento.paquetes(d), texto))) {
                tarjetas.add(tarjeta(d));
            }
        }
        if (tarjetas.isEmpty()) {
            tarjetas.add(vacia(!texto.isEmpty() ? "No se encontró ningún descuento."
                    : estado == DescuentoDTO.Estado.ACTIVO ? "Todavía no hay descuentos activos. Cree uno con «+ Nuevo descuento»."
                    : estado == DescuentoDTO.Estado.DESACTIVADO ? "No hay descuentos pausados."
                    : "No hay descuentos vencidos."));
        }
        listaDescuentos.getChildren().setAll(tarjetas);
    }

    /** Tarjeta de un descuento: cuánto, nombre y estado, la regla en una frase y sus botones. */
    private Node tarjeta(DescuentoDTO d) {
        Label valor = new Label("−" + (d.getTipo() == TipoDescuento.MONTO_FIJO
                ? String.format("$%,.0f", d.getValor()) : DescripcionDescuento.valor(d.getTipo(), d.getValor())));
        valor.getStyleClass().add("descuento-valor");

        Label nombre = new Label(d.getNombre());
        nombre.getStyleClass().add("descuento-nombre");
        Label estado = new Label(d.getEstado().getEtiqueta());
        estado.getStyleClass().addAll("estado-chip", switch (d.getEstado()) {
            case ACTIVO -> "estado-activo";
            case DESACTIVADO -> "estado-pausado";
            case VENCIDO -> "estado-vencido";
        });
        HBox encabezado = new HBox(10, nombre, estado);
        encabezado.setAlignment(Pos.CENTER_LEFT);

        Label regla = new Label(DescripcionDescuento.resumen(d));
        regla.getStyleClass().add("descuento-detalle");
        regla.setWrapText(true);
        VBox texto = new VBox(6, encabezado, regla);
        HBox.setHgrow(texto, Priority.ALWAYS);

        Button editar = boton("Editar", "boton-azul-claro", () -> abrirModal(d));
        Button pausar = boton(d.isActivo() ? "Pausar" : "Activar", "boton-texto-gris", () -> cambiarActivo(d));
        Button eliminar = boton("Eliminar", "boton-texto-rojo", () -> eliminar(d));
        VBox acciones = new VBox(4, editar, pausar);
        if (d.getFechaInicio() != null || d.getFechaFin() != null || d.getCotizarHasta() != null) {
            acciones.getChildren().add(boton("Repetir el próximo año", "boton-texto-gris", () -> duplicar(d)));
        }
        acciones.getChildren().add(eliminar);
        acciones.getChildren().forEach(b -> ((Button) b).setMaxWidth(Double.MAX_VALUE));
        acciones.setMinWidth(170);

        HBox tarjeta = new HBox(18, valor, texto, acciones);
        tarjeta.setAlignment(Pos.CENTER_LEFT);
        tarjeta.getStyleClass().add("tarjeta-descuento");
        tarjeta.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                abrirModal(d);
            }
        });
        return tarjeta;
    }

    private static Button boton(String texto, String estilo, Runnable accion) {
        Button boton = new Button(texto);
        boton.getStyleClass().add(estilo);
        boton.setOnAction(e -> accion.run());
        return boton;
    }

    private static Label vacia(String mensaje) {
        Label label = new Label(mensaje);
        label.getStyleClass().add("lista-vacia");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        return label;
    }

    private static boolean contiene(String valor, String texto) {
        return valor != null && valor.toLowerCase().contains(texto);
    }

    @FXML
    private void nuevo() {
        abrirModal(null);
    }

    private void cambiarActivo(DescuentoDTO d) {
        try {
            gestionarDescuentoUseCase.cambiarActivo(d.getId(), !d.isActivo());
            cargar(d.getId());
        } catch (CotizacionException e) {
            mostrarError(e.getMessage());
            cargar(null);
        }
    }

    private void duplicar(DescuentoDTO d) {
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

    private void eliminar(DescuentoDTO d) {
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION);
        confirmar.setTitle("Eliminar descuento");
        confirmar.setHeaderText("¿Eliminar el descuento «" + d.getNombre() + "»?");
        confirmar.setContentText("Las cotizaciones que ya lo tienen conservan su precio. "
                + "Si solo quiere dejar de usarlo por un tiempo, use «Pausar».");
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
