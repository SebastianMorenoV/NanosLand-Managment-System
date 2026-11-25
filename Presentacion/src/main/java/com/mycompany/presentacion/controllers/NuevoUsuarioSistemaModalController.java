package com.mycompany.presentacion.controllers;

import com.example.negocio.exception.CotizacionException;
import com.example.negocio.usuario.usecase.RegistrarUsuarioSistemaUseCase;
import com.example.negocio.usuario.usecase.ActualizarUsuarioSistemaUseCase;
import com.mycompany.common.dtos.UsuarioSistemaDTO;
import com.mycompany.persistencia.enums.RolUsuario;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class NuevoUsuarioSistemaModalController {

    private final RegistrarUsuarioSistemaUseCase registrarUsuarioUseCase;
    private final ActualizarUsuarioSistemaUseCase actualizarUsuarioUseCase;

    private UsuarioSistemaDTO usuarioCreado;
    private UsuarioSistemaDTO usuarioAEditar;

    @FXML private Label lblTitulo;
    @FXML private TextField txtCorreo;
    @FXML private PasswordField txtContrasena;
    @FXML private TextField txtTelefono;
    @FXML private ComboBox<RolUsuario> comboRol;
    /** Solo al registrar; la de un usuario existente se cambia con «Cambiar contraseña». */
    @FXML private VBox boxContrasena;

    @FXML
    public void initialize() {
        if (txtTelefono != null) {
            txtTelefono.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
                if (change.getControlNewText().matches("\\d{0,10}")) return change;
                return null;
            }));
        }

        comboRol.setItems(FXCollections.observableArrayList(RolUsuario.values()));
        comboRol.setConverter(new StringConverter<RolUsuario>() {
            @Override
            public String toString(RolUsuario rol) {
                if (rol == null) return "";
                String nombre = rol.name();
                return nombre.substring(0, 1).toUpperCase() + nombre.substring(1).toLowerCase();
            }
            @Override
            public RolUsuario fromString(String string) { return null; }
        });

        // Reset singleton state
        usuarioCreado = null;
        usuarioAEditar = null;
        txtCorreo.setText("");
        txtContrasena.setText("");
        txtTelefono.setText("");
        comboRol.setValue(RolUsuario.ADMINISTRADOR);
        lblTitulo.setText("Registrar Nuevo Usuario");
        boxContrasena.setVisible(true);
        boxContrasena.setManaged(true);
    }

    public UsuarioSistemaDTO getUsuarioCreado() {
        return usuarioCreado;
    }

    public void setUsuarioAEditar(UsuarioSistemaDTO usuarioAEditar) {
        this.usuarioAEditar = usuarioAEditar;
        if (usuarioAEditar != null) {
            lblTitulo.setText("Editar Usuario");
            txtCorreo.setText(usuarioAEditar.getCorreo());
            txtTelefono.setText(usuarioAEditar.getTelefono() != null ? usuarioAEditar.getTelefono() : "");
            comboRol.setValue(usuarioAEditar.getRol());
            boxContrasena.setVisible(false);
            boxContrasena.setManaged(false);
        }
    }

    @FXML
    private void guardarUsuario() {
        String correo = txtCorreo.getText().trim();
        String contrasena = txtContrasena.getText();
        String telefono = txtTelefono.getText().trim();
        RolUsuario rol = comboRol.getValue();

        if (correo.isEmpty()) {
            mostrarAlerta("Campos Incompletos", "El correo electrónico es obligatorio.");
            return;
        }
        if (rol == null) {
            mostrarAlerta("Campos Incompletos", "Debe seleccionar un rol.");
            return;
        }

        try {
            if (usuarioAEditar == null) {
                // Crear nuevo
                if (contrasena == null || contrasena.trim().isEmpty()) {
                    mostrarAlerta("Campos Incompletos", "La contraseña es obligatoria para nuevos usuarios.");
                    return;
                }
                UsuarioSistemaDTO dto = new UsuarioSistemaDTO();
                dto.setCorreo(correo);
                dto.setContrasena(contrasena);
                dto.setTelefono(telefono.isEmpty() ? null : telefono);
                dto.setRol(rol);
                usuarioCreado = registrarUsuarioUseCase.registrarUsuario(dto);
            } else {
                // Editar existente
                // Se envía una copia: si el guardado falla o se cancela, el usuario de la
                // tabla no debe quedar con los datos que se teclearon aquí.
                UsuarioSistemaDTO cambios = new UsuarioSistemaDTO();
                cambios.setId(usuarioAEditar.getId());
                cambios.setActivo(usuarioAEditar.isActivo());
                cambios.setCorreo(correo);
                cambios.setTelefono(telefono.isEmpty() ? null : telefono);
                cambios.setRol(rol);
                usuarioCreado = actualizarUsuarioUseCase.actualizarUsuario(cambios);
            }
            cerrarModal();
        } catch (CotizacionException ex) {
            mostrarAlerta("Error", ex.getMessage());
        } catch (Exception ex) {
            mostrarAlerta("Error", "Ocurrió un error inesperado al guardar el usuario.");
        }
    }

    @FXML
    private void cerrarModal() {
        Stage stage = (Stage) txtCorreo.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
