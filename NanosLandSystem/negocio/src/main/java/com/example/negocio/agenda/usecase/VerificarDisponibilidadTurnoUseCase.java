package com.example.negocio.agenda.usecase;

import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.persistencia.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VerificarDisponibilidadTurnoUseCase {

    private final EventoRepository eventoRepository;

    public String obtenerEstadoDisponibilidad(LocalDate fecha) {
        List<Evento> eventos = eventoRepository.findByFecha(fecha);
        if (eventos.isEmpty()) return "LIBRE";
        if (eventos.size() >= 2) return "GRIS";
        return "AMARILLO";
    }

    public boolean esTurnoDisponible(LocalDate fecha, TurnoEvento turno) {
        List<Evento> eventos = eventoRepository.findByFecha(fecha);
        return eventos.stream().noneMatch(e -> e.getTurno() == turno);
    }
}