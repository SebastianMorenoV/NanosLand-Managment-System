package com.mycompany.presentacion.controllers;

import com.example.negocio.exception.CotizacionException;
import com.example.negocio.venta.usecase.GestionarProductoUseCase;
import com.mycompany.common.dtos.ProductoDTO;
import com.mycompany.presentacion.config.RegistroErrores;
import java.util.List;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/** Alta, edición y baja de los productos que se venden aparte de los eventos. */
@Controller
@Scope("prototype")
@RequiredArgsConstructor
public class ProductosModalController {

    private final GestionarProductoUseCase gestionarProductoUseCase;

    @FXML private Label lblFormulario;
    @FXML private TextField txtNombre;
    @FXML private TextField txtPrecio;
    @FXML private Label lblError;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelarEdicion;
    @FXML private Label lblSinProductos;
    @FXML private VBox listaProductos;

    private ProductoDTO enEdicion;

    @FXML
    public void initialize() {
        txtPrecio.setTextFormatter(new TextFormatter<>(cambio ->
                cambio.getControlNewText().matches("\\d{0,7}(\\.\\d{0,2})?") ? cambio : null));
        cargarProductos();
    }

    private void cargarProductos() {
        List<ProductoDTO> productos = gestionarProductoUseCase.obtenerProductosActivos();
        listaProductos.getChildren().clear();
        for (ProductoDTO producto : productos) {
            listaProductos.getChildren().add(filaProducto(producto));
        }
        lblSinProductos.setVisible(productos.isEmpty());
        lblSinProductos.setManaged(productos.isEmpty());
    }

    private HBox filaProducto(ProductoDTO producto) {
        Label nombre = new Label(producto.getNombre());
        nombre.setWrapText(true);
        nombre.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nombre, Priority.ALWAYS);
        nombre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label precio = new Label(String.format("$%,.2f", producto.getPrecio()));
        precio.setMinWidth(80);
        precio.setAlignment(Pos.CENTER_RIGHT);
        precio.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        Button editar = new Button("Editar");
        editar.setMinWidth(70);
        editar.getStyleClass().add("boton-azul-claro");
        editar.setOnAction(e -> empezarEdicion(producto));

        Button quitar = new Button("Quitar");
        quitar.setMinWidth(70);
        quitar.getStyleClass().add("boton-texto-rojo");
        quitar.setOnAction(e -> quitarProducto(producto));

        HBox fila = new HBox(10, nombre, precio, editar, quitar);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setPadding(new Insets(3, 8, 3, 0));
        return fila;
    }

    private void empezarEdicion(ProductoDTO producto) {
        enEdicion = producto;
        lblFormulario.setText("Editar producto");
        txtNombre.setText(producto.getNombre());
        txtPrecio.setText(String.format(java.util.Locale.ROOT, "%.2f", producto.getPrecio()));
        btnGuardar.setText("Guardar cambios");
        btnCancelarEdicion.setVisible(true);
        btnCancelarEdicion.setManaged(true);
        ocultarError();
        txtNombre.requestFocus();
    }

    @FXML
    private void cancelarEdicion() {
        enEdicion = null;
        lblFormulario.setText("Agregar producto");
        txtNombre.clear();
        txtPrecio.clear();
        btnGuardar.setText("Agregar");
        btnCancelarEdicion.setVisible(false);
        btnCancelarEdicion.setManaged(false);
        ocultarError();
    }

    @FXML
    private void guardar() {
        String textoPrecio = txtPrecio.getText() == null ? "" : txtPrecio.getText().trim();
        double precio;
        try {
            precio = textoPrecio.isEmpty() || textoPrecio.equals(".") ? 0 : Double.parseDouble(textoPrecio);
        } catch (NumberFormatException e) {
            mostrarError("Escribe un precio válido.");
            return;
        }
        try {
            if (enEdicion == null) {
                gestionarProductoUseCase.crearProducto(txtNombre.getText(), precio);
            } else {
                gestionarProductoUseCase.actualizarProducto(enEdicion.getId(), txtNombre.getText(), precio);
            }
            cancelarEdicion();
            cargarProductos();
            txtNombre.requestFocus();
        } catch (CotizacionException e) {
            mostrarError(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            RegistroErrores.avisar("No se pudo guardar el producto", e);
        }
    }

    private void quitarProducto(ProductoDTO producto) {
        ButtonType si = new ButtonType("Sí, quitar", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION, "", si, no);
        confirmar.setTitle("Quitar producto");
        confirmar.setHeaderText("¿Quitar \"" + producto.getNombre() + "\"?");
        confirmar.setContentText("Ya no aparecerá para vender. Las ventas anteriores no cambian.");
        Optional<ButtonType> respuesta = confirmar.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != si) {
            return;
        }
        try {
            gestionarProductoUseCase.eliminarProducto(producto.getId());
            if (enEdicion != null && enEdicion.getId().equals(producto.getId())) {
                cancelarEdicion();
            }
            cargarProductos();
        } catch (CotizacionException e) {
            mostrarError(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            RegistroErrores.avisar("No se pudo quitar el producto", e);
        }
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void ocultarError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    @FXML
    private void cerrar() {
        ((Stage) txtNombre.getScene().getWindow()).close();
    }
}
