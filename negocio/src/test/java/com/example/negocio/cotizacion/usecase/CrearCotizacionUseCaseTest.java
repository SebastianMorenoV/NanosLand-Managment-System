package com.example.negocio.cotizacion.usecase;

import static com.example.negocio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.negocio.exception.CotizacionException;
import com.example.negocio.precio.CalcularPrecioPaqueteUseCase;
import com.mycompany.common.dtos.CotizacionDTO;
import com.mycompany.common.dtos.DetalleCotizacionDTO;
import com.mycompany.persistencia.dominio.Cliente;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.enums.CategoriaServicio;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.persistencia.repository.ClienteRepository;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.EventoRepository;
import com.mycompany.persistencia.repository.PaqueteRepository;
import com.mycompany.persistencia.repository.ServicioRepository;
import com.mycompany.persistencia.repository.UsuarioRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrearCotizacionUseCaseTest {

    @Mock CotizacionRepository cotizacionRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock PaqueteRepository paqueteRepository;
    @Mock ServicioRepository servicioRepository;
    @Mock EventoRepository eventoRepository;
    @Mock UsuarioRepository usuarioRepository;

    CrearCotizacionUseCase crear;

    Servicio barbacoa = comida(10, "Barbacoa");
    Servicio pinata = servicio(20, "Piñata", 400, CategoriaServicio.EXTRA);
    Servicio payasita = servicio(21, "Payasita (2 horas)", 0, CategoriaServicio.INCLUIDO);
    Paquete renta = paqueteFijo(1, "Renta de local", 9800);
    Paquete full = paqueteConTabla(2, "Paquete Full");

    @BeforeEach
    void setUp() {
        CalcularPrecioPaqueteUseCase precios = new CalcularPrecioPaqueteUseCase(paqueteRepository, cotizacionRepository);
        crear = new CrearCotizacionUseCase(cotizacionRepository, clienteRepository, paqueteRepository,
                servicioRepository, eventoRepository, usuarioRepository, precios);
        precio(full, barbacoa, 100, 23800);

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Cliente");
        lenient().when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        lenient().when(paqueteRepository.findById(1L)).thenReturn(Optional.of(renta));
        lenient().when(paqueteRepository.findById(2L)).thenReturn(Optional.of(full));
        lenient().when(servicioRepository.findById(10L)).thenReturn(Optional.of(barbacoa));
        lenient().when(servicioRepository.findById(20L)).thenReturn(Optional.of(pinata));
        lenient().when(servicioRepository.findById(21L)).thenReturn(Optional.of(payasita));
        lenient().when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static CotizacionDTO dto(long paqueteId, Long comidaId, Integer personas, DetalleCotizacionDTO... extras) {
        CotizacionDTO dto = new CotizacionDTO();
        dto.setClienteId(1L);
        dto.setFecha(LocalDate.now().plusDays(10));
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

    private Cotizacion guardada() {
        ArgumentCaptor<Cotizacion> captor = ArgumentCaptor.forClass(Cotizacion.class);
        verify(cotizacionRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void guardaComidaPersonasYPrecioPactadoDeLaTabla() {
        crear.crearCotizacion(dto(2, 10L, 100, extraDTO(20, 400)));

        Cotizacion c = guardada();
        assertThat(c.getPaquete()).isSameAs(full);
        assertThat(c.getComida()).isSameAs(barbacoa);
        assertThat(c.getPersonas()).isEqualTo(100);
        assertThat(c.getPrecioPaquete()).isEqualTo(23800);
        assertThat(c.getTotal()).isEqualTo(24200);
    }

    @Test
    void paqueteFijoGuardaSuPrecioSinComida() {
        crear.crearCotizacion(dto(1, null, null));

        Cotizacion c = guardada();
        assertThat(c.getComida()).isNull();
        assertThat(c.getPersonas()).isNull();
        assertThat(c.getPrecioPaquete()).isEqualTo(9800);
        assertThat(c.getTotal()).isEqualTo(9800);
    }

    @Test
    void noAceptaServiciosIncluidosComoExtra() {
        assertThatThrownBy(() -> crear.crearCotizacion(dto(1, null, null, extraDTO(21, 0))))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("no se vende como extra");
        verify(cotizacionRepository, never()).save(any());
    }

    @Test
    void noAceptaPaquetesDadosDeBaja() {
        renta.setActivo(false);
        assertThatThrownBy(() -> crear.crearCotizacion(dto(1, null, null)))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("ya no se ofrece");
    }

    @Test
    void paqueteConTablaSinComidaSeRechaza() {
        assertThatThrownBy(() -> crear.crearCotizacion(dto(2, null, 100)))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("comida");
    }

    @Test
    void elPrecioNoSeTomaDeLaPantalla() {
        CotizacionDTO dto = dto(1, null, null);
        dto.setTotal(1); // aunque la pantalla mande otro total
        dto.setPrecioPaquete(1.0);
        when(paqueteRepository.findById(1L)).thenReturn(Optional.of(renta));

        crear.crearCotizacion(dto);

        assertThat(guardada().getTotal()).isEqualTo(9800);
    }
}
