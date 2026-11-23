package com.example.negocio.evento.usecase;

import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.enums.TurnoEvento;

import java.time.LocalTime;

/** Horario por defecto de cada turno del salón. */
public final class HorarioTurno {

    private HorarioTurno() {
    }

    /** Asigna al evento el horario por defecto de su turno, reemplazando el que tuviera. */
    public static void asignar(Evento evento) {
        if (evento.getTurno() == TurnoEvento.MATUTINO) {
            evento.setHoraInicio(LocalTime.of(9, 0));
            evento.setHoraFin(LocalTime.of(14, 0));
        } else if (evento.getTurno() == TurnoEvento.VESPERTINO) {
            evento.setHoraInicio(LocalTime.of(15, 0));
            evento.setHoraFin(LocalTime.of(20, 0));
        }
    }
}
