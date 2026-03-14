/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.negocio.agenda.usecase;

import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.persistencia.repository.EventoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 *
 * @author skyro
 */
@Service
@RequiredArgsConstructor
public class ConsultarAgendaUseCase {

    private final EventoRepository eventoRepository;

    public List<Evento> obtenerMesCompleto(int anio, int mes) {
        LocalDateTime inicio = YearMonth.of(anio, mes).atDay(1).atStartOfDay();
        LocalDateTime fin = YearMonth.of(anio, mes).atEndOfMonth().atTime(23, 59, 59);
        return eventoRepository.findByFechaHoraInicioBetween(inicio, fin);
    }

    public boolean verificarTurnoDisponible(LocalDate fecha, TurnoEvento turno) {
        LocalDateTime fechaHora = fecha.atStartOfDay();
        List<Evento> eventos = eventoRepository.findByFechaHoraInicioAndTurno(fechaHora, turno);
        return eventos.isEmpty();
    }
}
