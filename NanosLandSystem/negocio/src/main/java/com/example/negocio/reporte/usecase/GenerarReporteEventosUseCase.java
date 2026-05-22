package com.example.negocio.reporte.usecase;

import com.mycompany.common.dtos.EventoDTO;
import com.mycompany.common.mapper.EventoMapper;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.persistencia.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Caso de uso para generar el reporte de eventos (CU-07).
 * Usa constructor injection (@RequiredArgsConstructor) para facilitar
 * el testing unitario sin necesidad de un contenedor Spring.
 */
@Service
@RequiredArgsConstructor  // Fix #9: constructor injection en lugar de @Autowired field injection
public class GenerarReporteEventosUseCase {

    private final EventoRepository eventoRepository;  // Fix #9: final + constructor injection

    @Transactional(readOnly = true)  // Fix #11: evita dirty-checking innecesario en consultas
    public List<EventoDTO> generarReporte(LocalDate inicio, LocalDate fin, TurnoEvento turno, EstadoEvento estado) {
        if (inicio != null && fin != null && inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin.");
        }

        List<Evento> eventos;
        if (inicio == null && fin == null) {
            eventos = eventoRepository.findReporteEventosMasRecientes(
                turno, 
                estado, 
                org.springframework.data.domain.Pageable.unpaged()
            );
        } else {
            eventos = eventoRepository.findReporteEventos(inicio, fin, turno, estado);
        }
        return EventoMapper.toDTOList(eventos);
    }
}
