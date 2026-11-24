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
 * Cada cotización guarda su precio pactado, así que el nombre, la descripción y los precios
 * (fijo o de la tabla) se pueden cambiar aunque haya cotizaciones abiertas. Mientras las haya,
 * no se pueden cambiar los servicios incluidos (la logística de esos eventos depende de ellos),
 * ni quitar de la tabla una combinación de comida y personas que alguna use, ni pasar de precio
 * fijo a tabla (o al revés) si alguna depende de la forma actual.
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
            if (serviciosDistintos) {
                throw new CotizacionException("No se pueden cambiar los servicios incluidos porque el paquete tiene "
                    + "cotizaciones abiertas (" + folios(abiertas) + "). El nombre, la descripción y los precios sí "
                    + "se pueden cambiar; para otros servicios cree un paquete nuevo.");
            }
            validarCombinacionesAbiertas(p, abiertas, conTabla, precios);
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

    /** Las cotizaciones abiertas deben poder seguir cotizándose con la forma de precio y combinaciones nuevas. */
    private void validarCombinacionesAbiertas(Paquete p, List<Object[]> abiertas, boolean conTabla,
                                              List<ValidadorPaquete.Precio> precios) {
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
            } else if (disponibles != null && !disponibles.contains(ValidadorPaquete.clave(comidaId, personas == null ? 0 : personas))) {
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

    private static String folios(List<Object[]> abiertas) {
        Set<String> folios = new LinkedHashSet<>();
        for (Object[] fila : abiertas) folios.add((String) fila[0]);
        return String.join(", ", folios);
    }
}
