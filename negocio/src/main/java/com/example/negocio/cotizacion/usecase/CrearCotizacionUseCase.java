package com.example.negocio.cotizacion.usecase;

import com.example.negocio.evento.usecase.HorarioEvento;
import com.example.negocio.exception.CotizacionException;
import com.example.negocio.precio.CalcularPrecioPaqueteUseCase;
import com.example.negocio.precio.Dinero;
import com.example.negocio.precio.PrecioPaquete;
import com.mycompany.common.dtos.CotizacionDTO;
import com.mycompany.common.mapper.CotizacionMapper;
import com.mycompany.persistencia.dominio.DetalleCotizacion;
import com.mycompany.persistencia.dominio.Cliente;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.dominio.Servicio;
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
import java.util.List;
import java.util.Set;

/**
 * Caso de uso: Crear una nueva cotización.
 *
 * Responsabilidades:
 * - Validar todos los campos obligatorios del DTO.
 * - Verificar que el cliente y el paquete existan en la base de datos.
 * - Verificar que la fecha del evento sea futura (no en el pasado).
 * - Verificar que el turno no esté duplicado en la misma fecha.
 * - Guardar el horario real del evento (hora de inicio y de fin) que se capturó.
 * - Generar un folio único con formato COT-YYYY-NNNN.
 * - Calcular el precio del paquete (fijo o por comida y personas, con el descuento de la fecha)
 *   y guardarlo como precio pactado.
 * - Guardar la copia de lo que incluye el paquete (nombre, descripción y servicios incluidos).
 * - Total = precio del paquete + servicios extra.
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
    private final com.mycompany.persistencia.repository.UsuarioRepository usuarioRepository;
    private final CalcularPrecioPaqueteUseCase calcularPrecioPaqueteUseCase;

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
        HorarioEvento.validar(dto.getHoraInicio(), dto.getHoraFin());
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

        // ── 4. Verificación del paquete y su precio ──────────────────────────
        Paquete paquete = paqueteRepository.findById(dto.getPaqueteId())
            .orElseThrow(() -> new CotizacionException(
                "El paquete con ID " + dto.getPaqueteId() + " no existe en el catálogo."
            ));
        // Valida también que esté activo y que la comida y las personas correspondan a su forma de precio.
        // Lleva el descuento que corresponda a la fecha del evento.
        PrecioPaquete precio = calcularPrecioPaqueteUseCase.precioDeCatalogo(
            paquete, dto.getComidaId(), dto.getPersonas(), dto.getFecha());
        double precioPaquete = precio.precio();
        Servicio comida = dto.getComidaId() == null ? null : servicioRepository.findById(dto.getComidaId())
            .orElseThrow(() -> new CotizacionException("La comida seleccionada no existe en el catálogo."));

        // ── 5. Validación: no duplicar turno en la misma fecha ───────────────
        // Usamos EventoRepository en lugar de CotizacionRepository ya que la fecha vive en Evento.
        // Solo ocupan el turno los eventos confirmados; otros borradores no lo apartan.
        boolean turnoOcupado = eventoRepository.existsByFechaAndTurnoAndEstadoIn(
            dto.getFecha(),
            dto.getTurno(),
            com.example.negocio.agenda.usecase.VerificarDisponibilidadTurnoUseCase.ESTADOS_QUE_OCUPAN
        );
        if (turnoOcupado) {
            throw new CotizacionException("Ya existe un evento confirmado para esta fecha y turno. Seleccione otro turno.");
        }

        // ── 6. Construcción de la entidad ────────────────────────────────────
        Cotizacion cotizacion = CotizacionMapper.toEntity(dto);
        cotizacion.setCliente(cliente);
        cotizacion.setPaquete(paquete);
        cotizacion.setComida(comida);
        cotizacion.setPersonas(dto.getPersonas());
        precio.guardarEn(cotizacion);
        // Lo que incluye el paquete hoy queda guardado en la cotización, como su precio.
        PaqueteCotizado.guardarCopia(cotizacion);
        EstadoCotizacion estado = dto.getEstado() != null ? dto.getEstado() : EstadoCotizacion.BORRADOR;
        cotizacion.setEstado(estado);
        if (dto.getUsuarioId() != null) {
            // Usuario que elaboró la cotización (el que tiene la sesión abierta).
            usuarioRepository.findById(dto.getUsuarioId()).ifPresent(cotizacion::setUsuario);
        }

        Evento evento = new Evento();
        evento.setFecha(dto.getFecha());
        evento.setTurno(dto.getTurno());
        evento.setHoraInicio(dto.getHoraInicio());
        evento.setHoraFin(dto.getHoraFin());
        evento.setNombreFestejado(dto.getNombreFestejado());
        evento.setTematica(dto.getTematica());
        evento.setEstado(EstadoEvento.TENTATIVO);
        evento.setCotizacion(cotizacion);
        cotizacion.setEvento(evento);

        // ── 7. Generación de folio único COT-YYYY-NNNN ───────────────────────
        String folio = generarFolio();
        cotizacion.setFolio(folio);

        // ── 8. Total = precio del paquete + servicios extra ──────────────────
        List<DetalleCotizacion> detallesEntidad = LineasExtraCotizacion.construir(
            dto.getDetalles(), cotizacion, servicioRepository, Set.of());

        cotizacion.setServiciosExtra(detallesEntidad);
        cotizacion.setTotal(Dinero.aCentavos(precioPaquete + LineasExtraCotizacion.total(detallesEntidad)));

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
