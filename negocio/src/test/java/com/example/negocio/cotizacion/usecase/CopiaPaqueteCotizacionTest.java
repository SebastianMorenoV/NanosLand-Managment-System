package com.example.negocio.cotizacion.usecase;

import static com.example.negocio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.example.negocio.logistica.usecase.GenerarLogisticaEventoUseCase;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.mycompany.persistencia.dominio.Cliente;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.enums.CategoriaServicio;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.PagoRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** La cotización conserva lo que incluía su paquete al venderse, aunque el paquete cambie después. */
@ExtendWith(MockitoExtension.class)
class CopiaPaqueteCotizacionTest {

    @Mock CotizacionRepository cotizacionRepository;
    @Mock PagoRepository pagoRepository;

    Servicio barbacoa = comida(10, "Barbacoa");
    Servicio mesero = servicio(22, "Mesero(a)", 500, CategoriaServicio.EXTRA);
    Servicio payasita = servicio(21, "Payasita (2 horas)", 0, CategoriaServicio.INCLUIDO);
    Paquete full;
    Cotizacion cot;

    @BeforeEach
    void setUp() {
        full = paqueteConTabla(2, "Paquete Full");
        full.setDescripcion("2 meseros y 1 payasita");
        incluido(full, mesero, 2);
        incluido(full, payasita, 1);
        cot = cotizacion(50, full, barbacoa, 100, 23800.0);
        cot.setTotal(23800);
        cot.setEstado(EstadoCotizacion.VIGENTE);
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente");
        cot.setCliente(cliente);
        Evento evento = new Evento();
        evento.setFecha(LocalDate.now().plusDays(30));
        evento.setTurno(TurnoEvento.VESPERTINO);
        evento.setHoraInicio(LocalTime.of(16, 0));
        evento.setHoraFin(LocalTime.of(21, 0));
        evento.setEstado(EstadoEvento.CONFIRMADO);
        evento.setCotizacion(cot);
        evento.setLogisticaServicios(new ArrayList<>());
        cot.setEvento(evento);
        lenient().when(cotizacionRepository.findById(50L)).thenReturn(Optional.of(cot));
        lenient().when(pagoRepository.findByCotizacionId(50L)).thenReturn(List.of());
    }

    /** El paquete del catálogo cambia después de vender la cotización. */
    private void cambiarElPaquete() {
        full.setNombre("Paquete Full 2027");
        full.setDescripcion("3 meseros, sin payasita");
        full.getServicios().get(0).setCantidad(3);
        full.getServicios().remove(1);
    }

    private String contrato() throws Exception {
        byte[] pdf = new GenerarContratoUseCase(cotizacionRepository, pagoRepository).generarContrato(50L);
        PdfReader reader = new PdfReader(pdf);
        String texto = new PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();
        return texto;
    }

    // ── Logística ───────────────────────────────────────────────────────────

    @Test
    void laLogisticaUsaLoQueIncluiaElPaqueteAlVenderse() {
        PaqueteCotizado.guardarCopia(cot);
        cambiarElPaquete();

        var contratados = new GenerarLogisticaEventoUseCase().serviciosContratados(cot);

        assertThat(contratados.get(22L).cantidad).isEqualTo(2);
        assertThat(contratados).containsKey(21L);
        assertThat(contratados.get(10L).cantidad).isEqualTo(100); // la comida, como siempre
    }

    @Test
    void laLogisticaSeparaLoVendidoDelPaqueteYCadaExtraAunqueElPaqueteCambie() {
        PaqueteCotizado.guardarCopia(cot); // se vendió con 2 meseros
        cambiarElPaquete();                 // hoy el paquete trae 3
        com.mycompany.persistencia.dominio.DetalleCotizacion meseroExtra = extra(cot, mesero, 1, 500);
        meseroExtra.setHoraSugerida(LocalTime.of(15, 0));

        new GenerarLogisticaEventoUseCase().sincronizar(cot.getEvento(), false);

        assertThat(cot.getEvento().getLogisticaServicios())
                .filteredOn(r -> r.getServicio().getId() == 22L)
                .extracting(r -> r.getOrigen() + " " + r.getCantidad() + " " + r.getHoraRequerida())
                .containsExactlyInAnyOrder("PAQUETE 2 null", "EXTRA 1 15:00");
    }

    @Test
    void sinCopiaLaLogisticaUsaElPaqueteDelCatalogoComoAntes() {
        cambiarElPaquete();

        var contratados = new GenerarLogisticaEventoUseCase().serviciosContratados(cot);

        assertThat(contratados.get(22L).cantidad).isEqualTo(3);
        assertThat(contratados).doesNotContainKey(21L);
    }

    // ── Contrato ────────────────────────────────────────────────────────────

    @Test
    void elContratoDiceLoQueSeVendio() throws Exception {
        PaqueteCotizado.guardarCopia(cot);
        cambiarElPaquete();

        String texto = contrato();

        assertThat(texto).contains("2 meseros y 1 payasita").contains("Paquete Full ·");
        assertThat(texto).doesNotContain("3 meseros, sin payasita").doesNotContain("Paquete Full 2027");
    }

    @Test
    void conLaCopiaRecienHechaElContratoQuedaIgualQueAntes() throws Exception {
        String antes = contrato();
        PaqueteCotizado.guardarCopia(cot);

        assertThat(contrato()).isEqualTo(antes);
    }

    // ── Corrección al arrancar ──────────────────────────────────────────────

    /** Otra cotización del Paquete Full, en el estado dado y con su evento en el estado dado. */
    private Cotizacion otra(long id, EstadoCotizacion estado, EstadoEvento estadoEvento) {
        Cotizacion c = cotizacion(id, full, barbacoa, 80, 22600.0);
        c.setEstado(estado);
        Evento e = new Evento();
        e.setEstado(estadoEvento);
        e.setCotizacion(c);
        c.setEvento(e);
        return c;
    }

    @Test
    void alArrancarGuardaLaCopiaDeLasVendidasSinTocarNadaMas() {
        Cotizacion borrador = otra(53, EstadoCotizacion.BORRADOR, EstadoEvento.TENTATIVO);
        Cotizacion realizada = otra(54, EstadoCotizacion.VIGENTE, EstadoEvento.FINALIZADO);
        Cotizacion cancelada = otra(55, EstadoCotizacion.CANCELADA, EstadoEvento.CANCELADO);
        Cotizacion eventoCancelado = otra(56, EstadoCotizacion.VIGENTE, EstadoEvento.CANCELADO_TARDIO);
        Cotizacion eliminada = otra(57, EstadoCotizacion.ELIMINADA, EstadoEvento.TENTATIVO);
        Paquete renta = paqueteFijo(1, "Renta de local", 9800);
        Cotizacion sinPaqueteActivo = cotizacion(51, renta, null, null, 9800.0);
        sinPaqueteActivo.setEstado(EstadoCotizacion.VIGENTE);
        renta.setActivo(false);
        Cotizacion yaConCopia = otra(52, EstadoCotizacion.VIGENTE, EstadoEvento.CONFIRMADO);
        PaqueteCotizado.guardarCopia(yaConCopia);
        yaConCopia.setPaqueteDescripcion("la que se vendió");
        when(cotizacionRepository.findByPaqueteIsNotNullAndPaqueteNombreIsNull()).thenReturn(List.of(
                cot, borrador, realizada, cancelada, eventoCancelado, eliminada, sinPaqueteActivo, yaConCopia));

        int completadas = new CompletarCopiaPaqueteUseCase(cotizacionRepository).completar();

        assertThat(completadas).isEqualTo(3);
        assertThat(borrador.tieneCopiaDelPaquete()).isTrue();
        assertThat(realizada.tieneCopiaDelPaquete()).isTrue();
        assertThat(cancelada.tieneCopiaDelPaquete()).isFalse();
        assertThat(eventoCancelado.tieneCopiaDelPaquete()).isFalse();
        assertThat(eliminada.tieneCopiaDelPaquete()).isFalse();
        assertThat(cot.getPaqueteNombre()).isEqualTo("Paquete Full");
        assertThat(cot.getPaqueteDescripcion()).isEqualTo("2 meseros y 1 payasita");
        assertThat(cot.getServiciosIncluidos()).extracting(s -> s.getNombre() + " x" + s.getCantidad())
                .containsExactly("Mesero(a) x2", "Payasita (2 horas) x1");
        assertThat(cot.getPrecioPaquete()).isEqualTo(23800);
        assertThat(cot.getTotal()).isEqualTo(23800);
        assertThat(cot.getPersonas()).isEqualTo(100);
        assertThat(sinPaqueteActivo.tieneCopiaDelPaquete()).isFalse();
        assertThat(yaConCopia.getPaqueteDescripcion()).isEqualTo("la que se vendió");
    }
}
