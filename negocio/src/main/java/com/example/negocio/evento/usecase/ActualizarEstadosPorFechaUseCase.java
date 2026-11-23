package com.example.negocio.evento.usecase;

import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Avanza el estado de los eventos confirmados según la fecha:
 * el día del evento pasan a EN_CURSO y al día siguiente a FINALIZADO.
 *
 * No hay un proceso en segundo plano: se ejecuta al abrir la aplicación y
 * antes de las consultas que dependen del estado (eventos, logística,
 * oportunidades de recompra). Los eventos TENTATIVO (sin anticipo) y los
 * cancelados no se tocan.
 */
@Service
@RequiredArgsConstructor
public class ActualizarEstadosPorFechaUseCase {

    private final EventoRepository eventoRepository;

    /**
     * Usa su propia transacción para poder llamarse desde consultas de solo lectura.
     *
     * @return cantidad de eventos que cambiaron de estado.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int actualizar() {
        LocalDate hoy = LocalDate.now();
        int finalizados = eventoRepository.cambiarEstadoDeEventosAnterioresA(
            hoy, List.of(EstadoEvento.CONFIRMADO, EstadoEvento.EN_CURSO), EstadoEvento.FINALIZADO);
        int enCurso = eventoRepository.cambiarEstadoDeEventosDelDia(
            hoy, EstadoEvento.CONFIRMADO, EstadoEvento.EN_CURSO);
        return finalizados + enCurso;
    }
}
