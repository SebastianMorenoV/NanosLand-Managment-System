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
     * Estados de evento que ocupan su fecha y turno. Un borrador sin anticipo
     * (TENTATIVO) NO aparta la fecha: puede haber varios para el mismo turno y
     * se lo queda el primero que paga el anticipo. Los cancelados la liberan.
     */
    public static final java.util.List<EstadoEvento> ESTADOS_QUE_OCUPAN = java.util.List.of(
        EstadoEvento.CONFIRMADO, EstadoEvento.EN_CURSO, EstadoEvento.FINALIZADO);

    /**
     * Verifica si un turno específico está disponible para una fecha dada.
     * Un turno está ocupado si existe un Evento confirmado (o ya realizado) en él.
     */
    @Transactional(readOnly = true)
    public boolean esTurnoDisponible(LocalDate fecha, TurnoEvento turno) {
        return !eventoRepository.existsByFechaAndTurnoAndEstadoIn(fecha, turno, ESTADOS_QUE_OCUPAN);
    }

    /**
     * Indica si la fecha y turno están ocupados por un evento distinto al indicado.
     * Sirve para validar el propio evento al confirmarlo, editarlo o reprogramarlo.
     */
    @Transactional(readOnly = true)
    public boolean estaOcupadoPorOtroEvento(LocalDate fecha, TurnoEvento turno, Long eventoId) {
        if (eventoId == null) {
            return !esTurnoDisponible(fecha, turno);
        }
        return eventoRepository.existsByFechaAndTurnoAndEstadoInAndIdNot(fecha, turno, ESTADOS_QUE_OCUPAN, eventoId);
    }

    /**
     * Retorna el estado visual del día para el calendario:
     * - "LIBRE"    → ambos turnos disponibles
     * - "AMARILLO" → un turno ocupado, uno libre
     * - "GRIS"     → todos los turnos ocupados (día bloqueado)
     */
    /**
     * Estado visual ("LIBRE", "AMARILLO" o "GRIS") de cada día de un periodo, con UNA
     * sola consulta. Para pintar un calendario se usa este método en lugar de llamar a
     * {@link #obtenerEstadoDisponibilidad(LocalDate)} día por día (dos consultas por día).
     * Los días sin eventos confirmados no aparecen en el mapa: están libres.
     */
    @Transactional(readOnly = true)
    public java.util.Map<LocalDate, String> obtenerEstadosDisponibilidad(LocalDate inicio, LocalDate fin) {
        java.util.Map<LocalDate, java.util.Set<TurnoEvento>> ocupados = new java.util.HashMap<>();
        for (Object[] fila : eventoRepository.findFechaYTurnoOcupados(inicio, fin, ESTADOS_QUE_OCUPAN)) {
            ocupados.computeIfAbsent((LocalDate) fila[0], f -> java.util.EnumSet.noneOf(TurnoEvento.class))
                    .add((TurnoEvento) fila[1]);
        }
        java.util.Map<LocalDate, String> estados = new java.util.HashMap<>();
        ocupados.forEach((fecha, turnos) ->
            estados.put(fecha, turnos.size() >= TurnoEvento.values().length ? "GRIS" : "AMARILLO"));
        return estados;
    }

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