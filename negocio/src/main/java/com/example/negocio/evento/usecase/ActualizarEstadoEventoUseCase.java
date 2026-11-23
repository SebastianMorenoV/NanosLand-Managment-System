package com.example.negocio.evento.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.EventoDTO;
import com.mycompany.common.mapper.EventoMapper;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso para actualizar el estado y detalles operativos de un evento.
 */
@Service
@RequiredArgsConstructor
public class ActualizarEstadoEventoUseCase {

    private final EventoRepository eventoRepository;

    /**
     * Actualiza el estado de un evento existente.
     *
     * @param eventoId ID del evento.
     * @param nuevoEstado Nuevo estado a asignar.
     * @return EventoDTO actualizado.
     */
    @Transactional
    public EventoDTO actualizarEstado(Long eventoId, EstadoEvento nuevoEstado) {
        if (eventoId == null) {
            throw new CotizacionException("El ID del evento no puede ser nulo.");
        }
        if (nuevoEstado == null) {
            throw new CotizacionException("Debe especificar un estado válido para el evento.");
        }

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new CotizacionException("No se encontró el evento con ID " + eventoId));

        if (nuevoEstado == EstadoEvento.CANCELADO) {
            if (evento.getEstado() == EstadoEvento.CANCELADO
                    || evento.getEstado() == EstadoEvento.CANCELADO_TARDIO) {
                throw new CotizacionException("El evento ya está cancelado.");
            }
            if (evento.getEstado() == EstadoEvento.FINALIZADO) {
                throw new CotizacionException("Un evento finalizado no se puede cancelar.");
            }

            // La cancelación tardía solo aplica a eventos ya confirmados: un borrador
            // sin anticipo se cancela sin más, sea cual sea la fecha.
            boolean confirmado = evento.getEstado() == EstadoEvento.CONFIRMADO
                || evento.getEstado() == EstadoEvento.EN_CURSO;
            java.time.LocalDate hoy = java.time.LocalDate.now();
            java.time.LocalDate fechaEvento = evento.getFecha();
            if (confirmado && fechaEvento != null
                    && (hoy.isEqual(fechaEvento) || hoy.isEqual(fechaEvento.minusDays(1)))) {
                nuevoEstado = EstadoEvento.CANCELADO_TARDIO;
            }

            // Cancelar el evento cancela también su cotización, en la misma
            // transacción: deja de admitir cambios y pagos.
            Cotizacion cotizacion = evento.getCotizacion();
            if (cotizacion != null && cotizacion.getEstado() != EstadoCotizacion.ELIMINADA) {
                cotizacion.setEstado(EstadoCotizacion.CANCELADA);
            }
        }

        if (nuevoEstado == EstadoEvento.FINALIZADO) {
            if (evento.getEstado() != EstadoEvento.CONFIRMADO && evento.getEstado() != EstadoEvento.EN_CURSO) {
                throw new CotizacionException("Solo se puede finalizar un evento confirmado o en curso.");
            }
            if (evento.getFecha() != null && evento.getFecha().isAfter(java.time.LocalDate.now())) {
                throw new CotizacionException("No se puede finalizar un evento antes de su fecha.");
            }
        } else if (nuevoEstado != EstadoEvento.CANCELADO && nuevoEstado != EstadoEvento.CANCELADO_TARDIO) {
            // Confirmar ocurre al registrar el anticipo y "en curso" es automático por fecha.
            throw new CotizacionException("Ese cambio de estado no se puede hacer manualmente.");
        }

        evento.setEstado(nuevoEstado);
        Evento guardado = eventoRepository.save(evento);
        return EventoMapper.toDTO(guardado);
    }

    /**
     * Actualiza notas, temática o nombre de festejado de un evento.
     *
     * @param eventoId ID del evento.
     * @param nombreFestejado Nombre del festejado.
     * @param tematica Temática del evento.
     * @param notas Notas adicionales.
     * @return EventoDTO actualizado.
     */
    @Transactional
    public EventoDTO actualizarDetalles(Long eventoId, String nombreFestejado, String tematica, String notas) {
        if (eventoId == null) {
            throw new CotizacionException("El ID del evento no puede ser nulo.");
        }

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new CotizacionException("No se encontró el evento con ID " + eventoId));

        if (nombreFestejado != null) evento.setNombreFestejado(nombreFestejado.trim());
        if (tematica != null) evento.setTematica(tematica.trim());
        if (notas != null) evento.setNotas(notas.trim());

        Evento guardado = eventoRepository.save(evento);
        return EventoMapper.toDTO(guardado);
    }
}
