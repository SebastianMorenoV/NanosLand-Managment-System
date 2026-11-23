package com.mycompany.presentacion.config;

import java.util.Optional;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Ventana para capturar los datos de conexión a MySQL.
 *
 * Se arma en código (sin FXML ni Spring) porque debe poder mostrarse antes de
 * que exista el contexto de Spring: justamente cuando la aplicación todavía
 * no logra conectarse a la base de datos.
 */
public final class ConfiguracionBDDialog {

    private ConfiguracionBDDialog() {
    }

    /**
     * Muestra la ventana y espera a que el usuario guarde o cancele.
     *
     * @param propietario ventana sobre la que se muestra, o {@code null}.
     * @param inicial     valores con los que se llenan los campos.
     * @param aviso       mensaje inicial (por ejemplo, el error de conexión), o {@code null}.
     * @return la configuración ya probada (todavía sin guardar), o vacío si se canceló.
     */
    public static Optional<ConfiguracionBD> mostrar(Window propietario, ConfiguracionBD inicial, String aviso) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (propietario != null) {
            stage.initOwner(propietario);
        }
        stage.setTitle("Conexión a la base de datos");
        stage.setResizable(false);
        try {
            stage.getIcons().add(new Image(ConfiguracionBDDialog.class.getResourceAsStream("/Logo Nanos.png")));
        } catch (RuntimeException e) {
            // Sin ícono la ventana sigue siendo usable.
        }

        Label titulo = new Label("Conexión a la base de datos");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label ayuda = new Label("Datos del servidor MySQL de este equipo. La base de datos y sus tablas "
                + "se crean solas si todavía no existen.");
        ayuda.setWrapText(true);
        ayuda.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        TextField txtHost = campo(inicial.host());
        TextField txtPuerto = campo(String.valueOf(inicial.puerto()));
        txtPuerto.setPrefWidth(110);
        TextField txtBase = campo(inicial.baseDatos());
        TextField txtUsuario = campo(inicial.usuario());
        PasswordField txtContrasena = new PasswordField();
        txtContrasena.setText(inicial.contrasena());
        txtContrasena.getStyleClass().add("text-field-moderno");

        VBox grupoHost = grupo("SERVIDOR", txtHost);
        HBox.setHgrow(grupoHost, Priority.ALWAYS);
        HBox filaServidor = new HBox(12, grupoHost, grupo("PUERTO", txtPuerto));

        Label mensaje = new Label(aviso == null ? "" : aviso);
        mensaje.setWrapText(true);
        mensaje.setMinHeight(Region.USE_PREF_SIZE);
        estiloMensaje(mensaje, false);

        Button btnProbar = new Button("Probar conexión");
        btnProbar.getStyleClass().add("boton-azul-claro");
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("boton-texto-gris");
        btnCancelar.setCancelButton(true);
        Button btnGuardar = new Button("Guardar y continuar");
        btnGuardar.getStyleClass().add("boton-azul");
        btnGuardar.setDefaultButton(true);

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox botones = new HBox(10, btnProbar, espacio, btnCancelar, btnGuardar);
        botones.setAlignment(Pos.CENTER_RIGHT);

        VBox raiz = new VBox(16, titulo, ayuda, filaServidor,
                grupo("BASE DE DATOS", txtBase),
                grupo("USUARIO", txtUsuario),
                grupo("CONTRASEÑA", txtContrasena),
                mensaje, botones);
        raiz.setPadding(new Insets(28));
        raiz.setPrefWidth(460);
        raiz.getStyleClass().add("tarjeta-blanca");

        ConfiguracionBD[] resultado = new ConfiguracionBD[1];

        btnProbar.setOnAction(e -> {
            ConfiguracionBD config = leer(txtHost, txtPuerto, txtBase, txtUsuario, txtContrasena, mensaje);
            if (config == null) {
                return;
            }
            String error = config.probarConexion();
            estiloMensaje(mensaje, error == null);
            mensaje.setText(error == null ? "Conexión exitosa." : error);
        });

        btnGuardar.setOnAction(e -> {
            ConfiguracionBD config = leer(txtHost, txtPuerto, txtBase, txtUsuario, txtContrasena, mensaje);
            if (config == null) {
                return;
            }
            String error = config.probarConexion();
            if (error != null) {
                estiloMensaje(mensaje, false);
                mensaje.setText(error);
                return;
            }
            // No se guarda aquí: quien llama lo hace cuando el sistema ya arrancó con
            // estos datos, para no dejar guardada una configuración que no funciona.
            resultado[0] = config;
            stage.close();
        });

        btnCancelar.setOnAction(e -> stage.close());

        Scene scene = new Scene(raiz);
        scene.getStylesheets().add(ConfiguracionBDDialog.class
                .getResource("/com/mycompany/presentacion/styles/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.showAndWait();

        return Optional.ofNullable(resultado[0]);
    }

    private static ConfiguracionBD leer(TextField host, TextField puerto, TextField base,
            TextField usuario, PasswordField contrasena, Label mensaje) {
        estiloMensaje(mensaje, false);
        if (host.getText().isBlank() || base.getText().isBlank() || usuario.getText().isBlank()) {
            mensaje.setText("Servidor, base de datos y usuario son obligatorios.");
            return null;
        }
        // Nombre de equipo, IPv4 o IPv6 entre corchetes; sin puerto, rutas ni parámetros.
        if (!host.getText().trim().matches("[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?|\\[[0-9A-Fa-f:.]+\\]")) {
            mensaje.setText("El servidor debe ser un nombre de equipo o una dirección IP, sin puerto "
                    + "(por ejemplo: localhost o 192.168.1.10). Una dirección IPv6 va entre corchetes: [::1].");
            return null;
        }
        if (!base.getText().trim().matches("[A-Za-z0-9_]+")) {
            mensaje.setText("El nombre de la base de datos solo puede llevar letras, números y guion bajo.");
            return null;
        }
        int numPuerto;
        try {
            numPuerto = Integer.parseInt(puerto.getText().trim());
            if (numPuerto < 1 || numPuerto > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            mensaje.setText("El puerto debe ser un número entre 1 y 65535.");
            return null;
        }
        return new ConfiguracionBD(host.getText().trim(), numPuerto, base.getText().trim(),
                usuario.getText().trim(), contrasena.getText());
    }

    private static TextField campo(String valor) {
        TextField campo = new TextField(valor);
        campo.getStyleClass().add("text-field-moderno");
        return campo;
    }

    private static VBox grupo(String etiqueta, TextField campo) {
        Label label = new Label(etiqueta);
        label.getStyleClass().add("label-pequeno");
        return new VBox(6, label, campo);
    }

    private static void estiloMensaje(Label mensaje, boolean exito) {
        mensaje.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (exito ? "#27ae60" : "#c0392b") + ";");
    }
}
