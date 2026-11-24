package com.example.negocio.precio;

import com.mycompany.persistencia.dominio.Descuento;
import com.mycompany.persistencia.enums.TipoDescuento;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/**
 * Cuándo aplica un descuento a un paquete y cuánto rebaja.
 *
 * <ul>
 *   <li>Días y temporada se revisan contra la fecha del evento.</li>
 *   <li>"Cotizar hasta" se revisa contra el día en que se cotiza.</li>
 *   <li>Si aplican varios, gana el que más rebaja (no se suman).</li>
 * </ul>
 */
public final class ReglaDescuento {

    private ReglaDescuento() {
    }

    /** Si el descuento aplica al paquete para un evento en {@code fechaEvento}, cotizando el día {@code hoy}. */
    public static boolean aplica(Descuento descuento, Long paqueteId, LocalDate fechaEvento, LocalDate hoy) {
        if (descuento == null || !descuento.isActivo() || fechaEvento == null) {
            return false;
        }
        if (descuento.getCotizarHasta() != null && hoy.isAfter(descuento.getCotizarHasta())) {
            return false;
        }
        if (descuento.getFechaInicio() != null && fechaEvento.isBefore(descuento.getFechaInicio())) {
            return false;
        }
        if (descuento.getFechaFin() != null && fechaEvento.isAfter(descuento.getFechaFin())) {
            return false;
        }
        if (descuento.getDias() != null && !descuento.getDias().isEmpty()
                && !descuento.getDias().contains(fechaEvento.getDayOfWeek())) {
            return false;
        }
        return descuento.isTodosLosPaquetes()
                || (paqueteId != null && descuento.getPaquetes() != null
                    && descuento.getPaquetes().stream().anyMatch(p -> paqueteId.equals(p.getId())));
    }

    /** Pesos que rebaja al precio de lista, redondeados a centavos; nunca más que el precio. */
    public static double monto(Descuento descuento, double precioLista) {
        if (precioLista <= 0) {
            return 0;
        }
        double monto = descuento.getTipo() == TipoDescuento.MONTO_FIJO
                ? descuento.getValor()
                : precioLista * descuento.getValor() / 100.0;
        return Dinero.aCentavos(Math.min(Math.max(monto, 0), precioLista));
    }

    /** El que más rebaja de los que aplican; en empate, el más antiguo. Vacío si ninguno rebaja nada. */
    public static Optional<Descuento> mejor(Collection<Descuento> descuentos, Long paqueteId, LocalDate fechaEvento,
                                            LocalDate hoy, double precioLista) {
        if (descuentos == null) {
            return Optional.empty();
        }
        return descuentos.stream()
                .filter(d -> aplica(d, paqueteId, fechaEvento, hoy))
                .filter(d -> monto(d, precioLista) > 0)
                .min(Comparator.comparingDouble((Descuento d) -> -monto(d, precioLista))
                        .thenComparing(Descuento::getId, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /** Ya no puede aplicar a cotizaciones nuevas: terminó su temporada o su plazo para cotizar. */
    public static boolean vencido(Descuento descuento, LocalDate hoy) {
        return (descuento.getFechaFin() != null && descuento.getFechaFin().isBefore(hoy))
                || (descuento.getCotizarHasta() != null && descuento.getCotizarHasta().isBefore(hoy));
    }
}
