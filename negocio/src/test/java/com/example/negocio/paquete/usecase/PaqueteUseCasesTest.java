package com.example.negocio.paquete.usecase;

import static com.example.negocio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.dtos.PaquetePrecioDTO;
import com.mycompany.common.dtos.PaqueteServicioDTO;
import com.mycompany.common.dtos.ServicioDTO;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.dominio.PaquetePrecio;
import com.mycompany.persistencia.dominio.PaqueteServicio;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.enums.CategoriaServicio;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.PaqueteRepository;
import com.mycompany.persistencia.repository.ServicioRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Alta y modificación de paquetes con servicios incluidos y tabla de precios. */
@ExtendWith(MockitoExtension.class)
class PaqueteUseCasesTest {

    @Mock PaqueteRepository paqueteRepository;
    @Mock CotizacionRepository cotizacionRepository;
    @Mock ServicioRepository servicioRepository;

    RegistrarPaqueteUseCase registrar;
    ActualizarPaqueteUseCase actualizar;

    Servicio barbacoa = comida(10, "Barbacoa");
    Servicio taquiza = comida(11, "Taquiza");
    Servicio mesero = servicio(22, "Mesero(a)", 500, CategoriaServicio.EXTRA);
    Servicio payasita = servicio(21, "Payasita (2 horas)", 0, CategoriaServicio.INCLUIDO);
    Map<Long, Servicio> catalogo = Map.of(10L, barbacoa, 11L, taquiza, 22L, mesero, 21L, payasita);

    Paquete full;

    @BeforeEach
    void setUp() {
        ValidadorPaquete validador = new ValidadorPaquete(servicioRepository);
        registrar = new RegistrarPaqueteUseCase(paqueteRepository, validador);
        actualizar = new ActualizarPaqueteUseCase(paqueteRepository, cotizacionRepository, validador);

        lenient().when(servicioRepository.findAllById(anyIterable())).thenAnswer(i -> {
            Iterable<Long> ids = i.getArgument(0);
            return StreamSupport.stream(ids.spliterator(), false)
                    .map(catalogo::get).filter(s -> s != null).toList();
        });
        lenient().when(paqueteRepository.save(any(Paquete.class))).thenAnswer(i -> i.getArgument(0));

        full = paqueteConTabla(2, "Paquete Full");
        incluido(full, mesero, 2);
        precio(full, barbacoa, 80, 22600);
        precio(full, barbacoa, 100, 23800);
        lenient().when(paqueteRepository.findById(2L)).thenReturn(Optional.of(full));
        lenient().when(cotizacionRepository.combinacionesAbiertasDePaquete(anyLong(), anyCollection(), any()))
                .thenReturn(List.of());
        lenient().when(cotizacionRepository.foliosAbiertosSinCopiaDelPaquete(anyLong(), anyCollection(), any()))
                .thenReturn(List.of());
    }

    /** Cotizaciones abiertas de antes de la copia del paquete: dependen del paquete del catálogo. */
    private void sinCopia(String... folios) {
        when(cotizacionRepository.foliosAbiertosSinCopiaDelPaquete(anyLong(), anyCollection(), any()))
                .thenReturn(List.of(folios));
    }

    private static ServicioDTO s(Servicio servicio) {
        ServicioDTO dto = new ServicioDTO();
        dto.setId(servicio.getId());
        dto.setNombre(servicio.getNombre());
        dto.setPrecio(servicio.getPrecio());
        dto.setCategoria(servicio.getCategoria());
        return dto;
    }

    private static PaqueteServicioDTO inc(Servicio servicio, int cantidad) {
        return new PaqueteServicioDTO(null, s(servicio), cantidad, 0);
    }

    private static PaquetePrecioDTO pr(Servicio comida, int personas, double precio) {
        return new PaquetePrecioDTO(null, comida.getId(), comida.getNombre(), personas, precio);
    }

    private PaqueteDTO dtoFull(List<PaqueteServicioDTO> servicios, List<PaquetePrecioDTO> precios) {
        PaqueteDTO dto = new PaqueteDTO();
        dto.setId(2L);
        dto.setNombre("Paquete Full");
        dto.setServicios(servicios);
        dto.setPrecios(precios);
        return dto;
    }

    private void conCotizacionesAbiertas(Object[]... filas) {
        when(cotizacionRepository.combinacionesAbiertasDePaquete(anyLong(), anyCollection(), any()))
                .thenReturn(List.of(filas));
    }

    // ── Alta ────────────────────────────────────────────────────────────────

    @Test
    void altaConTablaDejaElPrecioFijoEnCeroYCalculaSubtotales() {
        PaqueteDTO dto = new PaqueteDTO();
        dto.setNombre("  Paquete Neón ");
        dto.setCostoBase(999);
        dto.setServicios(List.of(inc(mesero, 2), inc(payasita, 1)));
        dto.setPrecios(List.of(pr(barbacoa, 100, 30600), pr(taquiza, 100, 30200)));

        PaqueteDTO guardado = registrar.registrarPaquete(dto);

        assertThat(guardado.getNombre()).isEqualTo("Paquete Neón");
        assertThat(guardado.getCostoBase()).isZero();
        assertThat(guardado.isTablaPrecios()).isTrue();
        assertThat(guardado.getPrecioDesde()).isEqualTo(30200);
        assertThat(guardado.getServicios()).extracting(PaqueteServicioDTO::getSubtotal).containsExactly(1000.0, 0.0);
    }

    @Test
    void altaDePrecioFijoPideUnPrecioMayorACero() {
        PaqueteDTO dto = new PaqueteDTO();
        dto.setNombre("Renta de local");
        assertThatThrownBy(() -> registrar.registrarPaquete(dto))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("mayor a cero");
    }

    @Test
    void altaRechazaComidaRepetidaPrecioEnCeroYComidaComoIncluido() {
        PaqueteDTO repetida = new PaqueteDTO();
        repetida.setNombre("X");
        repetida.setPrecios(List.of(pr(barbacoa, 100, 1), pr(barbacoa, 100, 2)));
        assertThatThrownBy(() -> registrar.registrarPaquete(repetida)).hasMessageContaining("repetido");

        PaqueteDTO sinPrecio = new PaqueteDTO();
        sinPrecio.setNombre("X");
        sinPrecio.setPrecios(List.of(pr(barbacoa, 100, 0)));
        assertThatThrownBy(() -> registrar.registrarPaquete(sinPrecio)).hasMessageContaining("Falta el precio");

        PaqueteDTO comidaIncluida = new PaqueteDTO();
        comidaIncluida.setNombre("X");
        comidaIncluida.setCostoBase(100);
        comidaIncluida.setServicios(List.of(inc(barbacoa, 1)));
        assertThatThrownBy(() -> registrar.registrarPaquete(comidaIncluida)).hasMessageContaining("es una comida");
    }

    @Test
    void altaRechazaUnServicioQueNoEsComidaEnLaTabla() {
        PaqueteDTO dto = new PaqueteDTO();
        dto.setNombre("X");
        dto.setPrecios(List.of(pr(mesero, 100, 100)));
        assertThatThrownBy(() -> registrar.registrarPaquete(dto))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("no es de categoría Comida");
    }

    // ── Modificación ────────────────────────────────────────────────────────

    @Test
    void actualizaPreciosEnSuLugarYAgregaNuevos() {
        PaquetePrecio renglon100 = full.getPrecios().get(1);

        actualizar.actualizarPaquete(dtoFull(null,
                List.of(pr(barbacoa, 80, 22600), pr(barbacoa, 100, 24000), pr(taquiza, 100, 23400))));

        assertThat(full.getPrecios()).hasSize(3);
        assertThat(full.getPrecios()).contains(renglon100);
        assertThat(renglon100.getPrecio()).isEqualTo(24000);
        assertThat(full.getServicios()).hasSize(1); // servicios = null: sin cambios
    }

    @Test
    void conCotizacionesAbiertasSePuedenCambiarPrecios() {
        conCotizacionesAbiertas(new Object[]{"COT-1", 10L, 100});

        actualizar.actualizarPaquete(dtoFull(List.of(inc(mesero, 2)),
                List.of(pr(barbacoa, 80, 23000), pr(barbacoa, 100, 25000))));

        assertThat(full.getPrecios().get(1).getPrecio()).isEqualTo(25000);
    }

    @Test
    void conCotizacionesAbiertasConCopiaSeCambianLosServiciosIncluidos() {
        conCotizacionesAbiertas(new Object[]{"COT-1", 10L, 100});

        actualizar.actualizarPaquete(dtoFull(new ArrayList<>(List.of(inc(mesero, 3), inc(payasita, 1))), null));

        assertThat(full.getServicios()).extracting(ps -> ps.getServicio().getNombre() + " x" + ps.getCantidad())
                .containsExactly("Mesero(a) x3", "Payasita (2 horas) x1");
    }

    @Test
    void conCotizacionesAbiertasConCopiaSeQuitaUnaCombinacionQueUsan() {
        conCotizacionesAbiertas(new Object[]{"COT-1", 10L, 100}, new Object[]{"COT-2", 10L, 80});

        actualizar.actualizarPaquete(dtoFull(null, List.of(pr(barbacoa, 80, 22600))));

        assertThat(full.getPrecios()).extracting(PaquetePrecio::getPersonas).containsExactly(80);
    }

    @Test
    void conCotizacionesAbiertasSinCopiaNoSeCambianLosServiciosIncluidos() {
        conCotizacionesAbiertas(new Object[]{"COT-1", 10L, 100});
        sinCopia("COT-1");

        assertThatThrownBy(() -> actualizar.actualizarPaquete(dtoFull(List.of(inc(mesero, 3)), null)))
                .isInstanceOf(CotizacionException.class)
                .hasMessageContaining("servicios incluidos").hasMessageContaining("COT-1");
        assertThat(full.getServicios()).extracting(PaqueteServicio::getCantidad).containsExactly(2);
    }

    @Test
    void conCotizacionesAbiertasSinCopiaNoSeQuitaUnaCombinacionEnUso() {
        conCotizacionesAbiertas(new Object[]{"COT-1", 10L, 100}, new Object[]{"COT-2", 10L, 80});
        sinCopia("COT-1");

        assertThatThrownBy(() -> actualizar.actualizarPaquete(dtoFull(null, List.of(pr(barbacoa, 80, 22600)))))
                .isInstanceOf(CotizacionException.class)
                .hasMessageContaining("Barbacoa con 100 personas (COT-1)");
    }

    @Test
    void conCotizacionesAbiertasDeTablaNoSePasaAPrecioFijo() {
        conCotizacionesAbiertas(new Object[]{"COT-1", 10L, 100});
        PaqueteDTO dto = dtoFull(null, List.of());
        dto.setCostoBase(20000);

        assertThatThrownBy(() -> actualizar.actualizarPaquete(dto))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("COT-1");
    }

    @Test
    void conCotizacionesAbiertasDePrecioFijoNoSePasaATabla() {
        Paquete renta = paqueteFijo(1, "Renta de local", 9800);
        when(paqueteRepository.findById(1L)).thenReturn(Optional.of(renta));
        conCotizacionesAbiertas(new Object[]{"COT-9", null, null});
        PaqueteDTO dto = new PaqueteDTO();
        dto.setId(1L);
        dto.setNombre("Renta de local");
        dto.setPrecios(List.of(pr(barbacoa, 100, 100)));

        assertThatThrownBy(() -> actualizar.actualizarPaquete(dto))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("precio fijo").hasMessageContaining("COT-9");
    }

    @Test
    void sinCotizacionesAbiertasSeCambianLosServicios() {
        actualizar.actualizarPaquete(dtoFull(new ArrayList<>(List.of(inc(mesero, 1), inc(payasita, 1)))
                , null));

        assertThat(full.getServicios()).extracting(ps -> ps.getServicio().getNombre())
                .containsExactly("Mesero(a)", "Payasita (2 horas)");
    }
}
