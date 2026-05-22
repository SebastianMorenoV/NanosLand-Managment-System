package com.example.negocio.cotizacion.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.CotizacionDTO;
import com.mycompany.common.dtos.DetalleCotizacionDTO;
import com.mycompany.common.mapper.CotizacionMapper;
import com.mycompany.persistencia.dominio.DetalleCotizacion;
import com.mycompany.persistencia.dominio.Cliente;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.repository.ClienteRepository;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.PaqueteRepository;
import com.mycompany.persistencia.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/**
 * Caso de uso: Crear una nueva cotización.
 *
 * Responsabilidades:
 * - Validar todos los campos obligatorios del DTO.
 * - Verificar que el cliente y el paquete existan en la base de datos.
 * - Verificar que la fecha del evento sea futura (no en el pasado).
 * - Verificar que el turno no esté duplicado en la misma fecha.
 * - Generar un folio único con formato COT-YYYY-NNNN.
 * - Calcular el total base desde el costo del paquete.
 * - Asignar el estado inicial BORRADOR.
 * - Persistir la cotización.
 */
@Service
@RequiredArgsConstructor
public class CrearCotizacionUseCase {

    private final CotizacionRepository cotizacionRepository;
    private final ClienteRepository clienteRepository;
    private final PaqueteRepository paqueteRepository;
    private final ServicioRepository servicioRepository;
    private final com.mycompany.persistencia.repository.EventoRepository eventoRepository;

    /**
     * Crea y persiste una cotización con estado BORRADOR.
     *
     * @param dto DTO con los datos capturados desde la presentación.
     * @return DTO enriquecido con id y folio generados.
     * @throws CotizacionException si alguna validación de negocio falla.
     */
    @Transactional
    public CotizacionDTO crearCotizacion(CotizacionDTO dto) {

        // ── 1. Validaciones de campos obligatorios ───────────────────────────
        if (dto == null) {
            throw new CotizacionException("El objeto de cotización no puede ser nulo.");
        }
        if (dto.getClienteId() == null) {
            throw new CotizacionException("Debe seleccionar un cliente para la cotización.");
        }
        if (dto.getFecha() == null) {
            throw new CotizacionException("Debe seleccionar una fecha para el evento.");
        }
        if (dto.getTurno() == null) {
            throw new CotizacionException("Debe seleccionar un turno (Matutino o Vespertino).");
        }
        if (dto.getPaqueteId() == null) {
            throw new CotizacionException("Debe seleccionar un paquete base para la cotización.");
        }

        // ── 1.1 Validaciones de longitud para evitar Data Truncation (VARCHAR 255) ──
        if (dto.getNombreFestejado() != null && dto.getNombreFestejado().length() > 255) {
            throw new CotizacionException("El nombre del festejado es demasiado largo (máximo 255 caracteres).");
        }
        if (dto.getTematica() != null && dto.getTematica().length() > 255) {
            throw new CotizacionException("La temática es demasiado larga (máximo 255 caracteres).");
        }
        if (dto.getNotas() != null && dto.getNotas().length() > 255) {
            throw new CotizacionException("Las notas del evento son demasiado largas (máximo 255 caracteres).");
        }

        // ── 2. Validación: la fecha debe ser presente o futura ───────────────
        if (dto.getFecha().isBefore(LocalDate.now())) {
            throw new CotizacionException(
                "La fecha del evento no puede ser en el pasado. Seleccione una fecha válida."
            );
        }

        // ── 3. Verificación de existencia del cliente ────────────────────────
        Cliente cliente = clienteRepository.findById(dto.getClienteId())
            .orElseThrow(() -> new CotizacionException(
                "El cliente con ID " + dto.getClienteId() + " no existe en el sistema."
            ));

        // ── 4. Verificación de existencia del paquete ────────────────────────
        Paquete paquete = paqueteRepository.findById(dto.getPaqueteId())
            .orElseThrow(() -> new CotizacionException(
                "El paquete con ID " + dto.getPaqueteId() + " no existe en el catálogo."
            ));

        // ── 5. Validación: no duplicar turno en la misma fecha ───────────────
        // Usamos EventoRepository en lugar de CotizacionRepository ya que la fecha vive en Evento.
       boolean turnoOcupado = eventoRepository.existsByFechaAndTurnoAndEstadoNot(
            dto.getFecha(),
            dto.getTurno(),
            EstadoEvento.CANCELADO
        );
        if (turnoOcupado) {
            throw new CotizacionException("Ya existe un evento confirmado para esta fecha y turno. Seleccione otro turno.");
        }

        // ── 6. Construcción de la entidad ────────────────────────────────────
        Cotizacion cotizacion = CotizacionMapper.toEntity(dto);
        cotizacion.setCliente(cliente);
        cotizacion.setPaquete(paquete);
        EstadoCotizacion estado = dto.getEstado() != null ? dto.getEstado() : EstadoCotizacion.BORRADOR;
        cotizacion.setEstado(estado);

        Evento evento = new Evento();
        evento.setFecha(dto.getFecha());
        evento.setTurno(dto.getTurno());
        evento.setNombreFestejado(dto.getNombreFestejado());
        evento.setTematica(dto.getTematica());
        evento.setEstado(EstadoEvento.TENTATIVO);
        evento.setCotizacion(cotizacion);
        cotizacion.setEvento(evento);

        // ── 7. Generación de folio único COT-YYYY-NNNN ───────────────────────
        String folio = generarFolio();
        cotizacion.setFolio(folio);

        // ── 8. Cálculo del total base (costo del paquete) + extras ───────────
        double total = paquete.getCosto();

        // Persistimos los servicios extra (si se capturaron) y recalculamos el total.
        List<DetalleCotizacionDTO> detallesDTO = dto.getDetalles();
        List<DetalleCotizacion> detallesEntidad = new ArrayList<>();
        if (detallesDTO != null && !detallesDTO.isEmpty()) {
            for (DetalleCotizacionDTO detalleDTO : detallesDTO) {
                if (detalleDTO == null) continue;
                if (detalleDTO.getServicioId() == null) {
                    throw new CotizacionException("Un servicio extra no tiene id asociado.");
                }

                var servicio = servicioRepository.findById(detalleDTO.getServicioId())
                    .orElseThrow(() -> new CotizacionException(
                        "El servicio con ID " + detalleDTO.getServicioId() + " no existe en el catálogo."
                    ));

                int cantidad = detalleDTO.getCantidad() > 0 ? detalleDTO.getCantidad() : 1;
                double precioUnitario = detalleDTO.getPrecioUnitario();
                double subtotal = detalleDTO.getSubtotal();
                if (subtotal <= 0) {
                    subtotal = precioUnitario * cantidad;
                }

                DetalleCotizacion detalle = new DetalleCotizacion();
                detalle.setServicio(servicio);
                detalle.setCotizacion(cotizacion);
                detalle.setPrecioUnitario(precioUnitario);
                detalle.setCantidad(cantidad);
                detalle.setSubtotal(subtotal);
                detalle.setHoraSugerida(detalleDTO.getHoraSugerida());
                detalle.setEspecificacionesCliente(detalleDTO.getEspecificacionesCliente());
                detalle.setDesgloseOpciones(detalleDTO.getDesgloseOpciones());
                detalle.setUbicacionMontaje(detalleDTO.getUbicacionMontaje());

                detallesEntidad.add(detalle);
                total += subtotal;
            }
        }

        cotizacion.setServiciosExtra(detallesEntidad);
        cotizacion.setTotal(total);

        // ── 9. Persistencia ──────────────────────────────────────────────────
        Cotizacion guardada = cotizacionRepository.save(cotizacion);

        return CotizacionMapper.toDTO(guardada);
    }

    /**
     * Genera un folio único con formato COT-YYYY-NNNN.
     * Usa un contador de inicio basado en count() y verifica en bucle
     * hasta encontrar un folio que no colisione con registros existentes.
     */
    private String generarFolio() {
        int anio = Year.now().getValue();
        long base = cotizacionRepository.count() + 1;
        String folio;
        do {
            folio = String.format("COT-%d-%04d", anio, base);
            base++;
        } while (cotizacionRepository.findByFolio(folio).isPresent());
        return folio;
    }
}
