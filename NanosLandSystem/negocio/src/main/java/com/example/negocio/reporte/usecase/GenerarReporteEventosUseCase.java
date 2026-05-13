package com.example.negocio.reporte.usecase;

import com.mycompany.common.dtos.EventoDTO;
import com.mycompany.common.mapper.EventoMapper;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.persistencia.repository.EventoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class GenerarReporteEventosUseCase {

    @Autowired
    private EventoRepository eventoRepository;

    public List<EventoDTO> generarReporte(LocalDate inicio, LocalDate fin, TurnoEvento turno, EstadoCotizacion estado) {
        if (inicio != null && fin != null && inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin.");
        }

        List<Evento> eventos = eventoRepository.findReporteEventos(inicio, fin, turno, estado);
        return EventoMapper.toDTOList(eventos);
    }
}
