package com.mycompany.presentacion.controllers;

import com.example.negocio.catalogo.usecase.ConsultarServiciosUseCase;
import com.example.negocio.paquete.usecase.ActualizarPaqueteUseCase;
import com.example.negocio.paquete.usecase.ConsultarPaquetesUseCase;
import com.example.negocio.paquete.usecase.RegistrarPaqueteUseCase;
import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.dtos.PaquetePrecioDTO;
import com.mycompany.common.dtos.PaqueteServicioDTO;
import com.mycompany.common.dtos.ServicioDTO;
import com.mycompany.persistencia.enums.CategoriaServicio;
import com.mycompany.presentacion.utils.EditorTablaPrecios;
import com.mycompany.presentacion.utils.TablaPreciosPaquete;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Alta y edición de un paquete: datos generales, precio (fijo o tabla por comida y
 * personas) y servicios incluidos. Una instancia por ventana.
 */
@Controller
@Scope("prototype")
@RequiredArgsConstructor
public class PaqueteModalController {

    private final RegistrarPaqueteUseCase registrarPaqueteUseCase;
    private final ActualizarPaqueteUseCase actualizarPaqueteUseCase;
    private final ConsultarServiciosUseCase consultarServiciosUseCase;
    private final ConsultarPaquetesUseCase consultarPaquetesUseCase;

    // Columna Izquierda
    @FXML private Label lblTitulo;
    @FXML private TextField txtNombre;
    @FXML private RadioButton rbPrecioFijo;
    @FXML private RadioButton rbTablaPrecios;
    @FXML private TextField txtPrecio;
    @FXML private HBox boxTablaPrecios;
    @FXML private Label lblResumenTabla;
    @FXML private TextArea txtDescripcion;

    // Columna Derecha
    @FXML private Label lblAvisoCotizaciones;
    @FXML private HBox boxAgregarServicio;
    @FXML private ComboBox<ServicioDTO> cmbServicios;
    @FXML private TextField txtCantidad;

    @FXML private TableView<PaqueteServicioDTO> tablaServicios;
    @FXML private TableColumn<PaqueteServicioDTO, String> colServicioNombre;
    @FXML private TableColumn<PaqueteServicioDTO, Double> colPrecioUnitario;
    @FXML private TableColumn<PaqueteServicioDTO, Integer> colCantidad;
    @FXML private TableColumn<PaqueteServicioDTO, Double> colSubtotal;
    @FXML private TableColumn<PaqueteServicioDTO, Void> colEliminar;

    private PaqueteDTO paqueteAEditar;
    @Getter
    private PaqueteDTO paqueteCreado;
    /** Con cotizaciones abiertas no se pueden cambiar los servicios incluidos. */
    private boolean serviciosBloqueados;

    private final ObservableList<PaqueteServicioDTO> listaServicios = FXCollections.observableArrayList();
    private List<PaquetePrecioDTO> tablaPrecios = new ArrayList<>();

    @FXML
    public void initialize() {
        txtPrecio.setTextFormatter(new TextFormatter<>(change ->
            change.getControlNewText().matches("\\d{0,7}(\\.\\d{0,2})?") ? change : null));
        txtCantidad.setTextFormatter(new TextFormatter<>(change ->
            change.getControlNewText().matches("\\d{0,4}") ? change : null));

        ToggleGroup grupoPrecio = new ToggleGroup();
        rbPrecioFijo.setToggleGroup(grupoPrecio);
        rbTablaPrecios.setToggleGroup(grupoPrecio);
        grupoPrecio.selectedToggleProperty().addListener((obs, anterior, nuevo) -> aplicarModoPrecio());
        aplicarModoPrecio();

        // Las comidas no se incluyen aquí: van en la tabla de precios.
        cmbServicios.setItems(FXCollections.observableArrayList(consultarServiciosUseCase.obtenerParaComposicion()));
        cmbServicios.setConverter(new StringConverter<ServicioDTO>() {
            @Override
            public String toString(ServicioDTO s) {
                if (s == null) return "";
                return s.getCategoria() == CategoriaServicio.INCLUIDO && s.getPrecio() == 0
                    ? s.getNombre() + " (solo en paquetes)"
                    : s.getNombre() + " (" + TablaPreciosPaquete.dinero(s.getPrecio()) + ")";
            }

            @Override
            public ServicioDTO fromString(String string) {
                return null;
            }
        });

        // Configurar Tabla
        colServicioNombre.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getServicio().getNombre()));
        colPrecioUnitario.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getServicio().getPrecio()));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        colPrecioUnitario.setCellFactory(c -> celdaDinero());
        colSubtotal.setCellFactory(c -> celdaDinero());

        configurarColumnaEliminar();
        tablaServicios.setItems(listaServicios);
    }

    private static TableCell<PaqueteServicioDTO, Double> celdaDinero() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : TablaPreciosPaquete.dinero(item));
            }
        };
    }

    private void configurarColumnaEliminar() {
        colEliminar.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("X");

            {
                btn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                btn.setOnAction(event -> listaServicios.remove(getTableView().getItems().get(getIndex())));
            }

            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || serviciosBloqueados ? null : btn);
            }
        });
    }

    /** Se llama antes de mostrar la ventana. */
    public void setPaqueteAEditar(PaqueteDTO paquete) {
        this.paqueteAEditar = paquete;
        lblTitulo.setText("Editar Paquete");

        txtNombre.setText(paquete.getNombre());
        txtDescripcion.setText(paquete.getDescripcion() != null ? paquete.getDescripcion() : "");
        tablaPrecios = copiarPrecios(paquete.getPrecios());
        if (paquete.isTablaPrecios()) {
            rbTablaPrecios.setSelected(true);
        } else {
            rbPrecioFijo.setSelected(true);
            txtPrecio.setText(String.format(Locale.ROOT, "%.2f", paquete.getCostoBase()));
        }
        aplicarModoPrecio();

        listaServicios.clear();
        if (paquete.getServicios() != null) {
            // Se trabaja sobre copias: si el usuario cancela, el paquete de la tabla
            // no debe quedar con las cantidades que se cambiaron en este modal.
            for (PaqueteServicioDTO original : paquete.getServicios()) {
                listaServicios.add(new PaqueteServicioDTO(original.getId(), original.getServicio(),
                    original.getCantidad(), original.getSubtotal()));
            }
        }

        // Las cotizaciones abiertas guardan su copia del paquete: conservan lo que se les vendió y
        // el paquete se puede cambiar. Solo las que aún no la tienen dependen de él.
        List<String> sinCopia = consultarPaquetesUseCase.cotizacionesAbiertasSinCopia(paquete.getId());
        long abiertas = consultarPaquetesUseCase.contarCotizacionesAbiertas(paquete.getId());
        serviciosBloqueados = !sinCopia.isEmpty();
        if (serviciosBloqueados) {
            lblAvisoCotizaciones.setText("Estas cotizaciones abiertas aún no guardan su copia del paquete: "
                    + String.join(", ", sinCopia) + ". Por ahora no se pueden cambiar los servicios incluidos; "
                    + "cierre y vuelva a abrir el sistema para que se guarde. El nombre, la descripción y los "
                    + "precios sí se pueden cambiar.");
        } else if (abiertas > 0) {
            lblAvisoCotizaciones.setText((abiertas == 1 ? "1 cotización abierta conserva lo que se le vendió"
                    : abiertas + " cotizaciones abiertas conservan lo que se les vendió")
                    + " (precio y servicios). Los cambios aplican a las cotizaciones nuevas.");
        }
        boolean conAviso = serviciosBloqueados || abiertas > 0;
        lblAvisoCotizaciones.setVisible(conAviso);
        lblAvisoCotizaciones.setManaged(conAviso);
        boxAgregarServicio.setDisable(serviciosBloqueados);
        tablaServicios.refresh();
    }

    private void aplicarModoPrecio() {
        boolean tabla = rbTablaPrecios.isSelected();
        txtPrecio.setVisible(!tabla);
        boxTablaPrecios.setVisible(tabla);
        lblResumenTabla.setText(TablaPreciosPaquete.resumen(tablaPrecios));
    }

    @FXML
    private void editarTablaPrecios() {
        EditorTablaPrecios.mostrar(txtNombre.getScene().getWindow(),
                consultarServiciosUseCase.obtenerComidas(), tablaPrecios)
            .ifPresent(nueva -> {
                tablaPrecios = nueva;
                aplicarModoPrecio();
            });
    }

    @FXML
    private void agregarServicio() {
        ServicioDTO seleccionado = cmbServicios.getSelectionModel().getSelectedItem();
        String cantText = txtCantidad.getText().trim();

        if (seleccionado == null) {
            mostrarError("Debe seleccionar un servicio.");
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(cantText);
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            mostrarError("La cantidad debe ser un número entero positivo.");
            return;
        }

        boolean existe = false;
        for (PaqueteServicioDTO ps : listaServicios) {
            if (ps.getServicio().getId().equals(seleccionado.getId())) {
                ps.setCantidad(ps.getCantidad() + cantidad);
                ps.setSubtotal(ps.getCantidad() * ps.getServicio().getPrecio());
                existe = true;
                break;
            }
        }

        if (!existe) {
            PaqueteServicioDTO nuevo = new PaqueteServicioDTO();
            nuevo.setServicio(seleccionado);
            nuevo.setCantidad(cantidad);
            nuevo.setSubtotal(cantidad * seleccionado.getPrecio());
            listaServicios.add(nuevo);
        }

        tablaServicios.refresh();
        cmbServicios.getSelectionModel().clearSelection();
        txtCantidad.setText("");
    }

    @FXML
    private void guardarPaquete() {
        String nombre = txtNombre.getText().trim();
        String desc = txtDescripcion.getText().trim();
        boolean conTabla = rbTablaPrecios.isSelected();

        if (nombre.isEmpty()) {
            mostrarError("El nombre del paquete es obligatorio.");
            return;
        }

        double precioFijo = 0;
        if (conTabla) {
            if (tablaPrecios.isEmpty()) {
                mostrarError("Capture la tabla de precios con el botón «Editar tabla…».");
                return;
            }
        } else {
            try {
                precioFijo = Double.parseDouble(txtPrecio.getText().trim());
            } catch (NumberFormatException e) {
                mostrarError("Capture el precio del paquete.");
                return;
            }
            if (precioFijo <= 0) {
                mostrarError("El precio del paquete debe ser mayor a cero.");
                return;
            }
        }

        // Se arma un DTO nuevo: si el servidor rechaza los cambios, el paquete de la lista no se altera.
        PaqueteDTO datos = new PaqueteDTO();
        datos.setId(paqueteAEditar != null ? paqueteAEditar.getId() : null);
        datos.setNombre(nombre);
        datos.setDescripcion(desc);
        datos.setCostoBase(conTabla ? 0 : precioFijo);
        datos.setPrecios(conTabla ? new ArrayList<>(tablaPrecios) : new ArrayList<>());
        // null = sin cambios: con cotizaciones abiertas los servicios no se tocan.
        datos.setServicios(serviciosBloqueados ? null : new ArrayList<>(listaServicios));

        try {
            paqueteCreado = paqueteAEditar == null
                ? registrarPaqueteUseCase.registrarPaquete(datos)
                : actualizarPaqueteUseCase.actualizarPaquete(datos);
            cerrarModal();
        } catch (CotizacionException e) {
            mostrarError(e.getMessage());
        }
    }

    private static List<PaquetePrecioDTO> copiarPrecios(List<PaquetePrecioDTO> precios) {
        List<PaquetePrecioDTO> copia = new ArrayList<>();
        if (precios != null) {
            for (PaquetePrecioDTO p : precios) {
                copia.add(new PaquetePrecioDTO(p.getId(), p.getComidaId(), p.getComidaNombre(), p.getPersonas(), p.getPrecio()));
            }
        }
        return copia;
    }

    @FXML
    private void cerrarModal() {
        Stage stage = (Stage) txtNombre.getScene().getWindow();
        stage.close();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Validación");
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
