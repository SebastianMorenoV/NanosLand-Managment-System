package com.example.negocio.paquete.usecase;

import com.example.negocio.exception.CotizacionException;
import com.example.negocio.precio.Dinero;
import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.dtos.PaquetePrecioDTO;
import com.mycompany.common.dtos.PaqueteServicioDTO;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.dominio.PaquetePrecio;
import com.mycompany.persistencia.dominio.PaqueteServicio;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.enums.CategoriaServicio;
import com.mycompany.persistencia.repository.ServicioRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Reglas comunes para dar de alta y modificar paquetes: datos generales, servicios
 * incluidos y tabla de precios. Trabaja con servicios cargados de la base (no con los
 * que manda la pantalla) para validar que existan, estén activos y sean de la categoría correcta.
 */
@Component
@RequiredArgsConstructor
class ValidadorPaquete {

    /** Un servicio incluido ya validado. */
    record Incluido(Servicio servicio, int cantidad) {
    }

    /** Un renglón de la tabla de precios ya validado. */
    record Precio(Servicio comida, int personas, double precio) {
    }

    private final ServicioRepository servicioRepository;

    void validarDatosGenerales(PaqueteDTO dto, boolean conTabla) {
        if (dto == null) {
            throw new CotizacionException("Faltan los datos del paquete.");
        }
        if (dto.getNombre() == null || dto.getNombre().isBlank()) {
            throw new CotizacionException("El nombre del paquete es obligatorio.");
        }
        if (dto.getNombre().trim().length() > 255) {
            throw new CotizacionException("El nombre del paquete es demasiado largo (máximo 255 caracteres).");
        }
        if (dto.getDescripcion() != null && dto.getDescripcion().length() > 255) {
            throw new CotizacionException("La descripción es demasiado larga (máximo 255 caracteres).");
        }
        if (!conTabla && !(dto.getCostoBase() > 0)) {
            throw new CotizacionException("El precio del paquete debe ser mayor a cero.");
        }
    }

    /** Servicios incluidos: existentes, activos, que no sean comidas y con cantidad positiva. */
    List<Incluido> serviciosIncluidos(List<PaqueteServicioDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) return List.of();
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (PaqueteServicioDTO ps : dtos) {
            if (ps == null || ps.getServicio() == null || ps.getServicio().getId() == null) {
                throw new CotizacionException("Uno de los servicios incluidos no es válido.");
            }
            if (ps.getCantidad() <= 0) {
                throw new CotizacionException("La cantidad de \"" + ps.getServicio().getNombre() + "\" debe ser mayor a cero.");
            }
            cantidades.merge(ps.getServicio().getId(), ps.getCantidad(), Integer::sum);
        }
        Map<Long, Servicio> servicios = cargar(cantidades.keySet());
        List<Incluido> incluidos = new ArrayList<>();
        cantidades.forEach((id, cantidad) -> {
            Servicio s = servicios.get(id);
            if (s == null || !s.isActivo()) {
                throw new CotizacionException("Uno de los servicios incluidos ya no existe en el catálogo.");
            }
            if (s.getCategoria() == CategoriaServicio.COMIDA) {
                throw new CotizacionException("\"" + s.getNombre()
                    + "\" es una comida: va en la tabla de precios del paquete, no en los servicios incluidos.");
            }
            incluidos.add(new Incluido(s, cantidad));
        });
        return incluidos;
    }

    /** Tabla de precios: comidas activas de categoría Comida, personas y precio positivos, sin repetir combinación. */
    List<Precio> precios(List<PaquetePrecioDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) return List.of();
        Set<Long> ids = new HashSet<>();
        for (PaquetePrecioDTO p : dtos) {
            if (p == null || p.getComidaId() == null) {
                throw new CotizacionException("Un renglón de la tabla de precios no tiene comida.");
            }
            ids.add(p.getComidaId());
        }
        Map<Long, Servicio> comidas = cargar(ids);
        Set<String> combinaciones = new HashSet<>();
        List<Precio> precios = new ArrayList<>();
        for (PaquetePrecioDTO p : dtos) {
            Servicio comida = comidas.get(p.getComidaId());
            if (comida == null || !comida.isActivo()) {
                throw new CotizacionException("Una de las comidas de la tabla de precios ya no existe en el catálogo.");
            }
            if (comida.getCategoria() != CategoriaServicio.COMIDA) {
                throw new CotizacionException("\"" + comida.getNombre()
                    + "\" no es de categoría Comida; cámbiela en Servicios para usarla en la tabla de precios.");
            }
            if (p.getPersonas() <= 0) {
                throw new CotizacionException("El número de personas debe ser mayor a cero.");
            }
            double precio = Dinero.aCentavos(p.getPrecio());
            if (Double.isNaN(precio) || !(precio > 0)) {
                throw new CotizacionException("Falta el precio de \"" + comida.getNombre() + "\" para "
                    + p.getPersonas() + " personas (debe ser mayor a cero).");
            }
            if (!combinaciones.add(comida.getId() + "|" + p.getPersonas())) {
                throw new CotizacionException("\"" + comida.getNombre() + "\" para " + p.getPersonas()
                    + " personas está repetido en la tabla de precios.");
            }
            precios.add(new Precio(comida, p.getPersonas(), precio));
        }
        return precios;
    }

    /** Reemplaza los servicios incluidos del paquete; el subtotal se calcula con el precio del catálogo. */
    void aplicarServicios(Paquete paquete, List<Incluido> incluidos) {
        if (paquete.getServicios() == null) {
            paquete.setServicios(new ArrayList<>());
        }
        paquete.getServicios().clear();
        for (Incluido inc : incluidos) {
            PaqueteServicio ps = new PaqueteServicio();
            ps.setPaquete(paquete);
            ps.setServicio(inc.servicio());
            ps.setCantidad(inc.cantidad());
            ps.setSubtotal(Dinero.aCentavos(inc.servicio().getPrecio() * inc.cantidad()));
            paquete.getServicios().add(ps);
        }
    }

    /**
     * Actualiza la tabla de precios en su lugar por (comida, personas): cambia el precio de los
     * renglones que siguen, agrega los nuevos y quita los que ya no vienen. No reemplaza la lista,
     * porque es una colección con orphanRemoval que Hibernate está siguiendo.
     */
    void aplicarPrecios(Paquete paquete, List<Precio> precios) {
        List<PaquetePrecio> actuales = paquete.getPrecios();
        Map<String, Precio> nuevos = precios.stream()
            .collect(Collectors.toMap(p -> clave(p.comida().getId(), p.personas()), Function.identity(),
                (a, b) -> a, LinkedHashMap::new));
        actuales.removeIf(pp -> !nuevos.containsKey(clave(pp.getComida().getId(), pp.getPersonas())));
        for (Precio p : nuevos.values()) {
            PaquetePrecio existente = actuales.stream()
                .filter(pp -> pp.getPersonas() == p.personas() && Objects.equals(pp.getComida().getId(), p.comida().getId()))
                .findFirst().orElse(null);
            if (existente != null) {
                existente.setPrecio(p.precio());
            } else {
                PaquetePrecio nuevo = new PaquetePrecio();
                nuevo.setPaquete(paquete);
                nuevo.setComida(p.comida());
                nuevo.setPersonas(p.personas());
                nuevo.setPrecio(p.precio());
                actuales.add(nuevo);
            }
        }
    }

    static String clave(Long comidaId, int personas) {
        return comidaId + "|" + personas;
    }

    private Map<Long, Servicio> cargar(Set<Long> ids) {
        return servicioRepository.findAllById(ids).stream()
            .collect(Collectors.toMap(Servicio::getId, Function.identity()));
    }
}
