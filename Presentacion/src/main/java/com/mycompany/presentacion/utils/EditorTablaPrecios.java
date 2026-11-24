package com.mycompany.presentacion.utils;

import com.mycompany.common.dtos.PaquetePrecioDTO;
import com.mycompany.common.dtos.ServicioDTO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Diálogo para capturar la tabla de precios de un paquete: se eligen las comidas y los
 * rangos de personas, y se llena un precio por cada combinación (como en los volantes).
 * Trabaja sobre una copia: si se cancela, la tabla original no cambia.
 */
public final class EditorTablaPrecios {

    private static final String RANGOS_SUGERIDOS = "60, 80, 100, 120";

    private final List<ServicioDTO> comidas = new ArrayList<>();
    private final Map<Long, CheckBox> checks = new LinkedHashMap<>();
    /** Precios capturados por "comidaId|personas"; sobreviven a los cambios de comidas o rangos. */
    private final Map<String, String> valores = new HashMap<>();
    private final Map<String, TextField> celdas = new HashMap<>();
    private final TextField txtRangos = ModalHelper.campoTexto(RANGOS_SUGERIDOS);
    private final GridPane grid = new GridPane();
    private final Label lblError = new Label();
    private List<Integer> rangos = new ArrayList<>();

    private EditorTablaPrecios(List<ServicioDTO> catalogo, List<PaquetePrecioDTO> actuales) {
        comidas.addAll(catalogo);
        // Comidas de la tabla actual que ya no están en el catálogo (p. ej. dadas de baja): se muestran
        // para que se puedan quitar.
        for (PaquetePrecioDTO p : actuales) {
            if (comidas.stream().noneMatch(c -> c.getId().equals(p.getComidaId()))) {
                ServicioDTO c = new ServicioDTO();
                c.setId(p.getComidaId());
                c.setNombre(p.getComidaNombre() + " (ya no está en el catálogo)");
                comidas.add(c);
            }
            valores.put(clave(p.getComidaId(), p.getPersonas()), texto(p.getPrecio()));
        }
        TreeSet<Integer> rangosActuales = actuales.stream().map(PaquetePrecioDTO::getPersonas)
                .collect(Collectors.toCollection(TreeSet::new));
        rangos = rangosActuales.isEmpty() ? parsearRangos(RANGOS_SUGERIDOS) : new ArrayList<>(rangosActuales);
        txtRangos.setText(rangos.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        for (ServicioDTO c : comidas) {
            CheckBox check = new CheckBox(c.getNombre());
            check.setSelected(actuales.stream().anyMatch(p -> c.getId().equals(p.getComidaId())));
            check.selectedProperty().addListener((obs, a, b) -> reconstruir());
            checks.put(c.getId(), check);
        }
    }

    /**
     * Muestra el editor.
     *
     * @param comidas  comidas activas del catálogo (servicios de categoría Comida)
     * @param actuales tabla actual del paquete (vacía si aún no tiene)
     * @return la tabla nueva, o vacío si se canceló
     */
    public static Optional<List<PaquetePrecioDTO>> mostrar(Window propietario, List<ServicioDTO> comidas,
                                                         List<PaquetePrecioDTO> actuales) {
        return new EditorTablaPrecios(comidas, actuales == null ? List.of() : actuales).abrir(propietario);
    }

    private Optional<List<PaquetePrecioDTO>> abrir(Window propietario) {
        Dialog<List<PaquetePrecioDTO>> dialog = new Dialog<>();
        dialog.setTitle("Tabla de precios");
        ButtonType aceptar = new ButtonType("Usar esta tabla", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, aceptar);

        FlowPane panelComidas = new FlowPane(16, 8);
        panelComidas.getChildren().addAll(checks.values());
        panelComidas.setPrefWrapLength(620);

        Button btnAplicar = new Button("Aplicar");
        btnAplicar.getStyleClass().add("boton-azul-claro");
        btnAplicar.setOnAction(e -> aplicarRangos());
        txtRangos.setOnAction(e -> aplicarRangos());
        HBox.setHgrow(txtRangos, Priority.ALWAYS);
        HBox filaRangos = new HBox(10, txtRangos, btnAplicar);
        filaRangos.setAlignment(Pos.CENTER_LEFT);

        grid.setHgap(10);
        grid.setVgap(8);
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportWidth(660);
        scroll.setPrefViewportHeight(300);
        scroll.getStyleClass().add("scroll-pane");

        lblError.setWrapText(true);
        lblError.setStyle("-fx-text-fill: #e74c3c;");
        lblError.setMinHeight(Region.USE_PREF_SIZE);

        VBox contenido = new VBox(14,
                ModalHelper.encabezado("Tabla de precios",
                        "Elija las comidas y los números de personas, y capture el precio de cada combinación."),
                ModalHelper.campo("COMIDAS", comidas.isEmpty()
                        ? new Label("No hay comidas en el catálogo. Dé de alta las comidas en Servicios con la categoría «Comida».")
                        : panelComidas),
                ModalHelper.campo("NÚMERO DE PERSONAS (separados por comas)", filaRangos),
                scroll,
                lblError);
        contenido.setPrefWidth(700);
        dialog.getDialogPane().setContent(contenido);
        ModalHelper.aplicarEstilo(dialog, propietario);

        Button btnAceptar = (Button) dialog.getDialogPane().lookupButton(aceptar);
        btnAceptar.setDisable(comidas.isEmpty());
        btnAceptar.addEventFilter(ActionEvent.ACTION, e -> {
            if (construir() == null) e.consume();
        });
        dialog.setResultConverter(boton -> boton == aceptar ? construir() : null);

        reconstruir();
        return dialog.showAndWait();
    }

    private void aplicarRangos() {
        List<Integer> nuevos = parsearRangos(txtRangos.getText());
        if (nuevos.isEmpty()) {
            mostrarError("Escriba al menos un número de personas, p. ej. " + RANGOS_SUGERIDOS + ".");
            return;
        }
        rangos = nuevos;
        txtRangos.setText(rangos.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        reconstruir();
    }

    /** Vuelve a armar la cuadrícula con las comidas marcadas y los rangos, conservando lo capturado. */
    private void reconstruir() {
        guardarCapturado();
        grid.getChildren().clear();
        celdas.clear();
        mostrarError(null);

        List<ServicioDTO> elegidas = comidasElegidas();
        if (elegidas.isEmpty()) {
            Label vacio = new Label("Marque las comidas que ofrece este paquete.");
            vacio.setStyle("-fx-text-fill: #7f8c8d;");
            grid.add(vacio, 0, 0);
            return;
        }
        Label esquina = new Label("COMIDA");
        esquina.getStyleClass().add("label-pequeno");
        grid.add(esquina, 0, 0);
        for (int c = 0; c < rangos.size(); c++) {
            Label encabezado = new Label(rangos.get(c) + " PERSONAS");
            encabezado.getStyleClass().add("label-pequeno");
            encabezado.setMinWidth(Region.USE_PREF_SIZE); // que la columna se ensanche en vez de cortar el texto
            GridPane.setHalignment(encabezado, HPos.CENTER);
            grid.add(encabezado, c + 1, 0);
        }
        for (int f = 0; f < elegidas.size(); f++) {
            ServicioDTO comida = elegidas.get(f);
            Label nombre = new Label(comida.getNombre());
            nombre.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            nombre.setMinWidth(160);
            grid.add(nombre, 0, f + 1);
            for (int c = 0; c < rangos.size(); c++) {
                String clave = clave(comida.getId(), rangos.get(c));
                TextField celda = ModalHelper.campoTexto("0.00");
                celda.setPrefWidth(100);
                celda.setTextFormatter(new TextFormatter<>(cambio ->
                        cambio.getControlNewText().matches("\\d{0,7}(\\.\\d{0,2})?") ? cambio : null));
                celda.setText(valores.getOrDefault(clave, ""));
                celdas.put(clave, celda);
                grid.add(celda, c + 1, f + 1);
            }
        }
    }

    /** Arma la tabla o devuelve {@code null} (mostrando el motivo) si falta algo. */
    private List<PaquetePrecioDTO> construir() {
        guardarCapturado();
        List<ServicioDTO> elegidas = comidasElegidas();
        if (elegidas.isEmpty() || rangos.isEmpty()) {
            mostrarError("Marque al menos una comida y capture al menos un número de personas.");
            return null;
        }
        List<PaquetePrecioDTO> tabla = new ArrayList<>();
        for (ServicioDTO comida : elegidas) {
            for (Integer personas : rangos) {
                String valor = valores.get(clave(comida.getId(), personas));
                double precio;
                try {
                    precio = valor == null || valor.isBlank() ? 0 : Double.parseDouble(valor);
                } catch (NumberFormatException e) {
                    precio = 0;
                }
                if (!(precio > 0)) {
                    mostrarError("Falta el precio de " + comida.getNombre() + " para " + personas + " personas.");
                    TextField celda = celdas.get(clave(comida.getId(), personas));
                    if (celda != null) celda.requestFocus();
                    return null;
                }
                tabla.add(new PaquetePrecioDTO(null, comida.getId(), comida.getNombre(), personas, precio));
            }
        }
        return tabla;
    }

    private List<ServicioDTO> comidasElegidas() {
        return comidas.stream().filter(c -> checks.get(c.getId()).isSelected()).collect(Collectors.toList());
    }

    private void guardarCapturado() {
        celdas.forEach((clave, celda) -> valores.put(clave, celda.getText()));
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje == null ? "" : mensaje);
    }

    private static List<Integer> parsearRangos(String texto) {
        TreeSet<Integer> rangos = new TreeSet<>();
        if (texto != null) {
            for (String parte : texto.split("[,;\\s]+")) {
                try {
                    int n = Integer.parseInt(parte.trim());
                    if (n > 0) rangos.add(n);
                } catch (NumberFormatException ignorado) {
                    // se ignora lo que no sea número
                }
            }
        }
        return new ArrayList<>(rangos);
    }

    private static String clave(Long comidaId, int personas) {
        return comidaId + "|" + personas;
    }

    private static String texto(double precio) {
        return precio == Math.floor(precio)
                ? String.format(Locale.ROOT, "%.0f", precio)
                : String.format(Locale.ROOT, "%.2f", precio);
    }
}
