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

    /**
     * Obtiene todos los eventos del sistema con sus relaciones precargadas.
     *
     * @return Lista de EventoDTO ordenada por fecha descendente.
     */
    @Transactional(readOnly = true)
    public List<EventoDTO> listarTodosEventos() {
        List<Evento> eventos = eventoRepository.findAllWithRelations();
        return EventoMapper.toDTOList(eventos);
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
