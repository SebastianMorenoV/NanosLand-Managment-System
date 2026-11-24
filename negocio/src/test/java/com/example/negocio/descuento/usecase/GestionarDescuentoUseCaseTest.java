package com.example.negocio.descuento.usecase;

import static com.example.negocio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.DescuentoDTO;
import com.mycompany.persistencia.dominio.Descuento;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.enums.TipoDescuento;
import com.mycompany.persistencia.repository.DescuentoRepository;
import com.mycompany.persistencia.repository.PaqueteRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GestionarDescuentoUseCaseTest {

    @Mock DescuentoRepository descuentoRepository;
    @Mock PaqueteRepository paqueteRepository;

    GestionarDescuentoUseCase gestionar;

    Paquete plus = paqueteConTabla(3, "Paquete Plus");
    Paquete full = paqueteConTabla(2, "Paquete Full");

    @BeforeEach
    void setUp() {
        gestionar = new GestionarDescuentoUseCase(descuentoRepository, paqueteRepository);
        lenient().when(paqueteRepository.findById(2L)).thenReturn(Optional.of(full));
        lenient().when(paqueteRepository.findById(3L)).thenReturn(Optional.of(plus));
        lenient().when(descuentoRepository.save(any(Descuento.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static DescuentoDTO entreSemana() {
        DescuentoDTO dto = new DescuentoDTO();
        dto.setNombre("  Entre semana ");
        dto.setTipo(TipoDescuento.PORCENTAJE);
        dto.setValor(15);
        dto.setDias(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY));
        return dto;
    }

    private Descuento guardado() {
        ArgumentCaptor<Descuento> captor = ArgumentCaptor.forClass(Descuento.class);
        verify(descuentoRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void creaElDescuentoConSusPaquetes() {
        DescuentoDTO dto = entreSemana();
        dto.setTodosLosPaquetes(false);
        dto.setPaqueteIds(List.of(3L, 2L));

        DescuentoDTO creado = gestionar.crear(dto);

        Descuento d = guardado();
        assertThat(d.getNombre()).isEqualTo("Entre semana");
        assertThat(d.getDias()).containsExactlyInAnyOrder(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY);
        assertThat(d.getPaquetes()).containsExactlyInAnyOrder(plus, full);
        assertThat(creado.getPaqueteNombres()).containsExactly("Paquete Full", "Paquete Plus");
        assertThat(creado.getEstado()).isEqualTo(DescuentoDTO.Estado.ACTIVO);
    }

    @Test
    void losSieteDiasSeGuardanComoSinRestriccion() {
        DescuentoDTO dto = entreSemana();
        dto.setDias(EnumSet.allOf(DayOfWeek.class));

        gestionar.crear(dto);

        assertThat(guardado().getDias()).isEmpty();
    }

    @Test
    void validaElValorSegunElTipo() {
        DescuentoDTO dto = entreSemana();
        dto.setValor(120);
        assertThatThrownBy(() -> gestionar.crear(dto))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("100");

        dto.setValor(0);
        assertThatThrownBy(() -> gestionar.crear(dto))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("mayor que 0");

        dto.setTipo(TipoDescuento.MONTO_FIJO);
        dto.setValor(-5);
        assertThatThrownBy(() -> gestionar.crear(dto))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("mayor que $0");
        verify(descuentoRepository, never()).save(any());
    }

    @Test
    void rechazaFechasQueNoTienenSentido() {
        DescuentoDTO dto = entreSemana();
        dto.setFechaInicio(LocalDate.now().plusDays(10));
        dto.setFechaFin(LocalDate.now().plusDays(5));
        assertThatThrownBy(() -> gestionar.crear(dto))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("termina antes de empezar");

        dto.setFechaInicio(null);
        dto.setFechaFin(LocalDate.now().minusDays(1));
        assertThatThrownBy(() -> gestionar.crear(dto))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("ya terminó");

        dto.setFechaFin(null);
        dto.setCotizarHasta(LocalDate.now().minusDays(1));
        assertThatThrownBy(() -> gestionar.crear(dto))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("ya pasó");
    }

    @Test
    void pidePaquetesSiNoEsParaTodos() {
        DescuentoDTO dto = entreSemana();
        dto.setTodosLosPaquetes(false);

        assertThatThrownBy(() -> gestionar.crear(dto))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("al menos un paquete");
    }

    @Test
    void noRepiteNombres() {
        when(descuentoRepository.existsByNombreIgnoreCase("Entre semana")).thenReturn(true);

        assertThatThrownBy(() -> gestionar.crear(entreSemana()))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("Ya existe");
    }

    @Test
    void duplicarPasaLasFechasAlSiguienteAnio() {
        Descuento diciembre = descuento(7, "Diciembre 2026", TipoDescuento.MONTO_FIJO, 1000);
        diciembre.setFechaInicio(LocalDate.of(2026, 12, 1));
        diciembre.setFechaFin(LocalDate.of(2026, 12, 31));
        diciembre.setActivo(false);
        when(descuentoRepository.findById(7L)).thenReturn(Optional.of(diciembre));

        DescuentoDTO copia = gestionar.duplicarAlSiguienteAnio(7L);

        assertThat(copia.getNombre()).isEqualTo("Diciembre 2027");
        assertThat(copia.getFechaInicio()).isEqualTo(LocalDate.of(2027, 12, 1));
        assertThat(copia.getFechaFin()).isEqualTo(LocalDate.of(2027, 12, 31));
        assertThat(copia.isActivo()).isTrue();
        assertThat(copia.getValor()).isEqualTo(1000);
    }

    @Test
    void sinAnioEnElNombreLoAgrega() {
        Descuento d = descuento(8, "Verano", TipoDescuento.PORCENTAJE, 10);
        d.setFechaInicio(LocalDate.of(2027, 7, 1));
        assertThat(GestionarDescuentoUseCase.nombreDelSiguienteAnio(d)).isEqualTo("Verano 2028");
    }

    @Test
    void noDuplicaUnDescuentoSinFechas() {
        when(descuentoRepository.findById(9L)).thenReturn(Optional.of(
                descuento(9, "Entre semana", TipoDescuento.PORCENTAJE, 15, DayOfWeek.MONDAY)));

        assertThatThrownBy(() -> gestionar.duplicarAlSiguienteAnio(9L))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("no tiene fechas");
    }

    @Test
    void elEstadoDistingueVencidosYDesactivados() {
        Descuento activo = descuento(1, "A", TipoDescuento.PORCENTAJE, 10);
        Descuento vencido = descuento(2, "B", TipoDescuento.PORCENTAJE, 10);
        vencido.setFechaFin(LocalDate.now().minusDays(1));
        Descuento apagado = descuento(3, "C", TipoDescuento.PORCENTAJE, 10);
        apagado.setActivo(false);
        when(descuentoRepository.findAll()).thenReturn(List.of(apagado, vencido, activo));

        assertThat(gestionar.obtenerTodos()).extracting(DescuentoDTO::getEstado).containsExactly(
                DescuentoDTO.Estado.ACTIVO, DescuentoDTO.Estado.VENCIDO, DescuentoDTO.Estado.DESACTIVADO);
    }
}
