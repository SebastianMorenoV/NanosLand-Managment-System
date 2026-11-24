package com.example.negocio.catalogo.usecase;

import static com.example.negocio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.ServicioDTO;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.enums.CategoriaServicio;
import com.mycompany.persistencia.repository.PaquetePrecioRepository;
import com.mycompany.persistencia.repository.PaqueteServicioRepository;
import com.mycompany.persistencia.repository.ServicioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GestionarServicioUseCaseTest {

    @Mock ServicioRepository servicioRepository;
    @Mock PaqueteServicioRepository paqueteServicioRepository;
    @Mock PaquetePrecioRepository paquetePrecioRepository;

    GestionarServicioUseCase servicios;

    Servicio barbacoa = comida(10, "Barbacoa");
    Servicio mesero = servicio(22, "Mesero(a)", 500, CategoriaServicio.EXTRA);

    @BeforeEach
    void setUp() {
        servicios = new GestionarServicioUseCase(servicioRepository, paqueteServicioRepository, paquetePrecioRepository);
        lenient().when(servicioRepository.findById(10L)).thenReturn(Optional.of(barbacoa));
        lenient().when(servicioRepository.findById(22L)).thenReturn(Optional.of(mesero));
        lenient().when(servicioRepository.save(any(Servicio.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void unaComidaSiempreQuedaConPrecioCero() {
        ServicioDTO dto = servicios.crearServicio("Taquiza", 150, null, CategoriaServicio.COMIDA);
        assertThat(dto.getPrecio()).isZero();
        assertThat(dto.getCategoria()).isEqualTo(CategoriaServicio.COMIDA);
    }

    @Test
    void sinCategoriaEsExtra() {
        assertThat(servicios.crearServicio("Piñata", 400, null, null).getCategoria()).isEqualTo(CategoriaServicio.EXTRA);
    }

    @Test
    void noSeConvierteEnComidaUnServicioIncluidoEnPaquetes() {
        when(paqueteServicioRepository.nombresPaquetesActivosConServicio(22L)).thenReturn(List.of("Paquete Plus"));

        assertThatThrownBy(() -> servicios.actualizarServicio(22L, "Mesero(a)", 500, null, CategoriaServicio.COMIDA))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("Paquete Plus");
    }

    @Test
    void unaComidaEnUsoNoCambiaDeCategoriaNiSeElimina() {
        when(paquetePrecioRepository.nombresPaquetesActivosConComida(10L)).thenReturn(List.of("Paquete Full", "Paquete Neón"));

        assertThatThrownBy(() -> servicios.actualizarServicio(10L, "Barbacoa", 0, null, CategoriaServicio.EXTRA))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("los paquetes Paquete Full, Paquete Neón");
        assertThatThrownBy(() -> servicios.eliminarServicioLogico(10L))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("tabla de precios");
    }

    @Test
    void entreExtraEIncluidoSePuedeCambiarSiempre() {
        ServicioDTO dto = servicios.actualizarServicio(22L, "Mesero(a)", 500, null, CategoriaServicio.INCLUIDO);
        assertThat(dto.getCategoria()).isEqualTo(CategoriaServicio.INCLUIDO);
    }
}
