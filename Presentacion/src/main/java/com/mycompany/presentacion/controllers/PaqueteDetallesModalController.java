package com.mycompany.presentacion.controllers;

import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.dtos.PaqueteServicioDTO;
import com.mycompany.common.dtos.ServicioDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class PaqueteDetallesModalController {

    // Etiquetas Info Básica
    @FXML private Label lblNombre;
    @FXML private Label lblCostoBase;
    @FXML private Label lblDescripcion;
    @FXML private javafx.scene.layout.VBox boxTablaPrecios;
    @FXML private javafx.scene.layout.VBox contenedorTablaPrecios;

    // Tabla
    @FXML private TableView<PaqueteServicioDTO> tablaServicios;
    @FXML private TableColumn<PaqueteServicioDTO, String> colServicioNombre;
    @FXML private TableColumn<PaqueteServicioDTO, Double> colPrecioUnitario;
    @FXML private TableColumn<PaqueteServicioDTO, Integer> colCantidad;
    @FXML private TableColumn<PaqueteServicioDTO, Double> colSubtotal;

    private PaqueteDTO paquete;
    private final ObservableList<PaqueteServicioDTO> listaServicios = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Configurar Tabla
        colServicioNombre.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getServicio().getNombre()));
        
        colPrecioUnitario.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getServicio().getPrecio()));
            
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        colPrecioUnitario.setCellFactory(c -> celdaDinero());
        colSubtotal.setCellFactory(c -> celdaDinero());
        
        tablaServicios.setItems(listaServicios);
    }

    public void setPaquete(PaqueteDTO paquete) {
        this.paquete = paquete;
        
        // Llenar labels
        lblNombre.setText(paquete.getNombre());
        lblCostoBase.setText(com.mycompany.presentacion.utils.TablaPreciosPaquete.precioParaMostrar(paquete));
        boolean conTabla = paquete.isTablaPrecios();
        boxTablaPrecios.setVisible(conTabla);
        boxTablaPrecios.setManaged(conTabla);
        contenedorTablaPrecios.getChildren().setAll(
                com.mycompany.presentacion.utils.TablaPreciosPaquete.soloLectura(paquete.getPrecios()));
        lblDescripcion.setText(paquete.getDescripcion() != null ? paquete.getDescripcion() : "-");

        // Llenar tabla
        listaServicios.clear();
        if (paquete.getServicios() != null) {
            listaServicios.addAll(paquete.getServicios());
        }
    }

    private static TableCell<PaqueteServicioDTO, Double> celdaDinero() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : com.mycompany.presentacion.utils.TablaPreciosPaquete.dinero(item));
            }
        };
    }

    @FXML
    private void cerrarModal() {
        Stage stage = (Stage) lblNombre.getScene().getWindow();
        stage.close();
    }
}
