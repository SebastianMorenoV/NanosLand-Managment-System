package com.example.negocio.logistica.usecase;

import static com.example.negocio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.DetalleCotizacion;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.dominio.LogisticaServicio;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.enums.CategoriaServicio;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.enums.EstadoLogistica;
import com.mycompany.persistencia.enums.OrigenLogistica;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenerarLogisticaEventoUseCaseTest {

    GenerarLogisticaEventoUseCase logistica = new GenerarLogisticaEventoUseCase();

    Servicio barbacoa = comida(10, "Barbacoa");
    Servicio taquiza = comida(11, "Taquiza");
    Servicio mesero = servicio(22, "Mesero(a)", 500, CategoriaServicio.EXTRA);
    Paquete full = paqueteConTabla(2, "Paquete Full");
    Cotizacion cot;
    Evento evento;

    @BeforeEach
    void setUp() {
        incluido(full, mesero, 2);
        cot = cotizacion(50, full, barbacoa, 100, 23800.0);
        evento = new Evento();
        evento.setEstado(EstadoEvento.CONFIRMADO);
        evento.setCotizacion(cot);
        evento.setLogisticaServicios(new ArrayList<>());
        cot.setEvento(evento);
    }

    private LogisticaServicio registroDe(Servicio servicio) {
        return evento.getLogisticaServicios().stream()
                .filter(r -> r.getServicio().getId().equals(servicio.getId()))
                .findFirst().orElse(null);
    }

    @Test
    void incluyeLaComidaConUnaPorcionPorPersona() {
        var contratados = logistica.serviciosContratados(cot);

        assertThat(contratados).containsKeys(10L, 22L);
        assertThat(contratados.get(10L).cantidad).isEqualTo(100);
    }

    @Test
    void paqueteFijoNoTieneComida() {
        Paquete renta = paqueteFijo(1, "Renta de local", 9800);
        Cotizacion c = cotizacion(51, renta, null, null, 9800.0);

        assertThat(logistica.serviciosContratados(c)).isEmpty();
    }

    @Test
    void alConfirmarCreaElRegistroDeComidaConSuCantidad() {
        logistica.sincronizar(evento, false);

        assertThat(registroDe(barbacoa).getEspecificaciones()).isEqualTo("Cantidad: 100");
        assertThat(registroDe(mesero).getEspecificaciones()).isEqualTo("Cantidad: 2");
    }

    @Test
    void cambiarPersonasActualizaLaCantidadSinBorrarNotas() {
        logistica.sincronizar(evento, false);
        registroDe(barbacoa).setEspecificaciones("Cantidad: 100. Sin picante");

        cot.setPersonas(80);
        logistica.sincronizar(evento, false);

        assertThat(registroDe(barbacoa).getEspecificaciones()).isEqualTo("Cantidad: 80. Sin picante");
    }

    @Test
    void cambiarLaComidaReemplazaElRegistro() {
        logistica.sincronizar(evento, false);

        cot.setComida(taquiza);
        logistica.sincronizar(evento, false);

        assertThat(registroDe(barbacoa)).isNull();
        assertThat(registroDe(taquiza).getEspecificaciones()).isEqualTo("Cantidad: 100");
    }

    // ── Lo del paquete y los extras van aparte ──────────────────────────────

    Servicio bollito = servicio(28, "Bollito", 19, CategoriaServicio.EXTRA);

    /** 100 bollitos extra temáticos: los mismos datos que se capturan al agregar el extra. */
    private DetalleCotizacion bollitosExtra() {
        DetalleCotizacion d = extra(cot, bollito, 100, 19);
        d.setHoraSugerida(LocalTime.of(9, 30));
        d.setEspecificacionesCliente("Cars");
        d.setDesgloseOpciones("50 Ch / 50 V");
        d.setUbicacionMontaje("Mesa del Pastel");
        return d;
    }

    private List<LogisticaServicio> registrosDe(Servicio servicio) {
        return evento.getLogisticaServicios().stream()
                .filter(r -> r.getServicio().getId().equals(servicio.getId())).toList();
    }

    private LogisticaServicio registroDe(Servicio servicio, OrigenLogistica origen) {
        return registrosDe(servicio).stream().filter(r -> r.getOrigen() == origen).findFirst().orElse(null);
    }

    @Test
    void unExtraAgregadoDespuesDeConfirmarVaAparteConSusDatos() {
        // El caso del error: el paquete ya incluye 80 bollitos y, ya confirmado, se agregan 100 extra.
        incluido(full, bollito, 80);
        logistica.sincronizar(evento, false);
        registroDe(bollito, OrigenLogistica.PAQUETE).setEstado(EstadoLogistica.CONFIRMADO);

        bollitosExtra();
        logistica.sincronizar(evento, false);

        assertThat(registrosDe(bollito)).hasSize(2);
        LogisticaServicio paquete = registroDe(bollito, OrigenLogistica.PAQUETE);
        assertThat(paquete.getEspecificaciones()).isEqualTo("Cantidad: 80");
        assertThat(paquete.getCantidad()).isEqualTo(80);
        assertThat(paquete.getEstado()).isEqualTo(EstadoLogistica.CONFIRMADO);
        assertThat(paquete.getHoraRequerida()).isNull();
        LogisticaServicio extra = registroDe(bollito, OrigenLogistica.EXTRA);
        assertThat(extra.getCantidad()).isEqualTo(100);
        assertThat(extra.getEspecificaciones()).isEqualTo("Cantidad: 100. Cars");
        assertThat(extra.getHoraRequerida()).isEqualTo(LocalTime.of(9, 30));
        assertThat(extra.getDesgloseOpciones()).isEqualTo("50 Ch / 50 V");
        assertThat(extra.getUbicacionMontaje()).isEqualTo("Mesa del Pastel");
        assertThat(extra.getEstado()).isEqualTo(EstadoLogistica.POR_CONTACTAR);
    }

    @Test
    void conElExtraDesdeAntesDeConfirmarTambienVanAparte() {
        incluido(full, bollito, 80);
        bollitosExtra();

        logistica.sincronizar(evento, false);

        assertThat(registroDe(bollito, OrigenLogistica.PAQUETE).getHoraRequerida()).isNull();
        assertThat(registroDe(bollito, OrigenLogistica.PAQUETE).getEspecificaciones()).isEqualTo("Cantidad: 80");
        assertThat(registroDe(bollito, OrigenLogistica.EXTRA).getUbicacionMontaje()).isEqualTo("Mesa del Pastel");
    }

    @Test
    void cambiarLosDatosDelExtraSoloActualizaLoQueCambioYConservaLoDeLogistica() {
        DetalleCotizacion d = bollitosExtra();
        logistica.sincronizar(evento, false);
        LogisticaServicio registro = registroDe(bollito, OrigenLogistica.EXTRA);
        registro.setResponsableTurno("Ana");
        registro.setEstado(EstadoLogistica.CONFIRMADO);
        registro.setHoraRequerida(LocalTime.of(9, 0)); // el coordinador la ajustó

        d.setUbicacionMontaje("Mesa de dulces"); // en la cotización cambian la ubicación
        d.setCantidad(120);
        logistica.sincronizar(evento, false);

        assertThat(registrosDe(bollito)).containsExactly(registro);
        assertThat(registro.getUbicacionMontaje()).isEqualTo("Mesa de dulces");
        assertThat(registro.getHoraRequerida()).isEqualTo(LocalTime.of(9, 0));
        assertThat(registro.getResponsableTurno()).isEqualTo("Ana");
        assertThat(registro.getEstado()).isEqualTo(EstadoLogistica.CONFIRMADO);
        assertThat(registro.getEspecificaciones()).isEqualTo("Cantidad: 120. Cars");
    }

    @Test
    void dosExtrasDelMismoServicioConDatosDistintosVanCadaUnoAparte() {
        bollitosExtra();
        DetalleCotizacion otros = extra(cot, bollito, 30, 19);
        otros.setEspecificacionesCliente("Frozen");

        logistica.sincronizar(evento, false);

        assertThat(registrosDe(bollito)).extracting(LogisticaServicio::getEspecificaciones)
                .containsExactlyInAnyOrder("Cantidad: 100. Cars", "Cantidad: 30. Frozen");
    }

    @Test
    void quitarElExtraQuitaSoloSuRegistro() {
        incluido(full, bollito, 80);
        DetalleCotizacion d = bollitosExtra();
        logistica.sincronizar(evento, false);

        cot.getServiciosExtra().remove(d);
        logistica.sincronizar(evento, false);

        assertThat(registrosDe(bollito)).hasSize(1);
        assertThat(registroDe(bollito, OrigenLogistica.PAQUETE).getEspecificaciones()).isEqualTo("Cantidad: 80");
    }

    // ── Registros de antes de separarlos ────────────────────────────────────

    /** Un registro como los de antes: sin origen, con todo el servicio sumado. */
    private LogisticaServicio registroAnterior(Servicio servicio, String especificaciones) {
        LogisticaServicio r = new LogisticaServicio();
        r.setEvento(evento);
        r.setServicio(servicio);
        r.setEstado(EstadoLogistica.CONFIRMADO);
        r.setResponsableTurno("Ana");
        r.setEspecificaciones(especificaciones);
        evento.getLogisticaServicios().add(r);
        return r;
    }

    @Test
    void unRegistroDeAntesSumadoSeSeparaConservandoSuEstado() {
        incluido(full, bollito, 80);
        bollitosExtra();
        LogisticaServicio anterior = registroAnterior(bollito, "Cantidad: 180");

        logistica.sincronizar(evento, false);

        assertThat(anterior.getOrigen()).isEqualTo(OrigenLogistica.PAQUETE);
        assertThat(anterior.getEspecificaciones()).isEqualTo("Cantidad: 80");
        assertThat(anterior.getEstado()).isEqualTo(EstadoLogistica.CONFIRMADO);
        assertThat(anterior.getResponsableTurno()).isEqualTo("Ana");
        LogisticaServicio extra = registroDe(bollito, OrigenLogistica.EXTRA);
        assertThat(extra.getEspecificaciones()).isEqualTo("Cantidad: 100. Cars");
        assertThat(extra.getEstado()).isEqualTo(EstadoLogistica.POR_CONTACTAR);
    }

    @Test
    void unRegistroDeAntesCreadoConLosDatosDelExtraQuedaComoDelExtra() {
        incluido(full, bollito, 80);
        bollitosExtra();
        LogisticaServicio anterior = registroAnterior(bollito, "Cantidad: 180. Cars");
        anterior.setHoraRequerida(LocalTime.of(9, 30));
        anterior.setDesgloseOpciones("50 Ch / 50 V");
        anterior.setUbicacionMontaje("Mesa del Pastel");

        logistica.sincronizar(evento, false);

        assertThat(anterior.getOrigen()).isEqualTo(OrigenLogistica.EXTRA);
        assertThat(anterior.getEspecificaciones()).isEqualTo("Cantidad: 100. Cars");
        assertThat(anterior.getResponsableTurno()).isEqualTo("Ana");
        assertThat(registroDe(bollito, OrigenLogistica.PAQUETE).getEspecificaciones()).isEqualTo("Cantidad: 80");
    }

    @Test
    void losRegistrosDeAntesDeOtrosServiciosSoloRecibenSuOrigen() {
        LogisticaServicio anterior = registroAnterior(mesero, "Cantidad: 2. Con corbata");

        logistica.sincronizar(evento, false);

        assertThat(registrosDe(mesero)).containsExactly(anterior);
        assertThat(anterior.getOrigen()).isEqualTo(OrigenLogistica.PAQUETE);
        assertThat(anterior.getCantidad()).isEqualTo(2);
        assertThat(anterior.getEspecificaciones()).isEqualTo("Cantidad: 2. Con corbata");
    }

    // ── Corrección al arrancar: solo separa lo sumado ───────────────────────

    @Test
    void laCorreccionSeparaLoSumadoConservandoEstadoYResponsable() {
        incluido(full, bollito, 80);
        bollitosExtra();
        LogisticaServicio anterior = registroAnterior(bollito, "Cantidad: 180");

        logistica.separarRegistrosAnteriores(evento);

        assertThat(anterior.getOrigen()).isEqualTo(OrigenLogistica.PAQUETE);
        assertThat(anterior.getEspecificaciones()).isEqualTo("Cantidad: 80");
        assertThat(anterior.getResponsableTurno()).isEqualTo("Ana");
        assertThat(registroDe(bollito, OrigenLogistica.EXTRA).getEspecificaciones()).isEqualTo("Cantidad: 100. Cars");
    }

    @Test
    void laCorreccionNoTocaLoQueNoEstabaSumadoAunqueElCatalogoHayaCambiado() {
        // El registro dice 2 meseros; el paquete del catálogo hoy dice 3.
        LogisticaServicio mesero2 = registroAnterior(mesero, "Cantidad: 2. Con corbata");
        full.getServicios().get(0).setCantidad(3);
        LogisticaServicio yaNoContratado = registroAnterior(taquiza, "Cantidad: 50");

        logistica.separarRegistrosAnteriores(evento);

        assertThat(mesero2.getOrigen()).isEqualTo(OrigenLogistica.PAQUETE);
        assertThat(mesero2.getEspecificaciones()).isEqualTo("Cantidad: 2. Con corbata");
        assertThat(mesero2.getCantidad()).isNull();
        assertThat(evento.getLogisticaServicios()).contains(yaNoContratado); // no quita nada
        assertThat(registroDe(barbacoa)).isNull(); // ni crea lo que no estaba sumado
    }

    @Test
    void laCorreccionNoHaceNadaSiYaEstaSeparado() {
        incluido(full, bollito, 80);
        bollitosExtra();
        logistica.sincronizar(evento, false);
        int antes = evento.getLogisticaServicios().size();

        logistica.separarRegistrosAnteriores(evento);

        assertThat(evento.getLogisticaServicios()).hasSize(antes);
    }

    @Test
    void elTotalPorServicioSigueSumandoPaqueteYExtras() {
        incluido(full, bollito, 80);
        bollitosExtra();

        assertThat(logistica.serviciosContratados(cot).get(28L).cantidad).isEqualTo(180);
    }

    @Test
    void prefijoDeCantidad() {
        assertThat(GenerarLogisticaEventoUseCase.conCantidad(null, 1)).isNull();
        assertThat(GenerarLogisticaEventoUseCase.conCantidad("Cantidad: 3. Azul", 1)).isEqualTo("Azul");
        assertThat(GenerarLogisticaEventoUseCase.conCantidad("Azul", 4)).isEqualTo("Cantidad: 4. Azul");
        assertThat(GenerarLogisticaEventoUseCase.conCantidad("Cantidad: 3", 5)).isEqualTo("Cantidad: 5");
    }
}
