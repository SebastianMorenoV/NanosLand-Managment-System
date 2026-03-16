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
import java.time.LocalTime;
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
        List<Evento> eventos = eventoRepository.findByFecha(fecha);
        if (eventos.isEmpty()) {
            return true;
        }

        // Definir los límites teóricos del turno consultado
        LocalTime inicioTurno = (turno == TurnoEvento.MATUTINO) ? LocalTime.of(9, 0) : LocalTime.of(15, 0);
        LocalTime finTurno = (turno == TurnoEvento.MATUTINO) ? LocalTime.of(14, 0) : LocalTime.of(20, 0);

        for (Evento evento : eventos) {
            // Un evento se empalma si inicia antes de que termine el turno Y termina después de que inicie el turno
            if (inicioTurno.isBefore(evento.getHoraFin()) && finTurno.isAfter(evento.getHoraInicio())) {
                return false;
            }
        }

        return true;
    }
}
