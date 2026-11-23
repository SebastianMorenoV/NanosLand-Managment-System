package com.mycompany.presentacion.controllers;

import com.example.negocio.cliente.usecase.BuscarClienteUseCase;
import com.example.negocio.cliente.usecase.EliminarClienteUseCase;
import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.ClienteDTO;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import java.io.IOException;
import java.util.Optional;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class UsuariosController {

    // Inyectamos el caso de uso para traer los clientes
    private final BuscarClienteUseCase buscarClienteUseCase;
    private final EliminarClienteUseCase eliminarClienteUseCase;

    @FXML private TextField txtBuscar;
    @FXML private TableView<ClienteDTO> tablaUsuarios;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    
    // Enlazamos las columnas creadas en el FXML
    @FXML private TableColumn<ClienteDTO, Long> colId;
    @FXML private TableColumn<ClienteDTO, String> colNombre;
    @FXML private TableColumn<ClienteDTO, String> colTelefono;
    @FXML private TableColumn<ClienteDTO, String> colCorreo;
    @FXML private TableColumn<ClienteDTO, String> colCiudad; 
    @FXML private TableColumn<ClienteDTO, String> colColonia;
    @FXML private TableColumn<ClienteDTO, String> colCalle;
    @FXML private TableColumn<ClienteDTO, String> colCodigoPostal;
    @FXML private Pagination paginacion;

    private static final int ITEMS_POR_PAGINA = 20;

    // Lista maestra donde guardaremos todos los clientes traídos de la BD
    private final ObservableList<ClienteDTO> masterData = FXCollections.observableArrayList();
    private FilteredList<ClienteDTO> filteredData;
    private SortedList<ClienteDTO> sortedData;

    @FXML
    public void initialize() {
        if (tablaUsuarios != null) {
            tablaUsuarios.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        }

        // Listener para habilitar botones de edición y eliminación
        tablaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            if (btnEditar != null) btnEditar.setDisable(!hasSelection);
            if (btnEliminar != null) btnEliminar.setDisable(!hasSelection);
        });

        // 1. Decirle a cada columna de dónde sacar la información (deben coincidir con las variables de ClienteDTO)
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colCiudad.setCellValueFactory(new PropertyValueFactory<>("ciudad"));
        colColonia.setCellValueFactory(new PropertyValueFactory<>("colonia"));
        colCalle.setCellValueFactory(new PropertyValueFactory<>("calle"));
        colCodigoPostal.setCellValueFactory(new PropertyValueFactory<>("codigoPostal"));

        // 2. Configurar la barra de búsqueda en tiempo real y paginación
        configurarBuscadorYPaginacion();

        // 3. Cargar los datos desde la BD
        cargarClientes();
    }

    private void cargarClientes() {
        // Obtenemos los DTOs usando tu caso de uso
        List<ClienteDTO> listaClientes = buscarClienteUseCase.obtenerTodos();
        
        masterData.clear();
        if (listaClientes != null) {
            masterData.addAll(listaClientes);
        }
    }

    private void configurarBuscadorYPaginacion() {
        // Envolvemos la ObservableList en una FilteredList
        filteredData = new FilteredList<>(masterData, p -> true);

        // Agregamos un "listener" (escuchador) al cuadro de texto de búsqueda
        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(cliente -> {
                // Si el buscador está vacío, mostramos todos los clientes
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                // Pasamos a minúsculas para que la búsqueda ignore mayúsculas
                String filtro = newValue.toLowerCase();

                // Buscamos coincidencias en Nombre, Teléfono o Correo
                if (cliente.getNombre() != null && cliente.getNombre().toLowerCase().contains(filtro)) {
                    return true;
                } else if (cliente.getTelefono() != null && cliente.getTelefono().toLowerCase().contains(filtro)) {
                    return true;
                } else if (cliente.getCorreo() != null && cliente.getCorreo().toLowerCase().contains(filtro)) {
                    return true;
                }
                
                return false; // No hay coincidencias
            });
        });

        // Envolvemos la FilteredList en una SortedList para mantener la capacidad de ordenar las columnas dando clic en ellas
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaUsuarios.comparatorProperty());
        
        filteredData.addListener((javafx.collections.ListChangeListener.Change<? extends ClienteDTO> c) -> {
            actualizarPaginacion();
        });

        paginacion.setPageCount(1);
        paginacion.setPageFactory(this::crearPagina);
    }

    private javafx.scene.Node crearPagina(int pageIndex) {
        int fromIndex = pageIndex * ITEMS_POR_PAGINA;
        int toIndex = Math.min(fromIndex + ITEMS_POR_PAGINA, sortedData.size());
        
        if (fromIndex < sortedData.size() && fromIndex <= toIndex) {
            tablaUsuarios.setItems(FXCollections.observableArrayList(sortedData.subList(fromIndex, toIndex)));
        } else {
            tablaUsuarios.setItems(FXCollections.observableArrayList());
        }
        return tablaUsuarios;
    }

    private void actualizarPaginacion() {
        int pageCount = (int) Math.ceil((double) sortedData.size() / ITEMS_POR_PAGINA);
        paginacion.setPageCount(pageCount == 0 ? 1 : pageCount);
        paginacion.setCurrentPageIndex(0);
        crearPagina(0);
    }

    @FXML
    private void nuevoCliente() {
        abrirModalCliente(null);
    }

    @FXML
    private void editarCliente() {
        ClienteDTO seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            abrirModalCliente(seleccionado);
        }
    }

    @FXML
    private void eliminarCliente() {
        ClienteDTO seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminación");
        alert.setHeaderText("¿Estás seguro de eliminar el cliente " + seleccionado.getNombre() + "?");
        alert.setContentText("Esta acción no se puede deshacer.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                eliminarClienteUseCase.eliminarCliente(seleccionado.getId());
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

    private void abrirModalCliente(ClienteDTO clienteEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/presentacion/views/NuevoClienteModal.fxml"));
            // Importante: setControllerFactory le dice a FXMLLoader que use Spring para crear los controladores
            loader.setControllerFactory(com.mycompany.presentacion.utils.ViewSwitcher.getSpringContext()::getBean);
            Parent root = loader.load();

            NuevoClienteModalController controller = loader.getController();
            if (clienteEditar != null) {
                controller.setClienteAEditar(clienteEditar);
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            com.mycompany.presentacion.utils.ModalHelper.mostrarModal(root,
                    clienteEditar == null ? "Nuevo Cliente" : "Editar Cliente", stage);

            // Si se guardó correctamente, recargar la tabla
            if (controller.getClienteCreado() != null) {
                cargarClientes();
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