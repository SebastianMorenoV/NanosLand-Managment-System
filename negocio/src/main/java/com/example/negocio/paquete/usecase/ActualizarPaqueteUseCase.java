package com.example.negocio.paquete.usecase;

import com.example.negocio.exception.CotizacionException;
import com.example.negocio.precio.Dinero;
import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.mapper.PaqueteMapper;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.dominio.PaquetePrecio;
import com.mycompany.persistencia.dominio.PaqueteServicio;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.PaqueteRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Modifica un paquete.
 *
 * Cada cotización guarda su precio pactado y su copia del paquete (nombre, descripción y servicios
 * incluidos), así que el paquete se puede cambiar aunque tenga cotizaciones abiertas: los cambios
 * valen para las cotizaciones nuevas y las abiertas conservan lo que se les vendió.
 * <ul>
 *   <li>Si alguna cotización abierta todavía no tiene su copia (es de antes de la copia y la corrección
 *       al arrancar aún no la llenó), depende del paquete del catálogo: mientras tanto no se cambian
 *       los servicios incluidos ni se quita de la tabla la combinación de comida y personas que use.</li>
 *   <li>No se pasa de precio fijo a tabla (o al revés) si alguna cotización abierta depende de la forma
 *       actual: al editarla, la pantalla no podría mostrar su selección y se volvería a cotizar.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ActualizarPaqueteUseCase {

    static final List<EstadoCotizacion> ESTADOS_ABIERTOS = List.of(EstadoCotizacion.BORRADOR, EstadoCotizacion.VIGENTE);

    private final PaqueteRepository paqueteRepository;
    private final CotizacionRepository cotizacionRepository;
    private final ValidadorPaquete validador;

    /**
     * @param paqueteDTO datos nuevos. Si {@code servicios} o {@code precios} vienen en {@code null},
     *                   esa parte no se modifica.
     */
    @Transactional
    public PaqueteDTO actualizarPaquete(PaqueteDTO paqueteDTO) {
        if (paqueteDTO == null || paqueteDTO.getId() == null) {
            throw new CotizacionException("El ID del paquete es obligatorio para actualizar.");
        }
        Paquete p = paqueteRepository.findById(paqueteDTO.getId())
            .filter(Paquete::isActivo)
            .orElseThrow(() -> new CotizacionException("El paquete no existe o no está activo."));

        boolean cambianPrecios = paqueteDTO.getPrecios() != null;
        boolean cambianServicios = paqueteDTO.getServicios() != null;
        List<ValidadorPaquete.Precio> precios = cambianPrecios ? validador.precios(paqueteDTO.getPrecios()) : null;
        boolean conTabla = cambianPrecios ? !precios.isEmpty() : p.tieneTablaDePrecios();
        validador.validarDatosGenerales(paqueteDTO, conTabla);
        List<ValidadorPaquete.Incluido> incluidos = cambianServicios ? validador.serviciosIncluidos(paqueteDTO.getServicios()) : null;
        boolean serviciosDistintos = cambianServicios && !mismosServicios(p, incluidos);

        List<Object[]> abiertas = cotizacionRepository.combinacionesAbiertasDePaquete(
            p.getId(), ESTADOS_ABIERTOS, EstadoEvento.FINALIZADO);
        if (!abiertas.isEmpty()) {
            // Las que aún no guardan su copia del paquete dependen de cómo está en el catálogo.
            Set<String> sinCopia = new LinkedHashSet<>(cotizacionRepository.foliosAbiertosSinCopiaDelPaquete(
                p.getId(), ESTADOS_ABIERTOS, EstadoEvento.FINALIZADO));
            if (serviciosDistintos && !sinCopia.isEmpty()) {
                throw new CotizacionException("Todavía no se pueden cambiar los servicios incluidos: estas cotizaciones "
                    + "abiertas no guardan su copia del paquete (" + String.join(", ", sinCopia) + "). Cierre y vuelva "
                    + "a abrir el sistema para que se guarde. El nombre, la descripción y los precios sí se pueden cambiar.");
            }
            validarCombinacionesAbiertas(p, abiertas, conTabla, precios, sinCopia);
        }

        p.setNombre(paqueteDTO.getNombre().trim());
        p.setDescripcion(paqueteDTO.getDescripcion());
        p.setCosto(conTabla ? 0 : Dinero.aCentavos(paqueteDTO.getCostoBase()));
        if (serviciosDistintos) {
            validador.aplicarServicios(p, incluidos);
        }
        if (cambianPrecios) {
            validador.aplicarPrecios(p, precios);
        }

        Paquete guardado = paqueteRepository.save(p);
        return PaqueteMapper.toDTO(guardado);
    }

    /**
     * Las cotizaciones abiertas deben poder seguir editándose: la forma de precio (fija o por tabla)
     * no cambia si alguna depende de ella, y las que no tienen su copia del paquete conservan su
     * combinación de comida y personas en la tabla.
     */
    private void validarCombinacionesAbiertas(Paquete p, List<Object[]> abiertas, boolean conTabla,
                                              List<ValidadorPaquete.Precio> precios, Set<String> sinCopia) {
        List<String> deFijo = new ArrayList<>();
        Map<String, List<String>> combinacionesQuitadas = new LinkedHashMap<>();
        Set<String> disponibles = precios == null ? null : precios.stream()
            .map(pr -> ValidadorPaquete.clave(pr.comida().getId(), pr.personas()))
            .collect(Collectors.toSet());

        for (Object[] fila : abiertas) {
            String folio = (String) fila[0];
            Long comidaId = (Long) fila[1];
            Integer personas = (Integer) fila[2];
            if (comidaId == null) {
                if (conTabla) deFijo.add(folio);
            } else if (!conTabla) {
                combinacionesQuitadas.computeIfAbsent(describir(p, comidaId, personas), k -> new ArrayList<>()).add(folio);
            } else if (sinCopia.contains(folio) && disponibles != null
                    && !disponibles.contains(ValidadorPaquete.clave(comidaId, personas == null ? 0 : personas))) {
                combinacionesQuitadas.computeIfAbsent(describir(p, comidaId, personas), k -> new ArrayList<>()).add(folio);
            }
        }
        if (!deFijo.isEmpty()) {
            throw new CotizacionException("No se puede pasar el paquete a tabla de precios porque estas cotizaciones "
                + "abiertas lo tienen con precio fijo: " + String.join(", ", deFijo) + ".");
        }
        if (!combinacionesQuitadas.isEmpty()) {
            String detalle = combinacionesQuitadas.entrySet().stream()
                .map(e -> e.getKey() + " (" + String.join(", ", e.getValue()) + ")")
                .collect(Collectors.joining("; "));
            throw new CotizacionException("No se puede quitar de la tabla de precios lo que usan cotizaciones abiertas: "
                + detalle + ".");
        }
    }

    private static boolean mismosServicios(Paquete p, List<ValidadorPaquete.Incluido> incluidos) {
        Map<Long, Integer> actuales = new LinkedHashMap<>();
        if (p.getServicios() != null) {
            for (PaqueteServicio ps : p.getServicios()) {
                if (ps.getServicio() != null) actuales.merge(ps.getServicio().getId(), ps.getCantidad(), Integer::sum);
            }
        }
        Map<Long, Integer> nuevos = new LinkedHashMap<>();
        for (ValidadorPaquete.Incluido inc : incluidos) {
            nuevos.merge(inc.servicio().getId(), inc.cantidad(), Integer::sum);
        }
        return actuales.equals(nuevos);
    }

    private static String describir(Paquete p, Long comidaId, Integer personas) {
        String comida = p.getPrecios().stream()
            .map(PaquetePrecio::getComida)
            .filter(c -> c != null && comidaId.equals(c.getId()))
            .map(c -> c.getNombre())
            .findFirst().orElse("comida " + comidaId);
        return comida + " con " + personas + " personas";
    }
}
