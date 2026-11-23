package com.example.negocio.evento.usecase;

import com.mycompany.common.dtos.EventoDTO;
import com.mycompany.common.mapper.EventoMapper;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Caso de uso para consultar y visualizar el catálogo completo de eventos del sistema.
 */
@Service
@RequiredArgsConstructor
public class ConsultarEventosUseCase {

    private final EventoRepository eventoRepository;
    private final com.mycompany.persistencia.repository.PagoRepository pagoRepository;
    private final com.example.negocio.evento.usecase.ActualizarEstadosPorFechaUseCase actualizarEstadosPorFechaUseCase;

    /**
     * Obtiene todos los eventos del sistema con sus relaciones precargadas.
     *
     * @return Lista de EventoDTO ordenada por fecha descendente.
     */
    @Transactional(readOnly = true)
    public List<EventoDTO> listarTodosEventos() {
        // Pone al día los estados (en curso / finalizado) según la fecha de hoy.
        actualizarEstadosPorFechaUseCase.actualizar();
        List<Evento> eventos = eventoRepository.findAllWithRelations();
        return EventoMapper.toDTOList(eventos);
    }

    /**
     * Eventos ya vendidos: confirmados, en curso, finalizados y los cancelados que
     * llegaron a recibir dinero. Los borradores, y los borradores cancelados sin
     * pago, nunca fueron un evento: se consultan en Cotizaciones.
     *
     * @return Lista de EventoDTO ordenada por fecha descendente.
     */
    @Transactional(readOnly = true)
    public List<EventoDTO> listarEventosConfirmados() {
        actualizarEstadosPorFechaUseCase.actualizar();
        java.util.Set<Long> conPagos = new java.util.HashSet<>(pagoRepository.findCotizacionIdsConPagos());
        return EventoMapper.toDTOList(eventoRepository.findAllWithRelations().stream()
            .filter(e -> esEventoVendido(e, conPagos))
            .collect(java.util.stream.Collectors.toList()));
    }

    private static boolean esEventoVendido(Evento evento, java.util.Set<Long> cotizacionesConPagos) {
        switch (evento.getEstado()) {
            case CONFIRMADO:
            case EN_CURSO:
            case FINALIZADO:
                return true;
            case CANCELADO:
            case CANCELADO_TARDIO:
                return evento.getCotizacion() != null
                    && cotizacionesConPagos.contains(evento.getCotizacion().getId());
            default:
                return false;
        }
    }

    /**
     * Obtiene el detalle de un evento por su ID.
     *
     * @param id ID del evento.
     * @return EventoDTO correspondiente o null si no existe.
     */
    @Transactional(readOnly = true)
    public EventoDTO obtenerPorId(Long id) {
        if (id == null) return null;
        Optional<Evento> eventoOpt = eventoRepository.findById(id);
        return eventoOpt.map(EventoMapper::toDTO).orElse(null);
    }
}
