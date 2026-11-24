package com.mycompany.presentacion.utils;

import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.dtos.PaquetePrecioDTO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

/** Utilidades para mostrar la tabla de precios de un paquete (comida × personas). */
public final class TablaPreciosPaquete {

    private TablaPreciosPaquete() {
    }

    public static String dinero(double importe) {
        return String.format("$%,.2f", importe);
    }

    /** Precio para mostrar en listas: el fijo, o "Desde $X" si depende de la comida y las personas. */
    public static String precioParaMostrar(PaqueteDTO paquete) {
        if (paquete == null) return "";
        return paquete.isTablaPrecios() ? "Desde " + dinero(paquete.getPrecioDesde()) : dinero(paquete.getCostoBase());
    }

    /** Resumen corto, p. ej. "7 comidas × 4 rangos de personas · desde $14,700.00". */
    public static String resumen(List<PaquetePrecioDTO> precios) {
        if (precios == null || precios.isEmpty()) return "Sin tabla de precios";
        long comidas = precios.stream().map(PaquetePrecioDTO::getComidaId).filter(Objects::nonNull).distinct().count();
        long rangos = precios.stream().map(PaquetePrecioDTO::getPersonas).distinct().count();
        double desde = precios.stream().mapToDouble(PaquetePrecioDTO::getPrecio).min().orElse(0);
        return comidas + (comidas == 1 ? " comida" : " comidas") + " × " + rangos
                + (rangos == 1 ? " rango" : " rangos") + " de personas · desde " + dinero(desde);
    }

    /** Cuadrícula de solo lectura: un renglón por comida y una columna por número de personas. */
    public static GridPane soloLectura(List<PaquetePrecioDTO> precios) {
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(8);
        grid.setPadding(new Insets(4, 0, 4, 0));
        if (precios == null || precios.isEmpty()) return grid;

        TreeSet<Integer> rangos = new TreeSet<>();
        Map<String, Map<Integer, Double>> porComida = new LinkedHashMap<>();
        for (PaquetePrecioDTO p : precios) {
            rangos.add(p.getPersonas());
            porComida.computeIfAbsent(p.getComidaNombre(), k -> new LinkedHashMap<>()).put(p.getPersonas(), p.getPrecio());
        }

        Label esquina = new Label("COMIDA");
        esquina.getStyleClass().add("label-pequeno");
        grid.add(esquina, 0, 0);
        int col = 1;
        for (Integer personas : rangos) {
            Label encabezado = new Label(personas + " PERSONAS");
            encabezado.getStyleClass().add("label-pequeno");
            GridPane.setHalignment(encabezado, HPos.RIGHT);
            grid.add(encabezado, col++, 0);
        }
        int fila = 1;
        for (Map.Entry<String, Map<Integer, Double>> e : porComida.entrySet()) {
            Label comida = new Label(e.getKey());
            comida.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            grid.add(comida, 0, fila);
            col = 1;
            for (Integer personas : rangos) {
                Double precio = e.getValue().get(personas);
                Label celda = new Label(precio == null ? "—" : dinero(precio));
                celda.setStyle("-fx-text-fill: #2c3e50;");
                GridPane.setHalignment(celda, HPos.RIGHT);
                grid.add(celda, col++, fila);
            }
            fila++;
        }
        return grid;
    }
}
