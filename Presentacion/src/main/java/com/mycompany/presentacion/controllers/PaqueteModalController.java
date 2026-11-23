package com.mycompany.presentacion.controllers;

import com.example.negocio.catalogo.usecase.ConsultarServiciosUseCase;
import com.example.negocio.paquete.usecase.ActualizarPaqueteUseCase;
import com.example.negocio.paquete.usecase.RegistrarPaqueteUseCase;
import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.dtos.PaqueteServicioDTO;
import com.mycompany.common.dtos.ServicioDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class PaqueteModalController {

    private final RegistrarPaqueteUseCase registrarPaqueteUseCase;
    private final ActualizarPaqueteUseCase actualizarPaqueteUseCase;
    private final ConsultarServiciosUseCase consultarServiciosUseCase;

    // Columna Izquierda
    @FXML private Label lblTitulo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtPrecio;
    @FXML private TextArea txtDescripcion;

    // Columna Derecha
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
    
    private final ObservableList<PaqueteServicioDTO> listaServicios = FXCollections.observableArrayList();
    private final ObservableList<ServicioDTO> catalogoServicios = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (txtPrecio != null) {
            txtPrecio.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
                if (change.getControlNewText().matches("\\d*(\\.\\d*)?")) return change;
                return null;
            }));
        }
        if (txtCantidad != null) {
            txtCantidad.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
                if (change.getControlNewText().matches("\\d*")) return change;
                return null;
            }));
        }
        // Reset singleton state so a new modal open starts clean
        paqueteAEditar = null;
        paqueteCreado = null;
        txtNombre.setText("");
        txtPrecio.setText("");
        txtDescripcion.setText("");
        lblTitulo.setText("Nuevo Paquete");
        listaServicios.clear();

        // Cargar catálogo de servicios (clear first to avoid duplicates)
        catalogoServicios.clear();
        List<ServicioDTO> servicios = consultarServiciosUseCase.obtenerTodos();
        if (servicios != null) {
            catalogoServicios.addAll(servicios);
        }
        cmbServicios.setItems(catalogoServicios);
        
        cmbServicios.setConverter(new StringConverter<ServicioDTO>() {
            @Override
            public String toString(ServicioDTO object) {
                return object == null ? "" : object.getNombre() + " ($" + object.getPrecio() + ")";
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

        configurarColumnaEliminar();
        
        tablaServicios.setItems(listaServicios);
    }

    private void configurarColumnaEliminar() {
        Callback<TableColumn<PaqueteServicioDTO, Void>, TableCell<PaqueteServicioDTO, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<PaqueteServicioDTO, Void> call(final TableColumn<PaqueteServicioDTO, Void> param) {
                return new TableCell<>() {
                    private final Button btn = new Button("X");

                    {
                        btn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                        btn.setOnAction((event) -> {
                            PaqueteServicioDTO ps = getTableView().getItems().get(getIndex());
                            listaServicios.remove(ps);
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
            }
        };
        colEliminar.setCellFactory(cellFactory);
    }

    public void setPaqueteAEditar(PaqueteDTO paquete) {
        this.paqueteAEditar = paquete;
        lblTitulo.setText("Editar Paquete");

        txtNombre.setText(paquete.getNombre());
        txtPrecio.setText(String.valueOf(paquete.getCostoBase()));
        txtDescripcion.setText(paquete.getDescripcion() != null ? paquete.getDescripcion() : "");
        
        listaServicios.clear();
        if (paquete.getServicios() != null) {
            listaServicios.addAll(paquete.getServicios());
        }
    }

    @FXML
    private void agregarServicio() {
        ServicioDTO seleccionado = cmbServicios.getSelectionModel().getSelectedItem();
        String cantText = txtCantidad.getText().trim();

        if (seleccionado == null) {
            mostrarError("Debe seleccionar un servicio.");
            return;
        }

        int cantidad = 0;
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
        String precioStr = txtPrecio.getText().trim();
        String desc = txtDescripcion.getText().trim();

        if (nombre.isEmpty() || precioStr.isEmpty()) {
            mostrarError("El nombre y el precio base son obligatorios.");
            return;
        }

        double precioBase;
        try {
            precioBase = Double.parseDouble(precioStr);
            if (precioBase < 0) {
                mostrarError("El precio base no puede ser negativo.");
                return;
            }
        } catch (NumberFormatException e) {
            mostrarError("El precio base debe ser un número válido.");
            return;
        }

        try {
            if (paqueteAEditar == null) {
                // Modo Crear
                PaqueteDTO nuevo = new PaqueteDTO();
                nuevo.setNombre(nombre);
                nuevo.setCostoBase(precioBase);
                nuevo.setDescripcion(desc);
                nuevo.setServicios(new ArrayList<>(listaServicios));

                paqueteCreado = registrarPaqueteUseCase.registrarPaquete(nuevo);
            } else {
                // Modo Editar
                paqueteAEditar.setNombre(nombre);
                paqueteAEditar.setCostoBase(precioBase);
                paqueteAEditar.setDescripcion(desc);
                paqueteAEditar.setServicios(new ArrayList<>(listaServicios));

                paqueteCreado = actualizarPaqueteUseCase.actualizarPaquete(paqueteAEditar);
            }

            cerrarModal();

        } catch (CotizacionException e) {
            mostrarError(e.getMessage());
        }
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
