package com.mycompany.presentacion.controllers;

import com.example.negocio.cliente.usecase.BuscarClienteUseCase;
import com.mycompany.common.dtos.ClienteDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class UsuariosController {

    // Inyectamos el caso de uso para traer los clientes
    private final BuscarClienteUseCase buscarClienteUseCase;

    @FXML private TextField txtBuscar;
    @FXML private TableView<ClienteDTO> tablaUsuarios;
    
    // Enlazamos las columnas creadas en el FXML
    @FXML private TableColumn<ClienteDTO, Long> colId;
    @FXML private TableColumn<ClienteDTO, String> colNombre;
    @FXML private TableColumn<ClienteDTO, String> colTelefono;
    @FXML private TableColumn<ClienteDTO, String> colCorreo;
    @FXML private TableColumn<ClienteDTO, String> colUltimoEvento; 

    // Lista maestra donde guardaremos todos los clientes traídos de la BD
    private final ObservableList<ClienteDTO> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (tablaUsuarios != null) {
            tablaUsuarios.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        }

        // 1. Decirle a cada columna de dónde sacar la información (deben coincidir con las variables de ClienteDTO)
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        // Nota: colUltimoEvento se quedará vacía de momento ya que tu DTO no tiene ese dato.

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
        
        // Asignamos la lista a la tabla
        tablaUsuarios.setItems(masterData);
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
}