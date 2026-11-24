package com.example.negocio.descuento.usecase;

import com.example.negocio.exception.CotizacionException;
import com.example.negocio.precio.Dinero;
import com.example.negocio.precio.ReglaDescuento;
import com.mycompany.common.dtos.DescuentoDTO;
import com.mycompany.common.mapper.DescuentoMapper;
import com.mycompany.persistencia.dominio.Descuento;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.enums.TipoDescuento;
import com.mycompany.persistencia.repository.DescuentoRepository;
import com.mycompany.persistencia.repository.PaqueteRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta, edición y baja de los descuentos automáticos a paquetes.
 *
 * Las cotizaciones guardan el nombre y el monto del descuento que tuvieron, así que
 * editar o borrar un descuento no cambia las cotizaciones ya hechas.
 */
@Service
@RequiredArgsConstructor
public class GestionarDescuentoUseCase {

    static final int LARGO_NOMBRE = 255;
    static final double MAXIMO_MONTO = 9_999_999;

    private final DescuentoRepository descuentoRepository;
    private final PaqueteRepository paqueteRepository;

    /** Todos los descuentos con su estado a hoy, por nombre. */
    @Transactional(readOnly = true)
    public List<DescuentoDTO> obtenerTodos() {
        LocalDate hoy = LocalDate.now();
        return descuentoRepository.findAll().stream()
                .sorted(Comparator.comparing(Descuento::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(d -> aDTO(d, hoy))
                .toList();
    }

    @Transactional
    public DescuentoDTO crear(DescuentoDTO dto) {
        validar(dto, null);
        Descuento descuento = new Descuento();
        copiar(dto, descuento);
        return aDTO(descuentoRepository.save(descuento), LocalDate.now());
    }

    @Transactional
    public DescuentoDTO actualizar(Long id, DescuentoDTO dto) {
        Descuento descuento = obtener(id);
        validar(dto, id);
        copiar(dto, descuento);
        return aDTO(descuentoRepository.save(descuento), LocalDate.now());
    }

    /** Enciende o apaga el descuento sin borrarlo. */
    @Transactional
    public DescuentoDTO cambiarActivo(Long id, boolean activo) {
        Descuento descuento = obtener(id);
        descuento.setActivo(activo);
        return aDTO(descuentoRepository.save(descuento), LocalDate.now());
    }

    /** Borra el descuento. Las cotizaciones que lo usaron conservan su precio y el nombre del descuento. */
    @Transactional
    public void eliminar(Long id) {
        descuentoRepository.delete(obtener(id));
    }

    /**
     * Crea una copia con las fechas un año después, para repetir una temporada
     * (p. ej. "Diciembre 2026" → "Diciembre 2027"). Queda encendida.
     */
    @Transactional
    public DescuentoDTO duplicarAlSiguienteAnio(Long id) {
        Descuento original = obtener(id);
        if (original.getFechaInicio() == null && original.getFechaFin() == null && original.getCotizarHasta() == null) {
            throw new CotizacionException("\"" + original.getNombre()
                    + "\" no tiene fechas: ya aplica todos los años, no hace falta duplicarlo.");
        }
        DescuentoDTO copia = DescuentoMapper.toDTO(original);
        copia.setId(null);
        copia.setNombre(nombreDelSiguienteAnio(original));
        copia.setFechaInicio(masUnAnio(original.getFechaInicio()));
        copia.setFechaFin(masUnAnio(original.getFechaFin()));
        copia.setCotizarHasta(masUnAnio(original.getCotizarHasta()));
        copia.setActivo(true);
        return crear(copia);
    }

    private Descuento obtener(Long id) {
        if (id == null) {
            throw new CotizacionException("Debe indicar el descuento.");
        }
        return descuentoRepository.findById(id)
                .orElseThrow(() -> new CotizacionException("El descuento ya no existe."));
    }

    private void validar(DescuentoDTO dto, Long id) {
        if (dto == null) {
            throw new CotizacionException("Faltan los datos del descuento.");
        }
        String nombre = dto.getNombre() == null ? "" : dto.getNombre().trim();
        if (nombre.isEmpty()) {
            throw new CotizacionException("El nombre del descuento es obligatorio.");
        }
        if (nombre.length() > LARGO_NOMBRE) {
            throw new CotizacionException("El nombre es demasiado largo (máximo " + LARGO_NOMBRE + " caracteres).");
        }
        boolean repetido = id == null
                ? descuentoRepository.existsByNombreIgnoreCase(nombre)
                : descuentoRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id);
        if (repetido) {
            throw new CotizacionException("Ya existe un descuento llamado \"" + nombre + "\". Use otro nombre.");
        }

        if (dto.getTipo() == null) {
            throw new CotizacionException("Elija si el descuento es un porcentaje o un monto fijo.");
        }
        double valor = dto.getValor();
        if (Double.isNaN(valor) || Double.isInfinite(valor) || valor <= 0) {
            throw new CotizacionException(dto.getTipo() == TipoDescuento.PORCENTAJE
                    ? "El porcentaje debe ser mayor que 0." : "El monto debe ser mayor que $0.");
        }
        if (dto.getTipo() == TipoDescuento.PORCENTAJE && valor > 100) {
            throw new CotizacionException("El porcentaje no puede pasar de 100.");
        }
        if (dto.getTipo() == TipoDescuento.MONTO_FIJO && valor > MAXIMO_MONTO) {
            throw new CotizacionException("El monto es demasiado grande.");
        }

        if (dto.getFechaInicio() != null && dto.getFechaFin() != null
                && dto.getFechaFin().isBefore(dto.getFechaInicio())) {
            throw new CotizacionException("La temporada termina antes de empezar: revise las fechas.");
        }
        LocalDate hoy = LocalDate.now();
        if (dto.getFechaFin() != null && dto.getFechaFin().isBefore(hoy)) {
            throw new CotizacionException("La temporada ya terminó: el descuento no aplicaría a ninguna cotización.");
        }
        if (dto.getCotizarHasta() != null && dto.getCotizarHasta().isBefore(hoy)) {
            throw new CotizacionException("La fecha límite para cotizar ya pasó: el descuento no aplicaría a ninguna cotización.");
        }

        if (!dto.isTodosLosPaquetes() && (dto.getPaqueteIds() == null || dto.getPaqueteIds().isEmpty())) {
            throw new CotizacionException("Elija al menos un paquete, o marque que aplica a todos.");
        }
    }

    private void copiar(DescuentoDTO dto, Descuento descuento) {
        descuento.setNombre(dto.getNombre().trim());
        descuento.setTipo(dto.getTipo());
        descuento.setValor(Dinero.aCentavos(dto.getValor()));
        // Los siete días es lo mismo que sin restricción de día.
        Set<DayOfWeek> dias = dto.getDias() == null || dto.getDias().size() == 7
                ? new HashSet<>() : new HashSet<>(dto.getDias());
        reemplazar(descuento.getDias(), dias, descuento::setDias);
        descuento.setFechaInicio(dto.getFechaInicio());
        descuento.setFechaFin(dto.getFechaFin());
        descuento.setCotizarHasta(dto.getCotizarHasta());
        descuento.setTodosLosPaquetes(dto.isTodosLosPaquetes());

        Set<Paquete> paquetes = new LinkedHashSet<>();
        if (!dto.isTodosLosPaquetes()) {
            for (Long paqueteId : new LinkedHashSet<>(dto.getPaqueteIds())) {
                paquetes.add(paqueteRepository.findById(paqueteId)
                        .orElseThrow(() -> new CotizacionException("Uno de los paquetes elegidos ya no existe.")));
            }
        }
        reemplazar(descuento.getPaquetes(), paquetes, descuento::setPaquetes);
        descuento.setActivo(dto.isActivo());
    }

    /** Cambia el contenido de una colección de Hibernate sin reemplazarla. */
    private static <T> void reemplazar(Set<T> actual, Set<T> nuevos, java.util.function.Consumer<Set<T>> asignar) {
        if (actual == null) {
            asignar.accept(nuevos);
            return;
        }
        actual.clear();
        actual.addAll(nuevos);
    }

    private static DescuentoDTO aDTO(Descuento descuento, LocalDate hoy) {
        DescuentoDTO dto = DescuentoMapper.toDTO(descuento);
        dto.setEstado(!descuento.isActivo() ? DescuentoDTO.Estado.DESACTIVADO
                : ReglaDescuento.vencido(descuento, hoy) ? DescuentoDTO.Estado.VENCIDO
                : DescuentoDTO.Estado.ACTIVO);
        return dto;
    }

    /** Cambia el año de la temporada en el nombre, o lo agrega si no lo tiene. */
    static String nombreDelSiguienteAnio(Descuento original) {
        LocalDate referencia = original.getFechaInicio() != null ? original.getFechaInicio()
                : original.getFechaFin() != null ? original.getFechaFin() : original.getCotizarHasta();
        String anio = String.valueOf(referencia.getYear());
        String siguiente = String.valueOf(referencia.getYear() + 1);
        String nombre = original.getNombre();
        String nuevo = nombre.contains(anio) ? nombre.replace(anio, siguiente) : nombre + " " + siguiente;
        return nuevo.length() > LARGO_NOMBRE ? nuevo.substring(0, LARGO_NOMBRE) : nuevo;
    }

    private static LocalDate masUnAnio(LocalDate fecha) {
        return fecha == null ? null : fecha.plusYears(1);
    }
}
