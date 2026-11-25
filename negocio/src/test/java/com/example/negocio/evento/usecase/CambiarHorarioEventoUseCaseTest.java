package com.example.negocio.evento.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.persistencia.repository.EventoRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CambiarHorarioEventoUseCaseTest {

    @Mock EventoRepository eventoRepository;

    CambiarHorarioEventoUseCase cambiar;
    Evento evento;

    @BeforeEach
    void setUp() {
        cambiar = new CambiarHorarioEventoUseCase(eventoRepository);
        evento = new Evento();
        evento.setId(7L);
        evento.setFecha(LocalDate.now().plusDays(3));
        evento.setTurno(TurnoEvento.VESPERTINO);
        evento.setEstado(EstadoEvento.CONFIRMADO);
        // Horario que ponía el sistema antes, igual para todos los vespertinos.
        evento.setHoraInicio(LocalTime.of(15, 0));
        evento.setHoraFin(LocalTime.of(20, 0));
        lenient().when(eventoRepository.findById(7L)).thenReturn(Optional.of(evento));
    }

    @Test
    void corrigeLasHorasSinTocarFechaNiTurno() {
        LocalDate fecha = evento.getFecha();

        cambiar.cambiarHorario(7L, LocalTime.of(16, 30), LocalTime.of(21, 30));

        assertThat(evento.getHoraInicio()).isEqualTo(LocalTime.of(16, 30));
        assertThat(evento.getHoraFin()).isEqualTo(LocalTime.of(21, 30));
        assertThat(evento.getFecha()).isEqualTo(fecha);
        assertThat(evento.getTurno()).isEqualTo(TurnoEvento.VESPERTINO);
        verify(eventoRepository).save(evento);
    }

    @Test
    void tambienCorrigeEventosYaRealizados() {
        evento.setEstado(EstadoEvento.FINALIZADO);

        cambiar.cambiarHorario(7L, LocalTime.of(17, 0), LocalTime.MIDNIGHT);

        assertThat(evento.getHoraFin()).isEqualTo(LocalTime.MIDNIGHT);
    }

    @Test
    void noCambiaEventosCancelados() {
        evento.setEstado(EstadoEvento.CANCELADO);

        assertThatThrownBy(() -> cambiar.cambiarHorario(7L, LocalTime.of(16, 0), LocalTime.of(21, 0)))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("cancelado");
        verify(eventoRepository, never()).save(any());
    }

    @Test
    void laHoraDeFinDebeSerPosteriorALaDeInicio() {
        assertThatThrownBy(() -> cambiar.cambiarHorario(7L, LocalTime.of(18, 0), LocalTime.of(17, 0)))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("posterior");
        verify(eventoRepository, never()).save(any());
    }
}
