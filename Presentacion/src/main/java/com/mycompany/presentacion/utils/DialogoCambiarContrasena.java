package com.mycompany.presentacion.utils;

import com.example.negocio.usuario.usecase.CambiarContrasenaUseCase;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Ventana para cambiar la contraseña de un usuario. Si es la del usuario con la sesión
 * abierta pide también la actual; si el dueño cambia la de otro, no. Las reglas las
 * valida {@link CambiarContrasenaUseCase}.
 */
public final class DialogoCambiarContrasena {

    private DialogoCambiarContrasena() {
    }

    /**
     * @param solicitanteId usuario con la sesión abierta.
     * @param usuarioId     usuario al que se le cambia la contraseña.
     * @param correo        correo de ese usuario, para mostrarlo.
     * @return {@code true} si la contraseña se cambió.
     */
    public static boolean mostrar(Window propietario, CambiarContrasenaUseCase casoDeUso,
                                  Long solicitanteId, Long usuarioId, String correo) {
        boolean propia = solicitanteId != null && solicitanteId.equals(usuarioId);

        PasswordField txtActual = campo();
        PasswordField txtNueva = campo();
        PasswordField txtConfirmar = campo();

        VBox contenido = new VBox(14, ModalHelper.encabezado("Cambiar contraseña", propia
                ? "Para cambiar su contraseña escriba primero la actual."
                : "Nueva contraseña para " + correo + ". No hace falta saber la actual."));
        if (propia) {
            contenido.getChildren().add(ModalHelper.campo("CONTRASEÑA ACTUAL", txtActual));
        }
        contenido.getChildren().addAll(
                ModalHelper.campo("CONTRASEÑA NUEVA", txtNueva),
                ModalHelper.campo("CONFIRMAR CONTRASEÑA NUEVA", txtConfirmar));
        contenido.setPrefWidth(380);

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Cambiar contraseña");
        ButtonType guardar = new ButtonType("Cambiar contraseña", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, guardar);
        dialogo.getDialogPane().setContent(contenido);
        ModalHelper.aplicarEstilo(dialogo, propietario);

        // Se guarda al pulsar el botón; si algo falla, la ventana sigue abierta para corregirlo.
        dialogo.getDialogPane().lookupButton(guardar).addEventFilter(ActionEvent.ACTION, e -> {
            String nueva = txtNueva.getText();
            String error = null;
            if (nueva == null || nueva.trim().isEmpty()) {
                error = "Escriba la contraseña nueva.";
            } else if (!nueva.equals(txtConfirmar.getText())) {
                error = "La confirmación no coincide con la contraseña nueva.";
            } else {
                try {
                    casoDeUso.cambiarContrasena(solicitanteId, usuarioId, propia ? txtActual.getText() : null, nueva);
                } catch (Exception ex) {
                    error = ex.getMessage();
                }
            }
            if (error != null) {
                e.consume();
                avisar(dialogo.getDialogPane().getScene().getWindow(), Alert.AlertType.WARNING,
                        "No se pudo cambiar la contraseña", error);
            }
        });

        boolean cambiada = dialogo.showAndWait().orElse(ButtonType.CANCEL) == guardar;
        if (cambiada) {
            avisar(propietario, Alert.AlertType.INFORMATION, "Contraseña cambiada", propia
                    ? "Use la contraseña nueva la próxima vez que inicie sesión."
                    : "La contraseña de " + correo + " se cambió. Compártala con esa persona.");
        }
        return cambiada;
    }

    private static PasswordField campo() {
        PasswordField campo = new PasswordField();
        campo.getStyleClass().add("text-field-moderno");
        return campo;
    }

    private static void avisar(Window propietario, Alert.AlertType tipo, String encabezado, String mensaje) {
        Alert alert = new Alert(tipo);
        if (propietario != null) {
            alert.initOwner(propietario);
        }
        alert.setTitle("Cambiar contraseña");
        alert.setHeaderText(encabezado);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
