/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.negocio.agenda.usecase;

import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.persistencia.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author skyro
 */

@Service
@RequiredArgsConstructor
public class VerificarDisponibilidadTurnoUseCase {

    private final EventoRepository eventoRepository;

    public boolean verificar(LocalDate fecha, TurnoEvento turno) {
        LocalDateTime fechaHora = fecha.atStartOfDay();
        List<Evento> eventos = eventoRepository.findByFechaHoraInicioAndTurno(fechaHora, turno);
        return eventos.isEmpty();
    }
}
