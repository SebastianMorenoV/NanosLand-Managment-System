package com.mycompany.presentacion.config;

import com.example.negocio.usuario.usecase.RegistrarUsuarioSistemaUseCase;
import com.mycompany.common.dtos.UsuarioSistemaDTO;
import com.mycompany.persistencia.enums.RolUsuario;
import com.mycompany.persistencia.repository.UsuarioRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.context.ApplicationContext;

/**
 * Alta del primer usuario del sistema.
 *
 * Una base de datos recién creada no tiene usuarios, así que nadie podría
 * iniciar sesión. Cuando la tabla está vacía se pide crear la cuenta del
 * dueño antes de mostrar el inicio de sesión.
 */
public final class PrimerUsuarioDialog {

    private PrimerUsuarioDialog() {
    }

    /**
     * Si la base de datos no tiene ningún usuario, muestra la ventana para
     * crear la cuenta del dueño. Si ya hay usuarios no hace nada.
     */
    public static void mostrarSiNoHayUsuarios(Window propietario, ApplicationContext spring) {
        if (spring.getBean(UsuarioRepository.class).count() > 0) {
            return;
        }
        RegistrarUsuarioSistemaUseCase registrar = spring.getBean(RegistrarUsuarioSistemaUseCase.class);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (propietario != null) {
            stage.initOwner(propietario);
        }
        stage.setTitle("Crear cuenta del dueño");
        stage.setResizable(false);
        try {
            stage.getIcons().add(new Image(PrimerUsuarioDialog.class.getResourceAsStream("/Logo Nanos.png")));
        } catch (RuntimeException e) {
            // Sin ícono la ventana sigue siendo usable.
        }

        Label titulo = new Label("Crear cuenta del dueño");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label ayuda = new Label("Esta base de datos todavía no tiene usuarios. Crea la cuenta del dueño "
                + "para poder iniciar sesión; después podrás dar de alta a los demás usuarios desde el sistema.\n\n"
                + "Si esperabas ver tus datos, cancela y revisa la conexión a la base de datos.");
        ayuda.setWrapText(true);
        ayuda.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        TextField txtCorreo = new TextField();
        txtCorreo.setPromptText("correo@nanosland.com");
        txtCorreo.getStyleClass().add("text-field-moderno");
        TextField txtTelefono = new TextField();
        txtTelefono.getStyleClass().add("text-field-moderno");
        PasswordField txtContrasena = new PasswordField();
        txtContrasena.getStyleClass().add("text-field-moderno");
        PasswordField txtConfirmar = new PasswordField();
        txtConfirmar.getStyleClass().add("text-field-moderno");

        Label mensaje = new Label();
        mensaje.setWrapText(true);
        mensaje.setMinHeight(Region.USE_PREF_SIZE);
        mensaje.setStyle("-fx-font-size: 12px; -fx-text-fill: #c0392b;");

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("boton-texto-gris");
        btnCancelar.setCancelButton(true);
        Button btnCrear = new Button("Crear cuenta");
        btnCrear.getStyleClass().add("boton-azul");
        btnCrear.setDefaultButton(true);
        HBox botones = new HBox(10, btnCancelar, btnCrear);
        botones.setAlignment(Pos.CENTER_RIGHT);

        VBox raiz = new VBox(16, titulo, ayuda,
                grupo("CORREO ELECTRÓNICO", txtCorreo),
                grupo("TELÉFONO (OPCIONAL)", txtTelefono),
                grupo("CONTRASEÑA", txtContrasena),
                grupo("CONFIRMAR CONTRASEÑA", txtConfirmar),
                mensaje, botones);
        raiz.setPadding(new Insets(28));
        raiz.setPrefWidth(460);
        raiz.getStyleClass().add("tarjeta-blanca");

        btnCrear.setOnAction(e -> {
            String correo = txtCorreo.getText() == null ? "" : txtCorreo.getText().trim();
            String contrasena = txtContrasena.getText() == null ? "" : txtContrasena.getText();
            if (!correo.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
                mensaje.setText("Escribe un correo electrónico válido.");
                return;
            }
            if (contrasena.trim().isEmpty()) {
                mensaje.setText("La contraseña es obligatoria.");
                return;
            }
            if (!contrasena.equals(txtConfirmar.getText())) {
                mensaje.setText("Las contraseñas no coinciden.");
                return;
            }
            try {
                UsuarioSistemaDTO dto = new UsuarioSistemaDTO();
                dto.setCorreo(correo);
                dto.setContrasena(contrasena);
                dto.setTelefono(txtTelefono.getText());
                dto.setRol(RolUsuario.DUEÑO);
                dto.setActivo(true);
                registrar.registrarUsuario(dto);
                stage.close();
            } catch (RuntimeException ex) {
                mensaje.setText(ex.getMessage() != null ? ex.getMessage() : "No se pudo crear la cuenta.");
            }
        });
        btnCancelar.setOnAction(e -> stage.close());

        Scene scene = new Scene(raiz);
        scene.getStylesheets().add(PrimerUsuarioDialog.class
                .getResource("/com/mycompany/presentacion/styles/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.showAndWait();
    }

    private static VBox grupo(String etiqueta, TextField campo) {
        Label label = new Label(etiqueta);
        label.getStyleClass().add("label-pequeno");
        return new VBox(6, label, campo);
    }
}
