package com.example.negocio.precio;

import static com.example.negocio.Fixtures.*;
import static java.time.DayOfWeek.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Descuento;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.TipoDescuento;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.DescuentoRepository;
import com.mycompany.persistencia.repository.PaqueteRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Descuentos por día de la semana y temporada aplicados al precio del paquete. */
@ExtendWith(MockitoExtension.class)
class DescuentoPaqueteTest {

    @Mock PaqueteRepository paqueteRepository;
    @Mock CotizacionRepository cotizacionRepository;
    @Mock DescuentoRepository descuentoRepository;

    CalcularPrecioPaqueteUseCase precios;

    Servicio barbacoa = comida(10, "Barbacoa");
    Paquete renta = paqueteFijo(1, "Renta de local", 9800);
    Paquete full = paqueteConTabla(2, "Paquete Full");

    /** 15 % de lunes a jueves en todos los paquetes. */
    Descuento entreSemana = descuento(1, "Entre semana", TipoDescuento.PORCENTAJE, 15, MONDAY, TUESDAY, WEDNESDAY, THURSDAY);

    LocalDate lunes = proximo(MONDAY);
    LocalDate martes = proximo(TUESDAY);
    LocalDate sabado = proximo(SATURDAY);

    @BeforeEach
    void setUp() {
        precios = new CalcularPrecioPaqueteUseCase(paqueteRepository, cotizacionRepository, descuentoRepository);
        precio(full, barbacoa, 100, 23800);
        lenient().when(paqueteRepository.findById(1L)).thenReturn(Optional.of(renta));
        lenient().when(paqueteRepository.findById(2L)).thenReturn(Optional.of(full));
    }

    private void descuentosVigentes(Descuento... descuentos) {
        when(descuentoRepository.findByActivoTrue()).thenReturn(List.of(descuentos));
    }

    private PrecioPaquete cotizarFull(LocalDate fecha) {
        return precios.calcular(new SolicitudPrecioPaquete(2L, 10L, 100, fecha));
    }

    @Test
    void porcentajeEnUnDiaDeLaSemana() {
        descuentosVigentes(entreSemana);

        PrecioPaquete p = cotizarFull(lunes);

        assertThat(p.precioLista()).isEqualTo(23800);
        assertThat(p.montoDescuento()).isEqualTo(3570);
        assertThat(p.precio()).isEqualTo(20230);
        assertThat(p.descuento()).isEqualTo("Entre semana");
        assertThat(p.tieneDescuento()).isTrue();
    }

    @Test
    void enOtroDiaNoHayDescuento() {
        descuentosVigentes(entreSemana);

        PrecioPaquete p = cotizarFull(sabado);

        assertThat(p.precio()).isEqualTo(23800);
        assertThat(p.tieneDescuento()).isFalse();
        assertThat(p.descuento()).isNull();
    }

    @Test
    void sinFechaNoHayDescuento() {
        PrecioPaquete p = cotizarFull(null);

        assertThat(p.precio()).isEqualTo(23800);
        assertThat(p.tieneDescuento()).isFalse();
    }

    @Test
    void montoFijoNuncaDejaElPaqueteEnNegativo() {
        descuentosVigentes(descuento(2, "Regalo", TipoDescuento.MONTO_FIJO, 50000));

        PrecioPaquete p = precios.calcular(new SolicitudPrecioPaquete(1L, null, null, lunes));

        assertThat(p.montoDescuento()).isEqualTo(9800);
        assertThat(p.precio()).isZero();
    }

    @Test
    void siAplicanVariosGanaElQueMasRebaja() {
        Descuento diciembre = descuento(2, "Diciembre", TipoDescuento.MONTO_FIJO, 3000);
        descuentosVigentes(entreSemana, diciembre);

        assertThat(cotizarFull(lunes).descuento()).isEqualTo("Entre semana"); // 3,570 > 3,000
        assertThat(precios.calcular(new SolicitudPrecioPaquete(1L, null, null, lunes)).descuento())
                .isEqualTo("Diciembre"); // en la renta: 3,000 > 1,470
    }

    @Test
    void temporadaSoloAplicaDentroDeSusFechas() {
        Descuento temporada = descuento(2, "Temporada", TipoDescuento.PORCENTAJE, 10);
        temporada.setFechaInicio(lunes);
        temporada.setFechaFin(lunes.plusDays(2));
        descuentosVigentes(temporada);

        assertThat(cotizarFull(lunes).tieneDescuento()).isTrue();
        assertThat(cotizarFull(lunes.plusDays(2)).tieneDescuento()).isTrue();
        assertThat(cotizarFull(lunes.plusDays(3)).tieneDescuento()).isFalse();
        assertThat(cotizarFull(lunes.minusDays(1)).tieneDescuento()).isFalse();
    }

    @Test
    void cotizarHastaSeRevisaContraElDiaDeHoy() {
        Descuento vencido = descuento(2, "Buen Fin", TipoDescuento.PORCENTAJE, 10);
        vencido.setCotizarHasta(LocalDate.now().minusDays(1));
        Descuento vigente = descuento(3, "Hoy", TipoDescuento.PORCENTAJE, 5);
        vigente.setCotizarHasta(LocalDate.now());
        descuentosVigentes(vencido, vigente);

        // El evento puede ser meses después: lo que cuenta es el día en que se cotiza.
        assertThat(cotizarFull(lunes.plusMonths(3)).descuento()).isEqualTo("Hoy");
    }

    @Test
    void soloAplicaALosPaquetesElegidos() {
        Descuento soloRenta = descuento(2, "Renta entre semana", TipoDescuento.MONTO_FIJO, 1000);
        soloRenta.setTodosLosPaquetes(false);
        soloRenta.setPaquetes(Set.of(renta));
        descuentosVigentes(soloRenta);

        assertThat(precios.calcular(new SolicitudPrecioPaquete(1L, null, null, lunes)).precio()).isEqualTo(8800);
        assertThat(cotizarFull(lunes).tieneDescuento()).isFalse();
    }

    @Test
    void unDescuentoApagadoNoAplica() {
        entreSemana.setActivo(false);
        assertThat(ReglaDescuento.aplica(entreSemana, 2L, lunes, LocalDate.now())).isFalse();
    }

    @Test
    void elPorcentajeSeRedondeaACentavos() {
        Descuento raro = descuento(2, "Raro", TipoDescuento.PORCENTAJE, 12.5);
        assertThat(ReglaDescuento.monto(raro, 100.05)).isEqualTo(12.51);
    }

    @Test
    void vencidoCuandoTerminoLaTemporadaOElPlazoParaCotizar() {
        LocalDate hoy = LocalDate.now();
        Descuento d = descuento(2, "D", TipoDescuento.PORCENTAJE, 10);
        assertThat(ReglaDescuento.vencido(d, hoy)).isFalse();
        d.setFechaFin(hoy);
        assertThat(ReglaDescuento.vencido(d, hoy)).isFalse();
        d.setFechaFin(hoy.minusDays(1));
        assertThat(ReglaDescuento.vencido(d, hoy)).isTrue();
        d.setFechaFin(null);
        d.setCotizarHasta(hoy.minusDays(1));
        assertThat(ReglaDescuento.vencido(d, hoy)).isTrue();
    }

    // ── Cotizaciones ya guardadas ────────────────────────────────────────────

    private Cotizacion guardadaConDescuento(EstadoCotizacion estado, LocalDate fecha) {
        Cotizacion c = cotizacion(50, full, barbacoa, 100, 20230.0);
        c.setPrecioLista(23800.0);
        c.setDescuentoNombre("Entre semana");
        c.setDescuentoMonto(3570.0);
        c.setEstado(estado);
        Evento e = new Evento();
        e.setFecha(fecha);
        c.setEvento(e);
        return c;
    }

    @Test
    void mismaSeleccionYFechaConservaElDescuentoAunqueYaNoExista() {
        Cotizacion c = guardadaConDescuento(EstadoCotizacion.BORRADOR, lunes);

        PrecioPaquete p = precios.precioParaCotizacion(c, full, 10L, 100, lunes);

        assertThat(p.pactado()).isTrue();
        assertThat(p.precio()).isEqualTo(20230);
        assertThat(p.descuento()).isEqualTo("Entre semana");
        assertThat(p.montoDescuento()).isEqualTo(3570);
    }

    @Test
    void borradorConOtraFechaVuelveABuscarDescuentoSobreElPrecioDeListaPactado() {
        Cotizacion c = guardadaConDescuento(EstadoCotizacion.BORRADOR, lunes);
        full.getPrecios().get(0).setPrecio(25000); // el catálogo subió después de cotizar
        descuentosVigentes(entreSemana);

        PrecioPaquete aSabado = precios.precioParaCotizacion(c, full, 10L, 100, sabado);
        assertThat(aSabado.precio()).isEqualTo(23800);
        assertThat(aSabado.tieneDescuento()).isFalse();

        PrecioPaquete aMartes = precios.precioParaCotizacion(c, full, 10L, 100, martes);
        assertThat(aMartes.precio()).isEqualTo(20230);
        assertThat(aMartes.descuento()).isEqualTo("Entre semana");
    }

    @Test
    void cotizacionConfirmadaConservaSuPrecioAunqueCambieDeFecha() {
        Cotizacion c = guardadaConDescuento(EstadoCotizacion.VIGENTE, lunes);

        PrecioPaquete p = precios.precioParaCotizacion(c, full, 10L, 100, sabado);

        assertThat(p.pactado()).isTrue();
        assertThat(p.precio()).isEqualTo(20230);
        assertThat(p.descuento()).isEqualTo("Entre semana");
    }

    @Test
    void cotizacionAnteriorALosDescuentosTomaSuPrecioComoPrecioDeLista() {
        Cotizacion c = cotizacion(51, renta, null, null, 9800.0);

        PrecioPaquete p = CalcularPrecioPaqueteUseCase.pactado(c);

        assertThat(p.precioLista()).isEqualTo(9800);
        assertThat(p.tieneDescuento()).isFalse();
    }

    @Test
    void guardarEnLaCotizacionDejaElDesglose() {
        descuentosVigentes(entreSemana);
        Cotizacion c = cotizacion(52, full, barbacoa, 100, null);

        cotizarFull(lunes).guardarEn(c);

        assertThat(c.getPrecioPaquete()).isEqualTo(20230);
        assertThat(c.getPrecioLista()).isEqualTo(23800);
        assertThat(c.getDescuentoNombre()).isEqualTo("Entre semana");
        assertThat(c.getDescuentoMonto()).isEqualTo(3570);

        cotizarFull(sabado).guardarEn(c);
        assertThat(c.getDescuentoNombre()).isNull();
        assertThat(c.getDescuentoMonto()).isNull();
    }
}
