package com.mycompany.presentacion.controllers;

import com.example.negocio.exception.CotizacionException;
import com.example.negocio.venta.usecase.CancelarVentaUseCase;
import com.example.negocio.venta.usecase.ConsultarVentasUseCase;
import com.example.negocio.venta.usecase.GestionarProductoUseCase;
import com.example.negocio.venta.usecase.RegistrarVentaUseCase;
import com.mycompany.common.dtos.DetalleVentaDTO;
import com.mycompany.common.dtos.ProductoDTO;
import com.mycompany.common.dtos.ReporteLogisticaSemanalDTO;
import com.mycompany.common.dtos.VentaDTO;
import com.mycompany.persistencia.enums.MetodoPago;
import com.mycompany.presentacion.config.RegistroErrores;
import com.mycompany.presentacion.context.SesionContext;
import com.mycompany.presentacion.utils.ModalHelper;
import com.mycompany.presentacion.utils.ViewSwitcher;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * Pantalla de ventas extra: se tocan los productos, se elige cómo pagó y se
 * registra. El total del día y los días anteriores solo los ve el dueño.
 */
@Controller
@Scope("prototype")
@RequiredArgsConstructor
public class VentasController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final GestionarProductoUseCase gestionarProductoUseCase;
    private final RegistrarVentaUseCase registrarVentaUseCase;
    private final CancelarVentaUseCase cancelarVentaUseCase;
    private final ConsultarVentasUseCase consultarVentasUseCase;
    private final SesionContext sesionContext;

    @FXML private FlowPane panelProductos;
    @FXML private Label lblSinProductos;
    @FXML private VBox listaCarrito;
    @FXML private Label lblCarritoVacio;
    @FXML private Label lblTotal;
    @FXML private Label lblMensaje;
    @FXML private ToggleGroup grupoPago;
    @FXML private ToggleButton btnEfectivo;
    @FXML private ToggleButton btnTransferencia;
    @FXML private ToggleButton btnTarjeta;
    @FXML private Button btnRegistrar;
    @FXML private Button btnVaciar;
    @FXML private Label lblTituloVentas;
    @FXML private Label lblTotalDia;
    @FXML private DatePicker dpDia;
    @FXML private Label lblSinVentas;
    @FXML private VBox listaVentas;

    /** Renglones de la venta en curso, por id de producto. */
    private final Map<Long, Renglon> carrito = new LinkedHashMap<>();

    private static final class Renglon {
        ProductoDTO producto;
        int cantidad;

        Renglon(ProductoDTO producto) {
            this.producto = producto;
        }

        double subtotal() {
            return Math.round(producto.getPrecio() * cantidad * 100.0) / 100.0;
        }
    }

    @FXML
    public void initialize() {
        btnEfectivo.setUserData(MetodoPago.EFECTIVO);
        btnTransferencia.setUserData(MetodoPago.TRANSFERENCIA);
        btnTarjeta.setUserData(MetodoPago.TARJETA);
        grupoPago.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            // Siempre debe quedar un método elegido.
            if (nuevo == null && anterior != null) {
                anterior.setSelected(true);
            }
            pintarMetodos();
        });
        btnEfectivo.setSelected(true);
        pintarMetodos();

        boolean esDueno = sesionContext.esDueno();
        lblTotalDia.setVisible(esDueno);
        lblTotalDia.setManaged(esDueno);
        dpDia.setVisible(esDueno);
        dpDia.setManaged(esDueno);
        dpDia.setEditable(false);
        dpDia.setValue(LocalDate.now());
        dpDia.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate fecha, boolean vacio) {
                super.updateItem(fecha, vacio);
                setDisable(vacio || fecha.isAfter(LocalDate.now()));
            }
        });
        dpDia.valueProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo != null) {
                cargarVentas();
            }
        });

        cargarProductos();
        cargarVentas();
    }

    // ─── Productos ──────────────────────────────────────────────────────────

    private void cargarProductos() {
        List<ProductoDTO> productos = gestionarProductoUseCase.obtenerProductosActivos();
        panelProductos.getChildren().clear();
        for (ProductoDTO producto : productos) {
            panelProductos.getChildren().add(botonProducto(producto));
        }
        lblSinProductos.setVisible(productos.isEmpty());
        lblSinProductos.setManaged(productos.isEmpty());

        // La venta en curso sigue a los cambios del catálogo (precio, nombre o baja).
        Map<Long, ProductoDTO> porId = new LinkedHashMap<>();
        productos.forEach(p -> porId.put(p.getId(), p));
        carrito.keySet().removeIf(id -> !porId.containsKey(id));
        carrito.forEach((id, renglon) -> renglon.producto = porId.get(id));
        actualizarCarrito();
    }

    private Button botonProducto(ProductoDTO producto) {
        Button boton = new Button(producto.getNombre() + "\n" + dinero(producto.getPrecio()));
        boton.getStyleClass().add("boton-azul-claro");
        boton.setWrapText(true);
        boton.setTextAlignment(TextAlignment.CENTER);
        boton.setAlignment(Pos.CENTER);
        boton.setPrefSize(150, 78);
        boton.setOnAction(e -> cambiarCantidad(producto, 1));
        return boton;
    }

    @FXML
    private void abrirProductos() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass()
                    .getResource("/com/mycompany/presentacion/views/ProductosModal.fxml"));
            loader.setControllerFactory(ViewSwitcher.getSpringContext()::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(btnRegistrar.getScene().getWindow());
            ModalHelper.mostrarModal(root, "Productos", stage);
            cargarProductos();
        } catch (Exception e) {
            e.printStackTrace();
            RegistroErrores.avisar("No se pudo abrir la ventana de productos", e);
        }
    }

    // ─── Venta en curso ─────────────────────────────────────────────────────

    private void cambiarCantidad(ProductoDTO producto, int cambio) {
        Renglon renglon = carrito.computeIfAbsent(producto.getId(), id -> new Renglon(producto));
        int nueva = renglon.cantidad + cambio;
        if (nueva > RegistrarVentaUseCase.CANTIDAD_MAXIMA) {
            return;
        }
        renglon.cantidad = nueva;
        if (renglon.cantidad <= 0) {
            carrito.remove(producto.getId());
        }
        actualizarCarrito();
    }

    private void actualizarCarrito() {
        listaCarrito.getChildren().clear();
        double total = 0.0;
        for (Renglon renglon : carrito.values()) {
            listaCarrito.getChildren().add(filaCarrito(renglon));
            total += renglon.subtotal();
        }
        boolean vacio = carrito.isEmpty();
        lblCarritoVacio.setVisible(vacio);
        lblCarritoVacio.setManaged(vacio);
        lblTotal.setText(dinero(total));
        btnRegistrar.setDisable(vacio);
        btnVaciar.setDisable(vacio);
        ocultarMensaje();
    }

    private HBox filaCarrito(Renglon renglon) {
        Label nombre = new Label(renglon.producto.getNombre());
        nombre.setWrapText(true);
        nombre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label precio = new Label(dinero(renglon.subtotal()));
        precio.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");
        VBox texto = new VBox(1, nombre, precio);
        HBox.setHgrow(texto, Priority.ALWAYS);
        texto.setMaxWidth(Double.MAX_VALUE);

        Button menos = botonCantidad("−");
        menos.setOnAction(e -> cambiarCantidad(renglon.producto, -1));
        Label cantidad = new Label(String.valueOf(renglon.cantidad));
        cantidad.setAlignment(Pos.CENTER);
        cantidad.setMinWidth(34);
        cantidad.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Button mas = botonCantidad("+");
        mas.setOnAction(e -> cambiarCantidad(renglon.producto, 1));

        HBox fila = new HBox(6, texto, menos, cantidad, mas);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setPadding(new Insets(0, 8, 0, 0));
        return fila;
    }

    private static Button botonCantidad(String texto) {
        Button boton = new Button(texto);
        boton.getStyleClass().add("boton-azul-claro");
        boton.setMinSize(36, 36);
        boton.setPrefSize(36, 36);
        boton.setStyle("-fx-font-size: 16px; -fx-padding: 0;");
        return boton;
    }

    private void pintarMetodos() {
        for (ToggleButton boton : List.of(btnEfectivo, btnTransferencia, btnTarjeta)) {
            boton.setStyle((boton.isSelected()
                    ? "-fx-background-color: #1a82b8; -fx-text-fill: white;"
                    : "-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50;")
                    + " -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 9 8 9 8; -fx-cursor: hand;");
        }
    }

    @FXML
    private void vaciarVenta() {
        carrito.clear();
        btnEfectivo.setSelected(true);
        actualizarCarrito();
    }

    @FXML
    private void registrarVenta() {
        if (carrito.isEmpty()) {
            return;
        }
        List<DetalleVentaDTO> productos = new ArrayList<>();
        for (Renglon renglon : carrito.values()) {
            DetalleVentaDTO detalle = new DetalleVentaDTO();
            detalle.setProductoId(renglon.producto.getId());
            detalle.setCantidad(renglon.cantidad);
            productos.add(detalle);
        }
        MetodoPago metodo = (MetodoPago) grupoPago.getSelectedToggle().getUserData();
        try {
            VentaDTO venta = registrarVentaUseCase.registrarVenta(
                    productos, metodo, sesionContext.getUsuarioAutenticado().getId());
            vaciarVenta();
            if (!LocalDate.now().equals(dpDia.getValue())) {
                dpDia.setValue(LocalDate.now());
            } else {
                cargarVentas();
            }
            mostrarMensaje("Venta registrada por " + dinero(venta.getTotal()) + ".");
        } catch (CotizacionException e) {
            mostrarAlerta(Alert.AlertType.WARNING, e.getMessage());
            cargarProductos();
        } catch (Exception e) {
            e.printStackTrace();
            RegistroErrores.avisar("No se pudo registrar la venta", e);
        }
    }

    private void mostrarMensaje(String texto) {
        lblMensaje.setText(texto);
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
    }

    private void ocultarMensaje() {
        lblMensaje.setVisible(false);
        lblMensaje.setManaged(false);
    }

    // ─── Ventas del día ─────────────────────────────────────────────────────

    private LocalDate diaMostrado() {
        // El administrador solo ve las ventas de hoy.
        return sesionContext.esDueno() && dpDia.getValue() != null ? dpDia.getValue() : LocalDate.now();
    }

    private void cargarVentas() {
        LocalDate dia = diaMostrado();
        List<VentaDTO> ventas = consultarVentasUseCase.obtenerVentasDelDia(dia);

        lblTituloVentas.setText(dia.equals(LocalDate.now()) ? "Ventas de hoy" : "Ventas del " + dia.format(FECHA));
        listaVentas.getChildren().clear();
        for (VentaDTO venta : ventas) {
            listaVentas.getChildren().add(filaVenta(venta));
        }
        lblSinVentas.setVisible(ventas.isEmpty());
        lblSinVentas.setManaged(ventas.isEmpty());

        double total = ventas.stream().filter(v -> !v.isCancelada()).mapToDouble(VentaDTO::getTotal).sum();
        lblTotalDia.setText("Total: " + dinero(total));
    }

    private HBox filaVenta(VentaDTO venta) {
        String colorTexto = venta.isCancelada() ? "#b2b2b2" : "#2c3e50";

        Label hora = new Label(ReporteLogisticaSemanalDTO.horaTexto(venta.getFechaHora().toLocalTime()));
        hora.setMinWidth(70);
        hora.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");

        Label productos = new Label(venta.getResumen());
        productos.setWrapText(true);
        productos.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(productos, Priority.ALWAYS);
        productos.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + colorTexto + ";");

        Label metodo = new Label(nombreMetodo(venta.getMetodoPago()));
        metodo.setMinWidth(Region.USE_PREF_SIZE);
        metodo.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        Label total = new Label(dinero(venta.getTotal()));
        total.setMinWidth(80);
        total.setAlignment(Pos.CENTER_RIGHT);
        total.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + colorTexto + ";");

        HBox fila = new HBox(10, hora, productos, metodo, total);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setPadding(new Insets(3, 8, 3, 0));
        if (venta.isCancelada()) {
            Label cancelada = new Label("Cancelada");
            cancelada.setMinWidth(76);
            cancelada.setAlignment(Pos.CENTER);
            cancelada.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
            fila.getChildren().add(cancelada);
        } else {
            Button cancelar = new Button("Cancelar");
            cancelar.setMinWidth(76);
            cancelar.getStyleClass().add("boton-texto-rojo");
            cancelar.setOnAction(e -> cancelarVenta(venta));
            fila.getChildren().add(cancelar);
        }
        return fila;
    }

    private void cancelarVenta(VentaDTO venta) {
        ButtonType si = new ButtonType("Sí, cancelar venta", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION, "", si, no);
        confirmar.setTitle("Cancelar venta");
        confirmar.setHeaderText("¿Cancelar esta venta de " + dinero(venta.getTotal()) + "?");
        confirmar.setContentText(venta.getResumen()
                + "\n\nSeguirá en la lista como cancelada y ya no contará en los ingresos.");
        Optional<ButtonType> respuesta = confirmar.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != si) {
            return;
        }
        try {
            cancelarVentaUseCase.cancelarVenta(venta.getId(), sesionContext.getUsuarioAutenticado().getId());
        } catch (CotizacionException e) {
            mostrarAlerta(Alert.AlertType.WARNING, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            RegistroErrores.avisar("No se pudo cancelar la venta", e);
        }
        cargarVentas();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle("Ventas");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private static String nombreMetodo(MetodoPago metodo) {
        switch (metodo) {
            case EFECTIVO: return "Efectivo";
            case TRANSFERENCIA: return "Transferencia";
            case TARJETA: return "Tarjeta";
            default: return metodo.name();
        }
    }

    private static String dinero(double monto) {
        return String.format("$%,.2f", monto);
    }
}
