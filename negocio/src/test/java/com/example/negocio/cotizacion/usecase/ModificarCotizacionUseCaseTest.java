package com.example.negocio.cotizacion.usecase;

import static com.example.negocio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.example.negocio.exception.CotizacionException;
import com.example.negocio.logistica.usecase.GenerarLogisticaEventoUseCase;
import com.example.negocio.precio.CalcularPrecioPaqueteUseCase;
import com.mycompany.common.dtos.CotizacionDTO;
import com.mycompany.common.dtos.DetalleCotizacionDTO;
import com.mycompany.persistencia.dominio.Cliente;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.enums.CategoriaServicio;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.persistencia.repository.ClienteRepository;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.EventoRepository;
import com.mycompany.persistencia.repository.PagoRepository;
import com.mycompany.persistencia.repository.PaqueteRepository;
import com.mycompany.persistencia.repository.ServicioRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModificarCotizacionUseCaseTest {

    @Mock CotizacionRepository cotizacionRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock PaqueteRepository paqueteRepository;
    @Mock ServicioRepository servicioRepository;
    @Mock EventoRepository eventoRepository;
    @Mock PagoRepository pagoRepository;

    ModificarCotizacionUseCase modificar;

    LocalDate fecha = LocalDate.now().plusDays(15);
    Servicio barbacoa = comida(10, "Barbacoa");
    Servicio pinata = servicio(20, "Piñata", 400, CategoriaServicio.EXTRA);
    Servicio mesero = servicio(22, "Mesero(a)", 500, CategoriaServicio.EXTRA);
    Servicio payasita = servicio(21, "Payasita (2 horas)", 0, CategoriaServicio.INCLUIDO);
    Paquete renta = paqueteFijo(1, "Renta de local", 9800);
    Paquete full = paqueteConTabla(2, "Paquete Full");

    @BeforeEach
    void setUp() {
        CalcularPrecioPaqueteUseCase precios = new CalcularPrecioPaqueteUseCase(paqueteRepository, cotizacionRepository);
        modificar = new ModificarCotizacionUseCase(cotizacionRepository, clienteRepository, paqueteRepository,
                servicioRepository, eventoRepository, pagoRepository, new GenerarLogisticaEventoUseCase(), precios);
        precio(full, barbacoa, 80, 22600);
        precio(full, barbacoa, 100, 23800);

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        lenient().when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        lenient().when(paqueteRepository.findById(1L)).thenReturn(Optional.of(renta));
        lenient().when(paqueteRepository.findById(2L)).thenReturn(Optional.of(full));
        lenient().when(servicioRepository.findById(10L)).thenReturn(Optional.of(barbacoa));
        lenient().when(servicioRepository.findById(20L)).thenReturn(Optional.of(pinata));
        lenient().when(servicioRepository.findById(21L)).thenReturn(Optional.of(payasita));
        lenient().when(servicioRepository.findById(22L)).thenReturn(Optional.of(mesero));
        lenient().when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Cotizacion existente(Paquete paquete, Servicio comida, Integer personas, Double precioPaquete, double total) {
        Cotizacion c = cotizacion(50, paquete, comida, personas, precioPaquete);
        c.setEstado(EstadoCotizacion.BORRADOR);
        c.setTotal(total);
        Evento e = new Evento();
        e.setId(500L);
        e.setFecha(fecha);
        e.setTurno(TurnoEvento.MATUTINO);
        e.setEstado(EstadoEvento.TENTATIVO);
        e.setCotizacion(c);
        e.setLogisticaServicios(new ArrayList<>());
        c.setEvento(e);
        lenient().when(cotizacionRepository.findById(50L)).thenReturn(Optional.of(c));
        return c;
    }

    private CotizacionDTO dto(long paqueteId, Long comidaId, Integer personas, DetalleCotizacionDTO... extras) {
        CotizacionDTO dto = new CotizacionDTO();
        dto.setClienteId(1L);
        dto.setFecha(fecha);
        dto.setTurno(TurnoEvento.MATUTINO);
        dto.setPaqueteId(paqueteId);
        dto.setComidaId(comidaId);
        dto.setPersonas(personas);
        dto.setDetalles(List.of(extras));
        return dto;
    }

    private static DetalleCotizacionDTO extraDTO(long servicioId, double precio) {
        DetalleCotizacionDTO d = new DetalleCotizacionDTO();
        d.setServicioId(servicioId);
        d.setPrecioUnitario(precio);
        d.setCantidad(1);
        d.setSubtotal(precio);
        return d;
    }

    @Test
    void conservaElPrecioPactadoAunqueSubaElPaquete() {
        Cotizacion c = existente(full, barbacoa, 100, 23800.0, 23800);
        full.getPrecios().get(1).setPrecio(25000);

        modificar.modificarCotizacion(50L, dto(2, 10L, 100, extraDTO(20, 400)));

        assertThat(c.getPrecioPaquete()).isEqualTo(23800);
        assertThat(c.getTotal()).isEqualTo(24200);
    }

    @Test
    void conservaElPrecioDeUnPaqueteFijoAunqueCambieSuCosto() {
        // Antes la edición volvía a cobrar el paquete con su costo del momento.
        Cotizacion c = existente(renta, null, null, 9800.0, 9800);
        renta.setCosto(10500);

        modificar.modificarCotizacion(50L, dto(1, null, null));

        assertThat(c.getTotal()).isEqualTo(9800);
    }

    @Test
    void cambiarPersonasTomaElPrecioDelCatalogo() {
        Cotizacion c = existente(full, barbacoa, 100, 23800.0, 23800);

        modificar.modificarCotizacion(50L, dto(2, 10L, 80));

        assertThat(c.getPersonas()).isEqualTo(80);
        assertThat(c.getPrecioPaquete()).isEqualTo(22600);
        assertThat(c.getTotal()).isEqualTo(22600);
    }

    @Test
    void cotizacionAntiguaSinPrecioPactadoLoDeduceDelTotal() {
        Cotizacion c = existente(renta, null, null, null, 10700);
        extra(c, pinata, 1, 400);
        extra(c, mesero, 1, 500);
        renta.setCosto(12000);

        modificar.modificarCotizacion(50L, dto(1, null, null, extraDTO(20, 400), extraDTO(22, 500)));

        assertThat(c.getPrecioPaquete()).isEqualTo(9800);
        assertThat(c.getTotal()).isEqualTo(10700);
    }

    @Test
    void aceptaUnExtraQueYaTeniaAunqueHoyNoSeaExtra() {
        Cotizacion c = existente(renta, null, null, 9800.0, 9800);
        extra(c, payasita, 1, 0);

        modificar.modificarCotizacion(50L, dto(1, null, null, extraDTO(21, 0)));

        assertThat(c.getServiciosExtra()).extracting(d -> d.getServicio().getId()).containsExactly(21L);
    }

    @Test
    void rechazaAgregarUnServicioIncluidoComoExtraNuevo() {
        existente(renta, null, null, 9800.0, 9800);

        assertThatThrownBy(() -> modificar.modificarCotizacion(50L, dto(1, null, null, extraDTO(21, 0))))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("no se vende como extra");
    }
}
