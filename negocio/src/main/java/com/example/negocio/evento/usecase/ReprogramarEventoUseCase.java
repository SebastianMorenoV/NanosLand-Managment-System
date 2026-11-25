package com.example.negocio.evento.usecase;

import com.example.negocio.exception.CotizacionException;
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
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class ReprogramarEventoUseCase {

    /** También lo usa la edición de la cotización, que es la otra forma de cambiar la fecha. */
    public static final String MENSAJE_EN_CURSO =
        "El evento ya está en curso (es hoy) y no se puede cambiar de fecha ni de turno. "
        + "Si no se va a realizar, cancélelo.";

    private final EventoRepository eventoRepository;

    @Transactional
    public EventoDTO reprogramar(Long eventoId, LocalDate nuevaFecha, TurnoEvento nuevoTurno,
                                 LocalTime horaInicio, LocalTime horaFin) {
        if (eventoId == null) {
            throw new CotizacionException("El ID del evento no puede ser nulo.");
        }
        if (nuevaFecha == null || nuevoTurno == null) {
            throw new CotizacionException("Debe especificar una nueva fecha y turno válidos.");
        }
        HorarioEvento.validar(horaInicio, horaFin);
        if (nuevaFecha.isBefore(LocalDate.now())) {
            throw new CotizacionException("No se puede reprogramar un evento a una fecha pasada.");
        }

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new CotizacionException("No se encontró el evento con ID " + eventoId));

        if (evento.getEstado() == EstadoEvento.CANCELADO || evento.getEstado() == EstadoEvento.CANCELADO_TARDIO) {
            throw new CotizacionException("No se puede reprogramar un evento cancelado.");
        }
        // El día del evento ya no se mueve: si no se va a realizar, se cancela (cancelación tardía).
        if (evento.getEstado() == EstadoEvento.EN_CURSO) {
            throw new CotizacionException(MENSAJE_EN_CURSO);
        }
        if (evento.getEstado() == EstadoEvento.FINALIZADO) {
            throw new CotizacionException("No se puede reprogramar un evento que ya se realizó.");
        }

        // Verificar disponibilidad
        // Solo lo ocupa OTRO evento confirmado; los borradores no apartan el turno.
        boolean ocupado = eventoRepository.existsByFechaAndTurnoAndEstadoInAndIdNot(
            nuevaFecha, nuevoTurno,
            com.example.negocio.agenda.usecase.VerificarDisponibilidadTurnoUseCase.ESTADOS_QUE_OCUPAN,
            evento.getId());
        if (ocupado) {
            throw new CotizacionException("El turno " + nuevoTurno.name() + " para la fecha " + nuevaFecha + " ya se encuentra ocupado.");
        }

        evento.setFecha(nuevaFecha);
        evento.setTurno(nuevoTurno);
        evento.setHoraInicio(horaInicio);
        evento.setHoraFin(horaFin);

        Evento guardado = eventoRepository.save(evento);
        return EventoMapper.toDTO(guardado);
    }
}
