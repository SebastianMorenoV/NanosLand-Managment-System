package com.mycompany.presentacion.controllers;

import com.example.negocio.catalogo.usecase.GestionarServicioUseCase;
import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.ServicioDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ServiciosController {

    private final GestionarServicioUseCase gestionarServicioUseCase;

    @FXML private TextField txtBuscar;
    @FXML private TableView<ServicioDTO> tablaServicios;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;

    @FXML private TableColumn<ServicioDTO, Long> colId;
    @FXML private TableColumn<ServicioDTO, String> colNombre;
    @FXML private TableColumn<ServicioDTO, String> colDescripcion;
    @FXML private TableColumn<ServicioDTO, Double> colPrecio;
    @FXML private Pagination paginacion;

    private static final int ITEMS_POR_PAGINA = 20;

    private final ObservableList<ServicioDTO> masterData = FXCollections.observableArrayList();
    private FilteredList<ServicioDTO> filteredData;
    private SortedList<ServicioDTO> sortedData;

    @FXML
    public void initialize() {
        if (tablaServicios != null) {
            tablaServicios.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        }

        tablaServicios.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            if (btnEditar != null) btnEditar.setDisable(!hasSelection);
            if (btnEliminar != null) btnEliminar.setDisable(!hasSelection);
        });

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));

        // Formatear columna de precio como moneda
        colPrecio.setCellFactory(column -> new TableCell<ServicioDTO, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("$%,.2f", item));
                }
            }
        });

        configurarBuscadorYPaginacion();
        cargarServicios();
    }

    private void cargarServicios() {
        List<ServicioDTO> listaServicios = gestionarServicioUseCase.obtenerServiciosActivos();
        masterData.clear();
        if (listaServicios != null) {
            masterData.addAll(listaServicios);
        }
    }

    private void configurarBuscadorYPaginacion() {
        filteredData = new FilteredList<>(masterData, p -> true);

        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(servicio -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String filtro = newValue.toLowerCase();

                if (servicio.getNombre() != null && servicio.getNombre().toLowerCase().contains(filtro)) {
                    return true;
                } else if (servicio.getDescripcion() != null && servicio.getDescripcion().toLowerCase().contains(filtro)) {
                    return true;
                }

                return false;
            });
            actualizarPaginacion();
        });

        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaServicios.comparatorProperty());

        filteredData.addListener((javafx.collections.ListChangeListener.Change<? extends ServicioDTO> c) -> {
            actualizarPaginacion();
        });

        paginacion.setPageCount(1);
        paginacion.setPageFactory(this::crearPagina);
    }

    private javafx.scene.Node crearPagina(int pageIndex) {
        int fromIndex = pageIndex * ITEMS_POR_PAGINA;
        int toIndex = Math.min(fromIndex + ITEMS_POR_PAGINA, sortedData.size());

        if (fromIndex < sortedData.size() && fromIndex <= toIndex) {
            tablaServicios.setItems(FXCollections.observableArrayList(sortedData.subList(fromIndex, toIndex)));
        } else {
            tablaServicios.setItems(FXCollections.observableArrayList());
        }
        return tablaServicios;
    }

    private void actualizarPaginacion() {
        int pageCount = (int) Math.ceil((double) sortedData.size() / ITEMS_POR_PAGINA);
        paginacion.setPageCount(pageCount == 0 ? 1 : pageCount);
        paginacion.setCurrentPageIndex(0);
        crearPagina(0);
    }

    @FXML
    private void nuevoServicio() {
        abrirModalServicio(null);
    }

    @FXML
    private void editarServicio() {
        ServicioDTO seleccionado = tablaServicios.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            abrirModalServicio(seleccionado);
        }
    }

    @FXML
    private void eliminarServicio() {
        ServicioDTO seleccionado = tablaServicios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminación");
        alert.setHeaderText("¿Estás seguro de eliminar el servicio \"" + seleccionado.getNombre() + "\"?");
        alert.setContentText("El servicio será desactivado y ya no estará disponible en cotizaciones.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                gestionarServicioUseCase.eliminarServicioLogico(seleccionado.getId());
                masterData.remove(seleccionado);
            } catch (CotizacionException e) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error al eliminar");
                errorAlert.setHeaderText(null);
                errorAlert.setContentText(e.getMessage());
                errorAlert.showAndWait();
            }
        }
    }

    private void abrirModalServicio(ServicioDTO servicioEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/presentacion/views/ServicioModal.fxml"));
            loader.setControllerFactory(com.mycompany.presentacion.utils.ViewSwitcher.getSpringContext()::getBean);
            Parent root = loader.load();

            ServicioModalController controller = loader.getController();
            if (servicioEditar != null) {
                controller.setServicioAEditar(servicioEditar);
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            com.mycompany.presentacion.utils.ModalHelper.mostrarModal(root,
                    servicioEditar == null ? "Nuevo Servicio" : "Editar Servicio", stage);

            if (controller.isGuardado()) {
                cargarServicios();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se pudo abrir la ventana");
            alert.showAndWait();
        }
    }
}
