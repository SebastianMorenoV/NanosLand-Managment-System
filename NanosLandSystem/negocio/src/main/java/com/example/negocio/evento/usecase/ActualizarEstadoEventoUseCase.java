package com.example.negocio.evento.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.EventoDTO;
import com.mycompany.common.mapper.EventoMapper;
import com.mycompany.persistencia.dominio.Evento;
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
