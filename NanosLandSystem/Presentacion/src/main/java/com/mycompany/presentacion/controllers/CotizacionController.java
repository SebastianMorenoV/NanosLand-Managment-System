package com.mycompany.presentacion.controllers;

import com.example.negocio.catalogo.usecase.ConsultarCatalogoUseCase;
import com.example.negocio.cliente.usecase.BuscarClienteUseCase;
import com.mycompany.common.dtos.ClienteDTO;
import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.presentacion.utils.ViewSwitcher;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.controlsfx.control.SearchableComboBox;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CotizacionController {

    private final BuscarClienteUseCase buscarClienteUseCase;
    private final ConsultarCatalogoUseCase consultarCatalogoUseCase;

    @FXML
    private SearchableComboBox<PaqueteDTO> comboPaquetes;
    @FXML
    private Label lblNombrePaquete;
    @FXML
    private Label lblPrecioPaquete;
    @FXML
    private Label lblDetallesPaquete;

    @FXML
    private SearchableComboBox<ClienteDTO> comboClientes;

    @FXML
    public void initialize() {
        configurarComboClientes();
        configurarComboPaquetes();
    }

    private void configurarComboClientes() {
        List<ClienteDTO> clienteDTOS = buscarClienteUseCase.obtenerTodos();
        ObservableList<ClienteDTO> listaClientes = FXCollections.observableArrayList(clienteDTOS);

        // 1. Cómo se ve el cliente cuando ya está seleccionado (Texto plano en la barra)
        comboClientes.setConverter(new StringConverter<ClienteDTO>() {
            @Override
            public String toString(ClienteDTO cliente) {
                if (cliente == null) return "";

                // Obtenemos el teléfono (protegiéndonos de los nulos por si un cliente no tiene)
                String telefono = cliente.getTelefono() != null ? cliente.getTelefono() : "";

                // Al unir el nombre y el teléfono aquí, ControlsFX buscará en ambos a la vez.
                // Además, así es como se verá el texto en la cajita una vez que lo selecciones.
                return cliente.getNombre() + " - " + telefono;
            }

            @Override
            public ClienteDTO fromString(String string) {
                return null;
            }
        });

        // 2. LA MAGIA: Cómo se ven los clientes en la lista desplegable (Dos líneas)
        comboClientes.setCellFactory(listView -> new ListCell<ClienteDTO>() {
            @Override
            protected void updateItem(ClienteDTO cliente, boolean empty) {
                super.updateItem(cliente, empty);

                if (empty || cliente == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    // Contenedor vertical
                    VBox contenedor = new VBox(2);

                    // Nombre del cliente
                    Label lblNombre = new Label(cliente.getNombre());
                    lblNombre.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");

                    // Teléfono del cliente
                    // Asumo que tu ClienteDTO tiene el método getTelefono(). Si se llama distinto, cámbialo aquí.
                    String telefono = cliente.getTelefono() != null ? cliente.getTelefono() : "Sin teléfono";
                    Label lblDetalles = new Label("📞 " + telefono);
                    lblDetalles.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

                    contenedor.getChildren().addAll(lblNombre, lblDetalles);
                    setGraphic(contenedor);
                    setText(null); // Borramos el texto por defecto para usar nuestro diseño
                }
            }
        });

        comboClientes.setItems(listaClientes);
    }

    private void configurarComboPaquetes() {
        // Obtenemos los paquetes reales de la Base de Datos
        List<PaqueteDTO> paqueteDTOS = consultarCatalogoUseCase.obtenerTodosLosPaquetes();
        ObservableList<PaqueteDTO> listaPaquetes = FXCollections.observableArrayList(paqueteDTOS);

        comboPaquetes.setItems(listaPaquetes);

        comboPaquetes.setConverter(new StringConverter<PaqueteDTO>() {
            @Override
            public String toString(PaqueteDTO paquete) {
                return paquete == null ? "" : paquete.getNombre();
            }

            @Override
            public PaqueteDTO fromString(String string) {
                return null;
            }
        });

        // Acción al seleccionar un paquete
        comboPaquetes.setOnAction(event -> {
            PaqueteDTO paqueteSeleccionado = comboPaquetes.getValue();
            if (paqueteSeleccionado != null) {
                lblNombrePaquete.setText(paqueteSeleccionado.getNombre());
                lblPrecioPaquete.setText("$" + paqueteSeleccionado.getCostoBase());

                // Asumo que el DTO tiene getDescripcion(), si no, ajusta el nombre del getter
                String detalles = paqueteSeleccionado.getDescripcion() != null ? paqueteSeleccionado.getDescripcion() : "Sin detalles adicionales";
                lblDetallesPaquete.setText(detalles);
            }
        });
    }

    @FXML
    private void abrirModalNuevoCliente() {
        try {
            // 1. Preparamos el cargador de FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/presentacion/views/NuevoClienteModal.fxml"));

            // 2. ¡ESTO ES LO IMPORTANTE!
            // Usamos el contexto de Spring que ya tienes guardado en ViewSwitcher
            // para que Spring gestione el nuevo controlador.
            loader.setControllerFactory(ViewSwitcher.getSpringContext()::getBean);

            Parent root = loader.load();

            // 3. Crear y configurar la ventana (Stage)
            Stage modalStage = new Stage();
            modalStage.setTitle("Registrar Nuevo Cliente");
            modalStage.initModality(Modality.APPLICATION_MODAL); // Bloquea la ventana de atrás
            modalStage.initStyle(StageStyle.UTILITY); // Estilo de ventana de diálogo
            modalStage.setScene(new Scene(root));
            modalStage.setResizable(false);

            // 4. Mostrar y esperar
            modalStage.showAndWait();

            // 5. Al cerrar, refrescamos el combo de clientes por si se agregó uno
            configurarComboClientes();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}