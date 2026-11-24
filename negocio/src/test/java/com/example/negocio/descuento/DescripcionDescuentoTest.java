package com.example.negocio.descuento;

import static java.time.DayOfWeek.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.common.dtos.DescuentoDTO;
import com.mycompany.common.util.DescripcionDescuento;
import com.mycompany.persistencia.enums.TipoDescuento;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Textos con los que se muestra un descuento (clase de Common). */
class DescripcionDescuentoTest {

    @Test
    void valor() {
        assertThat(DescripcionDescuento.valor(TipoDescuento.PORCENTAJE, 15)).isEqualTo("15%");
        assertThat(DescripcionDescuento.valor(TipoDescuento.PORCENTAJE, 12.5)).isEqualTo("12.5%");
        assertThat(DescripcionDescuento.valor(TipoDescuento.MONTO_FIJO, 1000))
                .isEqualTo(String.format("$%,.2f", 1000.0));
    }

    @Test
    void dias() {
        assertThat(DescripcionDescuento.dias(EnumSet.noneOf(DayOfWeek.class))).isEqualTo("Todos los días");
        assertThat(DescripcionDescuento.dias(EnumSet.allOf(DayOfWeek.class))).isEqualTo("Todos los días");
        assertThat(DescripcionDescuento.dias(EnumSet.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY))).isEqualTo("Lunes a jueves");
        assertThat(DescripcionDescuento.dias(EnumSet.of(FRIDAY, SATURDAY, SUNDAY))).isEqualTo("Viernes a domingo");
        assertThat(DescripcionDescuento.dias(EnumSet.of(SATURDAY, SUNDAY))).isEqualTo("Sábado y domingo");
        assertThat(DescripcionDescuento.dias(EnumSet.of(MONDAY, WEDNESDAY, FRIDAY))).isEqualTo("Lunes, miércoles y viernes");
    }

    @Test
    void temporada() {
        LocalDate inicio = LocalDate.of(2026, 12, 1);
        LocalDate fin = LocalDate.of(2026, 12, 31);
        assertThat(DescripcionDescuento.temporada(null, null)).isEqualTo("Cualquier fecha");
        assertThat(DescripcionDescuento.temporada(inicio, fin)).isEqualTo("Del 01/12/2026 al 31/12/2026");
        assertThat(DescripcionDescuento.temporada(inicio, null)).isEqualTo("Desde el 01/12/2026");
        assertThat(DescripcionDescuento.temporada(null, fin)).isEqualTo("Hasta el 31/12/2026");
        assertThat(DescripcionDescuento.temporada(fin, fin)).isEqualTo("Solo el 31/12/2026");
    }

    @Test
    void resumen() {
        DescuentoDTO d = new DescuentoDTO();
        d.setTipo(TipoDescuento.PORCENTAJE);
        d.setValor(15);
        d.setDias(EnumSet.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY));
        assertThat(DescripcionDescuento.resumen(d))
                .isEqualTo("15% menos en todos los paquetes, para eventos de lunes a jueves.");

        d.setTodosLosPaquetes(false);
        d.setPaqueteNombres(List.of("Paquete Full", "Paquete Neón", "Paquete Plus"));
        d.setDias(EnumSet.of(SATURDAY, SUNDAY));
        d.setFechaInicio(LocalDate.of(2026, 12, 1));
        d.setFechaFin(LocalDate.of(2026, 12, 31));
        d.setCotizarHasta(LocalDate.of(2026, 12, 15));
        assertThat(DescripcionDescuento.resumen(d)).isEqualTo(
                "15% menos en Paquete Full, Paquete Neón y Paquete Plus, para eventos en sábado y domingo"
                        + " del 01/12/2026 al 31/12/2026. Se puede cotizar hasta el 15/12/2026.");
    }
}
