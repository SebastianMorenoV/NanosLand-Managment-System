package com.mycompany.presentacion.controllers;

import com.example.negocio.paquete.usecase.ConsultarPaquetesUseCase;
import com.example.negocio.paquete.usecase.EliminarPaqueteUseCase;
import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.PaqueteDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Pagination;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import javafx.util.Callback;
import javafx.scene.control.TableCell;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class PaquetesController {

    private final ConsultarPaquetesUseCase consultarPaquetesUseCase;
    private final EliminarPaqueteUseCase eliminarPaqueteUseCase;

    @FXML private TextField txtBuscar;
    @FXML private TableView<PaqueteDTO> tablaPaquetes;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    
    @FXML private TableColumn<PaqueteDTO, Long> colId;
    @FXML private TableColumn<PaqueteDTO, String> colNombre;
    @FXML private TableColumn<PaqueteDTO, Double> colPrecio;
    @FXML private TableColumn<PaqueteDTO, String> colDescripcion;
    @FXML private TableColumn<PaqueteDTO, Void> colAcciones;
    @FXML private Pagination paginacion;

    private static final int ITEMS_POR_PAGINA = 20;

    private final ObservableList<PaqueteDTO> masterData = FXCollections.observableArrayList();
    private FilteredList<PaqueteDTO> filteredData;
    private SortedList<PaqueteDTO> sortedData;

    @FXML
    public void initialize() {
        if (tablaPaquetes != null) {
            tablaPaquetes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        }

        tablaPaquetes.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            if (btnEditar != null) btnEditar.setDisable(!hasSelection);
            if (btnEliminar != null) btnEliminar.setDisable(!hasSelection);
        });

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("costoBase"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        configurarColumnaAcciones();

        configurarBuscadorYPaginacion();
        cargarPaquetes();
    }

    private void configurarColumnaAcciones() {
        Callback<TableColumn<PaqueteDTO, Void>, TableCell<PaqueteDTO, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<PaqueteDTO, Void> call(final TableColumn<PaqueteDTO, Void> param) {
                return new TableCell<>() {
                    private final Button btn = new Button("Detalles");

                    {
                        btn.getStyleClass().add("boton-azul-claro");
                        btn.setOnAction((event) -> {
                            PaqueteDTO paquete = getTableView().getItems().get(getIndex());
                            abrirModalDetalles(paquete);
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
        colAcciones.setCellFactory(cellFactory);
    }

    private void cargarPaquetes() {
        List<PaqueteDTO> listaPaquetes = consultarPaquetesUseCase.obtenerPaquetesActivos();
        
        masterData.clear();
        if (listaPaquetes != null) {
            masterData.addAll(listaPaquetes);
        }
    }

    private void configurarBuscadorYPaginacion() {
        filteredData = new FilteredList<>(masterData, p -> true);

        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(paquete -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String filtro = newValue.toLowerCase();

                if (paquete.getNombre() != null && paquete.getNombre().toLowerCase().contains(filtro)) {
                    return true;
                } else if (paquete.getDescripcion() != null && paquete.getDescripcion().toLowerCase().contains(filtro)) {
                    return true;
                }
                
                return false;
            });
        });

        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaPaquetes.comparatorProperty());
        
        filteredData.addListener((javafx.collections.ListChangeListener.Change<? extends PaqueteDTO> c) -> {
            actualizarPaginacion();
        });

        paginacion.setPageCount(1);
        paginacion.setPageFactory(this::crearPagina);
    }

    private javafx.scene.Node crearPagina(int pageIndex) {
        int fromIndex = pageIndex * ITEMS_POR_PAGINA;
        int toIndex = Math.min(fromIndex + ITEMS_POR_PAGINA, sortedData.size());
        
        if (fromIndex < sortedData.size() && fromIndex <= toIndex) {
            tablaPaquetes.setItems(FXCollections.observableArrayList(sortedData.subList(fromIndex, toIndex)));
        } else {
            tablaPaquetes.setItems(FXCollections.observableArrayList());
        }
        return tablaPaquetes;
    }

    private void actualizarPaginacion() {
        int pageCount = (int) Math.ceil((double) sortedData.size() / ITEMS_POR_PAGINA);
        paginacion.setPageCount(pageCount == 0 ? 1 : pageCount);
        paginacion.setCurrentPageIndex(0);
        crearPagina(0);
    }

    @FXML
    private void nuevoPaquete() {
        abrirModalPaquete(null);
    }

    @FXML
    private void editarPaquete() {
        PaqueteDTO seleccionado = tablaPaquetes.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            abrirModalPaquete(seleccionado);
        }
    }

    @FXML
    private void eliminarPaquete() {
        PaqueteDTO seleccionado = tablaPaquetes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminación");
        alert.setHeaderText("¿Estás seguro de eliminar el paquete " + seleccionado.getNombre() + "?");
        alert.setContentText("Esta acción no se puede deshacer.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                eliminarPaqueteUseCase.eliminarPaqueteLogico(seleccionado.getId());
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

    private void abrirModalPaquete(PaqueteDTO paqueteEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/presentacion/views/PaqueteModal.fxml"));
            loader.setControllerFactory(com.mycompany.presentacion.utils.ViewSwitcher.getSpringContext()::getBean);
            Parent root = loader.load();

            PaqueteModalController controller = loader.getController();
            if (paqueteEditar != null) {
                controller.setPaqueteAEditar(paqueteEditar);
            }

            Stage stage = new Stage();
            stage.setTitle(paqueteEditar == null ? "Nuevo Paquete" : "Editar Paquete");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            if (controller.getPaqueteCreado() != null) {
                cargarPaquetes();
            }
        } catch (IOException e) {
            e.printStackTrace();
            mostrarErrorAbrirModal();
        }
    }

    private void abrirModalDetalles(PaqueteDTO paquete) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/presentacion/views/PaqueteDetallesModal.fxml"));
            loader.setControllerFactory(com.mycompany.presentacion.utils.ViewSwitcher.getSpringContext()::getBean);
            Parent root = loader.load();

            PaqueteDetallesModalController controller = loader.getController();
            controller.setPaquete(paquete);

            Stage stage = new Stage();
            stage.setTitle("Detalles del Paquete");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            // Si se modificaron los servicios o algo, refrescamos la tabla
            cargarPaquetes();
        } catch (IOException e) {
            e.printStackTrace();
            mostrarErrorAbrirModal();
        }
    }

    private void mostrarErrorAbrirModal() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("No se pudo abrir la ventana");
        alert.showAndWait();
    }
}
