package com.example.negocio.agenda.usecase;

import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.persistencia.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Verifica la disponibilidad de fechas y turnos para nuevos eventos.
 *
 * FIX #6 — Fuente de verdad unificada:
 * Se cambió de EventoRepository a CotizacionRepository para que la lógica
 * de disponibilidad coincida con la validación de CrearCotizacionUseCase.
 * Los Eventos solo se crean tras acumular $3,000 en anticipos, por lo que
 * una Cotizacion VIGENTE sin Evento asociado aparecía erróneamente como
 * "disponible" en el calendario, causando un conflicto confuso al intentar
 * crear una nueva cotización para esa misma fecha/turno.
 */
@Service
@RequiredArgsConstructor
public class VerificarDisponibilidadTurnoUseCase {

    // Modificado: Ahora EventoRepository es la única fuente de verdad.
    // Los eventos siempre se crean (incluso en BORRADOR como TENTATIVO).
    private final EventoRepository eventoRepository;

    /**
     * Verifica si un turno específico está disponible para una fecha dada.
     * Un turno está ocupado si existe un Evento que NO esté en estado CANCELADO.
     */
    @Transactional(readOnly = true)
    public boolean esTurnoDisponible(LocalDate fecha, TurnoEvento turno) {
        return !eventoRepository.existsByFechaAndTurnoAndEstadoNot(
            fecha, turno, EstadoEvento.CANCELADO
        );
    }

    /**
     * Retorna el estado visual del día para el calendario:
     * - "LIBRE"    → ambos turnos disponibles
     * - "AMARILLO" → un turno ocupado, uno libre
     * - "GRIS"     → todos los turnos ocupados (día bloqueado)
     */
    @Transactional(readOnly = true)  // Fix #11: solo lectura
    public String obtenerEstadoDisponibilidad(LocalDate fecha) {
        long turnosOcupados = Arrays.stream(TurnoEvento.values())
            .filter(t -> !esTurnoDisponible(fecha, t))
            .count();

        if (turnosOcupados == 0) return "LIBRE";
        if (turnosOcupados >= TurnoEvento.values().length) return "GRIS";
        return "AMARILLO";
    }
}