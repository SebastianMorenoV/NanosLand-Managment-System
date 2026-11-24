package com.example.negocio.logistica.usecase;

import static com.example.negocio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.dominio.LogisticaServicio;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.enums.CategoriaServicio;
import com.mycompany.persistencia.enums.EstadoEvento;
import java.util.ArrayList;
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

    @Test
    void prefijoDeCantidad() {
        assertThat(GenerarLogisticaEventoUseCase.conCantidad(null, 1)).isNull();
        assertThat(GenerarLogisticaEventoUseCase.conCantidad("Cantidad: 3. Azul", 1)).isEqualTo("Azul");
        assertThat(GenerarLogisticaEventoUseCase.conCantidad("Azul", 4)).isEqualTo("Cantidad: 4. Azul");
        assertThat(GenerarLogisticaEventoUseCase.conCantidad("Cantidad: 3", 5)).isEqualTo("Cantidad: 5");
    }
}
