package com.mycompany.common.util;

import com.mycompany.common.dtos.DescuentoDTO;
import com.mycompany.persistencia.enums.TipoDescuento;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Textos para mostrar un descuento, p. ej. "15%", "Lunes a jueves" o la frase completa
 * "15% menos en todos los paquetes, para eventos de lunes a jueves.".
 */
public final class DescripcionDescuento {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] NOMBRES = {"lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};
    private static final String[] CORTOS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    private DescripcionDescuento() {
    }

    /** "lunes", "miércoles"... */
    public static String nombreDia(DayOfWeek dia) {
        return NOMBRES[dia.getValue() - 1];
    }

    /** "Lun", "Mié"... */
    public static String nombreCortoDia(DayOfWeek dia) {
        return CORTOS[dia.getValue() - 1];
    }

    /** "15%", "12.5%" o "$1,000.00". */
    public static String valor(TipoDescuento tipo, double valor) {
        if (tipo == TipoDescuento.MONTO_FIJO) {
            return String.format("$%,.2f", valor);
        }
        String numero = valor == Math.rint(valor)
                ? String.valueOf((long) valor)
                : String.format(Locale.ROOT, "%.2f", valor).replaceAll("0+$", "");
        return numero + "%";
    }

    /** "Todos los días", "Lunes a jueves", "Sábado y domingo"... */
    public static String dias(Set<DayOfWeek> dias) {
        return capitalizar(textoDias(dias));
    }

    /** "Del 01/12/2026 al 31/12/2026", "Desde el 01/12/2026", "Cualquier fecha"... */
    public static String temporada(LocalDate inicio, LocalDate fin) {
        if (inicio == null && fin == null) return "Cualquier fecha";
        if (inicio != null && inicio.equals(fin)) return "Solo el " + FECHA.format(inicio);
        if (inicio == null) return "Hasta el " + FECHA.format(fin);
        if (fin == null) return "Desde el " + FECHA.format(inicio);
        return "Del " + FECHA.format(inicio) + " al " + FECHA.format(fin);
    }

    /** "Todos los paquetes" o la lista de nombres. */
    public static String paquetes(DescuentoDTO descuento) {
        if (descuento.isTodosLosPaquetes()) return "Todos los paquetes";
        List<String> nombres = descuento.getPaqueteNombres();
        return nombres == null || nombres.isEmpty() ? "Ningún paquete" : lista(nombres);
    }

    /**
     * La regla en una frase, p. ej. "15% menos en Paquete Plus y Paquete Full, para eventos de
     * lunes a jueves del 01/12/2026 al 31/12/2026. Se puede cotizar hasta el 15/12/2026."
     */
    public static String resumen(DescuentoDTO d) {
        StringBuilder sb = new StringBuilder();
        sb.append(valor(d.getTipo(), d.getValor())).append(" menos en ");
        sb.append(d.isTodosLosPaquetes() ? "todos los paquetes" : paquetes(d));
        sb.append(", para eventos ");

        Set<DayOfWeek> dias = d.getDias();
        if (todosLosDias(dias)) {
            sb.append("de cualquier día");
        } else {
            sb.append(esRango(dias) ? "de " : "en ").append(textoDias(dias));
        }

        LocalDate inicio = d.getFechaInicio();
        LocalDate fin = d.getFechaFin();
        if (inicio != null && inicio.equals(fin)) {
            sb.append(" del ").append(FECHA.format(inicio));
        } else if (inicio != null && fin != null) {
            sb.append(" del ").append(FECHA.format(inicio)).append(" al ").append(FECHA.format(fin));
        } else if (inicio != null) {
            sb.append(" a partir del ").append(FECHA.format(inicio));
        } else if (fin != null) {
            sb.append(" hasta el ").append(FECHA.format(fin));
        }
        sb.append('.');

        if (d.getCotizarHasta() != null) {
            sb.append(" Se puede cotizar hasta el ").append(FECHA.format(d.getCotizarHasta())).append('.');
        }
        return sb.toString();
    }

    /** Une nombres como en español: "A", "A y B", "A, B y C". */
    public static String lista(List<String> nombres) {
        if (nombres.size() == 1) return nombres.get(0);
        return String.join(", ", nombres.subList(0, nombres.size() - 1)) + " y " + nombres.get(nombres.size() - 1);
    }

    private static boolean todosLosDias(Set<DayOfWeek> dias) {
        return dias == null || dias.isEmpty() || dias.size() == 7;
    }

    /** Tres o más días seguidos (de lunes a domingo), que se escriben "lunes a jueves". */
    private static boolean esRango(Set<DayOfWeek> dias) {
        if (todosLosDias(dias) || dias.size() < 3) return false;
        TreeSet<DayOfWeek> orden = new TreeSet<>(dias);
        return orden.last().getValue() - orden.first().getValue() == orden.size() - 1;
    }

    private static String textoDias(Set<DayOfWeek> dias) {
        if (todosLosDias(dias)) return "todos los días";
        TreeSet<DayOfWeek> orden = new TreeSet<>(dias);
        if (esRango(dias)) {
            return nombreDia(orden.first()) + " a " + nombreDia(orden.last());
        }
        return lista(orden.stream().map(DescripcionDescuento::nombreDia).toList());
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
