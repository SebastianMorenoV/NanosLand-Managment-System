package com.example.negocio.precio;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.DetalleCotizacion;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.dominio.PaquetePrecio;
import com.mycompany.persistencia.repository.CotizacionRepository;
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
 *   <li>Cotización ya guardada: si no cambió el paquete, la comida ni las personas, se respeta
 *       el precio pactado aunque el catálogo haya cambiado después.</li>
 * </ul>
 *
 * Lo usan la pantalla de cotización (para mostrar el precio) y los casos de uso que guardan
 * (que nunca toman el precio de la pantalla), así ambos dan siempre el mismo resultado.
 * Aquí se conectarán después los descuentos por temporada o día de la semana.
 */
@Service
@RequiredArgsConstructor
public class CalcularPrecioPaqueteUseCase {

    private final PaqueteRepository paqueteRepository;
    private final CotizacionRepository cotizacionRepository;

    /** Precio de catálogo para una cotización nueva. */
    @Transactional(readOnly = true)
    public PrecioPaquete calcular(SolicitudPrecioPaquete solicitud) {
        Paquete paquete = buscarPaquete(solicitud.paqueteId());
        return new PrecioPaquete(
                precioDeLista(paquete, solicitud.comidaId(), solicitud.personas(), solicitud.fechaEvento()), false);
    }

    /**
     * Precio para una cotización que se está editando: el pactado si la selección no cambió,
     * o el de catálogo si cambió. Con {@code cotizacionId} nulo equivale a {@link #calcular}.
     */
    @Transactional(readOnly = true)
    public PrecioPaquete calcularParaCotizacion(Long cotizacionId, SolicitudPrecioPaquete solicitud) {
        Cotizacion existente = cotizacionId == null ? null : cotizacionRepository.findById(cotizacionId).orElse(null);
        if (existente != null && mismaSeleccion(existente, solicitud.paqueteId(), solicitud.comidaId(), solicitud.personas())) {
            return new PrecioPaquete(precioPactado(existente), true);
        }
        return calcular(solicitud);
    }

    /**
     * Precio de catálogo del paquete, dentro de la transacción de quien llama.
     *
     * @throws CotizacionException si el paquete ya no se ofrece o la selección no corresponde a su forma de precio
     */
    public double precioDeLista(Paquete paquete, Long comidaId, Integer personas, LocalDate fechaEvento) {
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
     * Precio que se guarda al editar una cotización, dentro de la transacción de quien llama:
     * el pactado si la selección no cambió, o el de catálogo si cambió.
     * Debe llamarse antes de modificar la cotización.
     */
    public double precioParaCotizacion(Cotizacion existente, Paquete paquete, Long comidaId, Integer personas,
                                       LocalDate fechaEvento) {
        if (existente != null && paquete != null
                && mismaSeleccion(existente, paquete.getId(), comidaId, personas)) {
            return precioPactado(existente);
        }
        return precioDeLista(paquete, comidaId, personas, fechaEvento);
    }

    /**
     * Precio pactado del paquete. En cotizaciones anteriores a este dato se deduce del total
     * menos los extras, igual que siempre lo mostró el estado de cuenta.
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

    // Por ahora la fecha no entra: el precio no depende del día. Cuando haya descuentos por
    // día de la semana o temporada, cambiar la fecha también deberá contar como otra selección.
    private static boolean mismaSeleccion(Cotizacion cotizacion, Long paqueteId, Long comidaId, Integer personas) {
        Long paqueteActual = cotizacion.getPaquete() != null ? cotizacion.getPaquete().getId() : null;
        Long comidaActual = cotizacion.getComida() != null ? cotizacion.getComida().getId() : null;
        return Objects.equals(paqueteActual, paqueteId)
                && Objects.equals(comidaActual, comidaId)
                && Objects.equals(cotizacion.getPersonas(), personas);
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
