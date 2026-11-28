package com.example.negocio.precio;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.DetalleCotizacion;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.dominio.PaquetePrecio;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.DescuentoRepository;
import com.mycompany.persistencia.repository.PaqueteRepository;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Único lugar donde se decide cuánto cuesta un paquete en una cotización.
 *
 * <ul>
 *   <li>Paquete de precio fijo: su precio ({@code Paquete.costo}); no lleva comida ni personas.</li>
 *   <li>Paquete con tabla de precios: el renglón de la comida y el número de personas elegidos.</li>
 *   <li>Descuentos: al precio de lista se le resta el descuento que más rebaje según la fecha
 *       del evento (ver {@link ReglaDescuento}). Sin fecha no hay descuento.</li>
 *   <li>Cotización ya guardada: si no cambió el paquete, la comida, las personas ni la fecha,
 *       se respeta el precio pactado (con su descuento) aunque el catálogo o los descuentos
 *       hayan cambiado después.</li>
 * </ul>
 *
 * Lo usan la pantalla de cotización (para mostrar el precio) y los casos de uso que guardan
 * (que nunca toman el precio de la pantalla), así ambos dan siempre el mismo resultado.
 */
@Service
@RequiredArgsConstructor
public class CalcularPrecioPaqueteUseCase {

    private final PaqueteRepository paqueteRepository;
    private final CotizacionRepository cotizacionRepository;
    private final DescuentoRepository descuentoRepository;

    /** Precio de catálogo, con el descuento de la fecha, para una cotización nueva. */
    @Transactional(readOnly = true)
    public PrecioPaquete calcular(SolicitudPrecioPaquete solicitud) {
        Paquete paquete = buscarPaquete(solicitud.paqueteId());
        return precioDeCatalogo(paquete, solicitud.comidaId(), solicitud.personas(), solicitud.fechaEvento());
    }

    /**
     * Precio para una cotización que se está editando, con las mismas reglas que
     * {@link #precioParaCotizacion}. Con {@code cotizacionId} nulo equivale a {@link #calcular}.
     */
    @Transactional(readOnly = true)
    public PrecioPaquete calcularParaCotizacion(Long cotizacionId, SolicitudPrecioPaquete solicitud) {
        Cotizacion existente = cotizacionId == null ? null : cotizacionRepository.findById(cotizacionId).orElse(null);
        if (existente == null) {
            return calcular(solicitud);
        }
        Paquete paquete = existente.getPaquete() != null
                && Objects.equals(existente.getPaquete().getId(), solicitud.paqueteId())
                ? existente.getPaquete()
                : buscarPaquete(solicitud.paqueteId());
        return precioParaCotizacion(existente, paquete, solicitud.comidaId(), solicitud.personas(),
                solicitud.fechaEvento());
    }

    /**
     * Precio de lista del paquete (sin descuento), dentro de la transacción de quien llama.
     *
     * @throws CotizacionException si el paquete ya no se ofrece o la selección no corresponde a su forma de precio
     */
    public double precioDeLista(Paquete paquete, Long comidaId, Integer personas) {
        if (paquete == null) {
            throw new CotizacionException("Debe seleccionar un paquete base.");
        }
        if (!paquete.isActivo()) {
            throw new CotizacionException("El paquete \"" + paquete.getNombre() + "\" ya no se ofrece. Elija otro paquete.");
        }
        if (!paquete.tieneTablaDePrecios()) {
            if (comidaId != null || personas != null) {
                throw new CotizacionException("El paquete \"" + paquete.getNombre()
                        + "\" tiene precio fijo: no se elige comida ni número de personas.");
            }
            return Dinero.aCentavos(paquete.getCosto());
        }
        if (comidaId == null) {
            throw new CotizacionException("Elija la comida del paquete \"" + paquete.getNombre() + "\".");
        }
        if (personas == null) {
            throw new CotizacionException("Elija el número de personas del paquete \"" + paquete.getNombre() + "\".");
        }
        return paquete.getPrecios().stream()
                .filter(pp -> esComida(pp, comidaId) && pp.getPersonas() == personas)
                .findFirst()
                .map(pp -> Dinero.aCentavos(pp.getPrecio()))
                .orElseThrow(() -> new CotizacionException("El paquete \"" + paquete.getNombre()
                        + "\" no tiene precio para " + nombreComida(paquete, comidaId) + " con " + personas + " personas."));
    }

    /**
     * Precio de catálogo con el descuento que corresponda a la fecha del evento, dentro de la
     * transacción de quien llama.
     *
     * @throws CotizacionException igual que {@link #precioDeLista}
     */
    public PrecioPaquete precioDeCatalogo(Paquete paquete, Long comidaId, Integer personas, LocalDate fechaEvento) {
        return conDescuento(paquete, precioDeLista(paquete, comidaId, personas), fechaEvento);
    }

    /**
     * Precio que se guarda al editar una cotización, dentro de la transacción de quien llama.
     * Debe llamarse antes de modificar la cotización, porque compara contra lo guardado.
     *
     * <ul>
     *   <li>Mismo paquete, comida y personas, y misma fecha: el precio pactado, con su descuento.</li>
     *   <li>Lo mismo en un borrador al que se le cambió la fecha: el precio de lista pactado,
     *       con el descuento que toque en la fecha nueva.</li>
     *   <li>En una cotización confirmada el cambio de fecha no mueve el precio (como al reprogramar).</li>
     *   <li>Otro paquete, comida o personas: el precio de catálogo actual con el descuento de la fecha.</li>
     * </ul>
     */
    public PrecioPaquete precioParaCotizacion(Cotizacion existente, Paquete paquete, Long comidaId, Integer personas,
                                              LocalDate fechaEvento) {
        if (existente != null && paquete != null && mismoPaquete(existente, paquete.getId(), comidaId, personas)) {
            if (!esBorrador(existente) || Objects.equals(fechaEvento(existente), fechaEvento)) {
                return pactado(existente);
            }
            return conDescuento(paquete, pactado(existente).precioLista(), fechaEvento);
        }
        return precioDeCatalogo(paquete, comidaId, personas, fechaEvento);
    }

    /** Precio pactado del paquete con su descuento, tal como está guardado en la cotización. */
    public static PrecioPaquete pactado(Cotizacion cotizacion) {
        double precio = precioPactado(cotizacion);
        double monto = cotizacion.getDescuentoMonto() != null ? cotizacion.getDescuentoMonto() : 0;
        double lista = cotizacion.getPrecioLista() != null
                ? cotizacion.getPrecioLista()
                : Dinero.aCentavos(precio + monto);
        return new PrecioPaquete(precio, true, lista, monto > 0 ? cotizacion.getDescuentoNombre() : null, monto);
    }

    /**
     * Precio pactado del paquete (ya con descuento). En cotizaciones anteriores a este dato se
     * deduce del total menos los extras, igual que siempre lo mostró el estado de cuenta.
     */
    public static double precioPactado(Cotizacion cotizacion) {
        if (cotizacion.getPrecioPaquete() != null) {
            return cotizacion.getPrecioPaquete();
        }
        double extras = cotizacion.getServiciosExtra() == null ? 0 : cotizacion.getServiciosExtra().stream()
                .mapToDouble(DetalleCotizacion::getSubtotal)
                .sum();
        return Dinero.aCentavos(Math.max(0, cotizacion.getTotal() - extras));
    }

    private PrecioPaquete conDescuento(Paquete paquete, double precioLista, LocalDate fechaEvento) {
        if (paquete == null || fechaEvento == null) {
            return PrecioPaquete.sinDescuento(precioLista, false);
        }
        return ReglaDescuento.mejor(descuentoRepository.findByActivoTrue(), paquete.getId(), fechaEvento,
                        LocalDate.now(), precioLista)
                .map(d -> {
                    double monto = ReglaDescuento.monto(d, precioLista);
                    return new PrecioPaquete(Dinero.aCentavos(precioLista - monto), false, precioLista,
                            d.getNombre(), monto);
                })
                .orElseGet(() -> PrecioPaquete.sinDescuento(precioLista, false));
    }

    /** Indica si la cotización ya tiene ese paquete, comida y personas (entonces se respeta lo pactado). */
    public static boolean mismoPaquete(Cotizacion cotizacion, Long paqueteId, Long comidaId, Integer personas) {
        Long paqueteActual = cotizacion.getPaquete() != null ? cotizacion.getPaquete().getId() : null;
        Long comidaActual = cotizacion.getComida() != null ? cotizacion.getComida().getId() : null;
        return Objects.equals(paqueteActual, paqueteId)
                && Objects.equals(comidaActual, comidaId)
                && Objects.equals(cotizacion.getPersonas(), personas);
    }

    /** Solo en un borrador cambiar la fecha vuelve a buscar descuento. */
    private static boolean esBorrador(Cotizacion cotizacion) {
        return cotizacion.getEstado() == null || cotizacion.getEstado() == EstadoCotizacion.BORRADOR;
    }

    private static LocalDate fechaEvento(Cotizacion cotizacion) {
        return cotizacion.getEvento() != null ? cotizacion.getEvento().getFecha() : null;
    }

    private Paquete buscarPaquete(Long paqueteId) {
        if (paqueteId == null) {
            throw new CotizacionException("Debe seleccionar un paquete base.");
        }
        return paqueteRepository.findById(paqueteId)
                .orElseThrow(() -> new CotizacionException("El paquete seleccionado no existe."));
    }

    private static boolean esComida(PaquetePrecio precio, Long comidaId) {
        return precio.getComida() != null && comidaId.equals(precio.getComida().getId());
    }

    private static String nombreComida(Paquete paquete, Long comidaId) {
        Optional<String> nombre = paquete.getPrecios().stream()
                .filter(pp -> esComida(pp, comidaId))
                .map(pp -> pp.getComida().getNombre())
                .findFirst();
        return nombre.map(n -> "\"" + n + "\"").orElse("esa comida");
    }
}
