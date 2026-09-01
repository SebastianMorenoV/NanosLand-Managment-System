package com.example.negocio.logistica.usecase;

import com.mycompany.common.dtos.EventoDTO;
import com.mycompany.common.dtos.LogisticaDTO;
import com.mycompany.common.mapper.EventoMapper;
import com.mycompany.common.mapper.LogisticaMapper;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.repository.EventoRepository;
import com.mycompany.persistencia.repository.LogisticaServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Caso de uso para consultar la logística de eventos (CU-06).
 *
 * Responsabilidades:
 * - Obtener eventos próximos (confirmados) para la bitácora.
 * - Obtener los servicios de logística asociados a un evento.
 */
@Service
@RequiredArgsConstructor
public class ConsultarLogisticaUseCase {

    private final EventoRepository eventoRepository;
    private final LogisticaServicioRepository logisticaRepo;

    /**
     * Devuelve los eventos confirmados cuya fecha es hoy o en el futuro.
     */
    @Transactional(readOnly = true)
    public List<EventoDTO> obtenerEventosProximos() {
        LocalDate hoy = LocalDate.now();
        // Traemos todos los eventos entre hoy y 90 días adelante
        LocalDate limite = hoy.plusDays(90);
        List<Evento> eventos = eventoRepository.findByFechaBetweenWithRelations(hoy, limite);

        return eventos.stream()
                .filter(e -> e.getEstado() != EstadoEvento.CANCELADO)
                .sorted((a, b) -> {
                    if (a.getFecha() == null) return 1;
                    if (b.getFecha() == null) return -1;
                    return a.getFecha().compareTo(b.getFecha());
                })
                .map(EventoMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Devuelve los registros de logística para un evento específico.
     */
    @Transactional(readOnly = true)
    public List<LogisticaDTO> obtenerServiciosPorEvento(Long eventoId) {
        return LogisticaMapper.toDTOList(logisticaRepo.findByEventoIdWithServicio(eventoId));
    }
}
