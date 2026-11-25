package com.mycompany.presentacion.controllers;

import com.example.negocio.usuario.usecase.BuscarUsuarioSistemaUseCase;
import com.example.negocio.usuario.usecase.EliminarUsuarioSistemaUseCase;
import com.example.negocio.usuario.usecase.ActivarUsuarioSistemaUseCase;
import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.UsuarioSistemaDTO;
import com.mycompany.persistencia.enums.RolUsuario;
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
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class GestionUsuariosController {

    private final BuscarUsuarioSistemaUseCase buscarUsuarioUseCase;
    private final EliminarUsuarioSistemaUseCase eliminarUsuarioUseCase;
    private final ActivarUsuarioSistemaUseCase activarUsuarioUseCase;
    private final com.mycompany.presentacion.context.SesionContext sesionContext;

    @FXML private TextField txtBuscar;
    @FXML private TableView<UsuarioSistemaDTO> tablaUsuarios;
    @FXML private Button btnEditar;
    /** Desactivar / Activar (baja lógica). */
    @FXML private Button btnActivar;
    /** Eliminar definitivamente. */
    @FXML private Button btnEliminar;

    @FXML private TableColumn<UsuarioSistemaDTO, Long> colId;
    @FXML private TableColumn<UsuarioSistemaDTO, String> colCorreo;
    @FXML private TableColumn<UsuarioSistemaDTO, String> colTelefono;
    @FXML private TableColumn<UsuarioSistemaDTO, RolUsuario> colRol;
    @FXML private TableColumn<UsuarioSistemaDTO, String> colEstado;
    @FXML private Pagination paginacion;

    private static final int ITEMS_POR_PAGINA = 20;

    private final ObservableList<UsuarioSistemaDTO> masterData = FXCollections.observableArrayList();
    private FilteredList<UsuarioSistemaDTO> filteredData;
    private SortedList<UsuarioSistemaDTO> sortedData;

    @FXML
    public void initialize() {
        // El botón del menú solo se muestra al dueño; se comprueba aquí también para que
        // la pantalla no dependa de que el botón esté oculto.
        if (!sesionContext.esDueno()) {
            throw new IllegalStateException("Esta pantalla solo está disponible para el dueño.");
        }
        if (tablaUsuarios != null) {
            tablaUsuarios.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        }

        // Botones según el usuario elegido. Antes el de Activar/Desactivar quedaba bloqueado
        // para cualquier dueño, así que un dueño inactivo ya no se podía reactivar. La regla
        // real (que el sistema no se quede sin un dueño activo) la valida el caso de uso.
        tablaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            if (btnEditar != null) btnEditar.setDisable(!hasSelection);
            if (btnActivar != null) {
                btnActivar.setDisable(!hasSelection);
                boolean activar = hasSelection && !newSelection.isActivo();
                btnActivar.setText(activar ? "Activar" : "Desactivar");
                btnActivar.getStyleClass().setAll("button", activar ? "boton-verde" : "boton-rojo");
            }
            if (btnEliminar != null) {
                // Nadie puede eliminar su propia cuenta mientras tiene la sesión abierta.
                btnEliminar.setDisable(!hasSelection || esUsuarioEnSesion(newSelection));
            }
        });

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colEstado.setCellValueFactory(cellData -> {
            boolean activo = cellData.getValue().isActivo();
            return new javafx.beans.property.SimpleStringProperty(activo ? "Activo" : "Inactivo");
        });

        configurarBuscadorYPaginacion();
        cargarUsuarios();
    }

    private void cargarUsuarios() {
        List<UsuarioSistemaDTO> listaUsuarios = buscarUsuarioUseCase.obtenerTodos();
        masterData.clear();
        if (listaUsuarios != null) {
            masterData.addAll(listaUsuarios);
        }
    }

    private void configurarBuscadorYPaginacion() {
        filteredData = new FilteredList<>(masterData, p -> true);

        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(usuario -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String filtro = newValue.toLowerCase();
                if (usuario.getCorreo() != null && usuario.getCorreo().toLowerCase().contains(filtro)) {
                    return true;
                } else if (usuario.getTelefono() != null && usuario.getTelefono().toLowerCase().contains(filtro)) {
                    return true;
                }
                return false;
            });
        });

        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablaUsuarios.comparatorProperty());
        // Cada página es una copia: al ordenar por una columna hay que regenerarla, o las
        // páginas quedan mezcladas (filas repetidas en una y ausentes en otra).
        sortedData.comparatorProperty().addListener((obs, anterior, nuevo) ->
            crearPagina(paginacion.getCurrentPageIndex()));

        filteredData.addListener((javafx.collections.ListChangeListener.Change<? extends UsuarioSistemaDTO> c) -> {
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
    private void nuevoUsuario() {
        abrirModalUsuario(null);
    }

    @FXML
    private void editarUsuario() {
        UsuarioSistemaDTO seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            abrirModalUsuario(seleccionado);
        }
    }

    private boolean esUsuarioEnSesion(UsuarioSistemaDTO usuario) {
        return usuario != null && sesionContext.getUsuarioAutenticado() != null
                && java.util.Objects.equals(usuario.getId(), sesionContext.getUsuarioAutenticado().getId());
    }

    /** Elimina la cuenta definitivamente, después de confirmarlo. */
    @FXML
    private void eliminarUsuario() {
        UsuarioSistemaDTO seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION);
        confirmar.setTitle("Eliminar usuario");
        confirmar.setHeaderText("¿Eliminar definitivamente la cuenta " + seleccionado.getCorreo() + "?");
        confirmar.setContentText("La cuenta se borrará y no se podrá recuperar. Las cotizaciones y ventas "
                + "que registró se conservan.\n\nSi solo quiere impedir que inicie sesión, use «Desactivar».");
        ButtonType eliminar = new ButtonType("Eliminar definitivamente", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        confirmar.getButtonTypes().setAll(ButtonType.CANCEL, eliminar);
        com.mycompany.presentacion.utils.ModalHelper.aplicarEstilo(confirmar, tablaUsuarios.getScene().getWindow());
        // Acción que no se puede deshacer: botón rojo y Enter no la confirma por accidente.
        Button botonEliminar = (Button) confirmar.getDialogPane().lookupButton(eliminar);
        botonEliminar.getStyleClass().setAll("button", "boton-rojo");
        botonEliminar.setDefaultButton(false);
        ((Button) confirmar.getDialogPane().lookupButton(ButtonType.CANCEL)).setDefaultButton(true);

        Optional<ButtonType> respuesta = confirmar.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != eliminar) return;
        try {
            eliminarUsuarioUseCase.eliminarDefinitivamente(seleccionado.getId(),
                    sesionContext.getUsuarioAutenticado() != null ? sesionContext.getUsuarioAutenticado().getId() : null);
            masterData.remove(seleccionado);
            tablaUsuarios.getSelectionModel().clearSelection();
        } catch (CotizacionException e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("No se pudo eliminar");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText(e.getMessage());
            errorAlert.showAndWait();
        }
    }

    /** Desactiva o reactiva la cuenta (baja lógica), después de confirmarlo. */
    @FXML
    private void cambiarEstado() {
        UsuarioSistemaDTO seleccionado = tablaUsuarios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        boolean desactivando = seleccionado.isActivo();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar " + (desactivando ? "Desactivación" : "Activación"));
        alert.setHeaderText("¿Estás seguro de " + (desactivando ? "desactivar" : "activar") + " el usuario " + seleccionado.getCorreo() + "?");
        alert.setContentText(desactivando ? "El usuario será desactivado y no podrá iniciar sesión." : "El usuario será activado y podrá volver a iniciar sesión.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (desactivando) {
                    eliminarUsuarioUseCase.desactivarUsuario(seleccionado.getId());
                    seleccionado.setActivo(false);
                } else {
                    activarUsuarioUseCase.activarUsuario(seleccionado.getId());
                    seleccionado.setActivo(true);
                }
                tablaUsuarios.refresh();
                tablaUsuarios.getSelectionModel().clearSelection();
                tablaUsuarios.getSelectionModel().select(seleccionado);
            } catch (CotizacionException e) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle(desactivando ? "No se pudo desactivar" : "No se pudo activar");
                errorAlert.setHeaderText(null);
                errorAlert.setContentText(e.getMessage());
                errorAlert.showAndWait();
            }
        }
    }

    private void abrirModalUsuario(UsuarioSistemaDTO usuarioEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/presentacion/views/NuevoUsuarioSistemaModal.fxml"));
            loader.setControllerFactory(com.mycompany.presentacion.utils.ViewSwitcher.getSpringContext()::getBean);
            Parent root = loader.load();

            NuevoUsuarioSistemaModalController controller = loader.getController();
            if (usuarioEditar != null) {
                controller.setUsuarioAEditar(usuarioEditar);
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            com.mycompany.presentacion.utils.ModalHelper.mostrarModal(root,
                    usuarioEditar == null ? "Nuevo Usuario" : "Editar Usuario", stage);

            if (controller.getUsuarioCreado() != null) {
                cargarUsuarios();
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
