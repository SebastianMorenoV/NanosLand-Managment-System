package com.example.negocio.evento.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

/**
 * Corrige la hora de inicio y de fin de un evento ya registrado, sin tocar su fecha ni su turno.
 * Sirve para los eventos que se confirmaron cuando el sistema les ponía un horario fijo por turno.
 */
@Service
@RequiredArgsConstructor
public class CambiarHorarioEventoUseCase {

    private final EventoRepository eventoRepository;

    @Transactional
    public void cambiarHorario(Long eventoId, LocalTime horaInicio, LocalTime horaFin) {
        if (eventoId == null) {
            throw new CotizacionException("Debe indicar el evento.");
        }
        HorarioEvento.validar(horaInicio, horaFin);

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new CotizacionException("No se encontró el evento indicado."));
        if (evento.getEstado() == EstadoEvento.CANCELADO || evento.getEstado() == EstadoEvento.CANCELADO_TARDIO) {
            throw new CotizacionException("No se puede cambiar el horario de un evento cancelado.");
        }

        evento.setHoraInicio(horaInicio);
        evento.setHoraFin(horaFin);
        eventoRepository.save(evento);
    }
}
