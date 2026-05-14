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

    // Lista maestra donde guardaremos todos los clientes traídos de la BD
    private final ObservableList<ClienteDTO> masterData = FXCollections.observableArrayList();

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

        // 2. Cargar los datos desde la BD
        cargarClientes();

        // 3. Configurar la barra de búsqueda en tiempo real
        configurarBuscador();
    }

    private void cargarClientes() {
        // Obtenemos los DTOs usando tu caso de uso
        List<ClienteDTO> listaClientes = buscarClienteUseCase.obtenerTodos();
        
        masterData.clear();
        if (listaClientes != null) {
            masterData.addAll(listaClientes);
        }
    }

    private void configurarBuscador() {
        // Envolvemos la ObservableList en una FilteredList
        FilteredList<ClienteDTO> filteredData = new FilteredList<>(masterData, p -> true);

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
        SortedList<ClienteDTO> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaUsuarios.comparatorProperty());
        
        // Metemos la lista final ordenada y filtrada a la tabla
        tablaUsuarios.setItems(sortedData);
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
            stage.setTitle(clienteEditar == null ? "Nuevo Cliente" : "Editar Cliente");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

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