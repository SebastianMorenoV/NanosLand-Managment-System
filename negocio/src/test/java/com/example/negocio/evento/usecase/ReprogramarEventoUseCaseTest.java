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
class ReprogramarEventoUseCaseTest {

    @Mock EventoRepository eventoRepository;

    ReprogramarEventoUseCase reprogramar;
    Evento evento;
    LocalDate nuevaFecha = LocalDate.now().plusDays(20);

    @BeforeEach
    void setUp() {
        reprogramar = new ReprogramarEventoUseCase(eventoRepository);
        evento = new Evento();
        evento.setId(9L);
        evento.setFecha(LocalDate.now().plusDays(5));
        evento.setTurno(TurnoEvento.MATUTINO);
        evento.setEstado(EstadoEvento.CONFIRMADO);
        evento.setHoraInicio(LocalTime.of(10, 0));
        evento.setHoraFin(LocalTime.of(14, 0));
        lenient().when(eventoRepository.findById(9L)).thenReturn(Optional.of(evento));
        lenient().when(eventoRepository.save(any(Evento.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void reprogramaUnEventoConfirmadoConSuNuevoHorario() {
        reprogramar.reprogramar(9L, nuevaFecha, TurnoEvento.VESPERTINO, LocalTime.of(17, 0), LocalTime.of(22, 0));

        assertThat(evento.getFecha()).isEqualTo(nuevaFecha);
        assertThat(evento.getTurno()).isEqualTo(TurnoEvento.VESPERTINO);
        assertThat(evento.getHoraInicio()).isEqualTo(LocalTime.of(17, 0));
        assertThat(evento.getHoraFin()).isEqualTo(LocalTime.of(22, 0));
    }

    @Test
    void unEventoEnCursoNoSeReprograma() {
        evento.setFecha(LocalDate.now());
        evento.setEstado(EstadoEvento.EN_CURSO);

        assertThatThrownBy(() -> reprogramar.reprogramar(9L, nuevaFecha, TurnoEvento.MATUTINO,
                LocalTime.of(10, 0), LocalTime.of(14, 0)))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("en curso");
        verify(eventoRepository, never()).save(any());
    }

    @Test
    void unEventoRealizadoNoSeReprograma() {
        evento.setFecha(LocalDate.now().minusDays(2));
        evento.setEstado(EstadoEvento.FINALIZADO);

        assertThatThrownBy(() -> reprogramar.reprogramar(9L, nuevaFecha, TurnoEvento.MATUTINO,
                LocalTime.of(10, 0), LocalTime.of(14, 0)))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("ya se realizó");
        verify(eventoRepository, never()).save(any());
    }
}
