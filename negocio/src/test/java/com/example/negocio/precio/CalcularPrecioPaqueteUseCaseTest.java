package com.example.negocio.precio;

import static com.example.negocio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.enums.CategoriaServicio;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.PaqueteRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalcularPrecioPaqueteUseCaseTest {

    @Mock PaqueteRepository paqueteRepository;
    @Mock CotizacionRepository cotizacionRepository;

    CalcularPrecioPaqueteUseCase precios;

    Servicio barbacoa = comida(10, "Barbacoa");
    Servicio taquiza = comida(11, "Taquiza");
    Paquete renta = paqueteFijo(1, "Renta de local", 9800);
    Paquete full = paqueteConTabla(2, "Paquete Full");

    @BeforeEach
    void setUp() {
        precios = new CalcularPrecioPaqueteUseCase(paqueteRepository, cotizacionRepository);
        precio(full, barbacoa, 80, 22600);
        precio(full, barbacoa, 100, 23800);
        precio(full, taquiza, 100, 23400);
    }

    @Test
    void paqueteFijoCuestaSuPrecio() {
        when(paqueteRepository.findById(1L)).thenReturn(Optional.of(renta));

        PrecioPaquete p = precios.calcular(new SolicitudPrecioPaquete(1L, null, null, null));

        assertThat(p.precio()).isEqualTo(9800);
        assertThat(p.pactado()).isFalse();
    }

    @Test
    void paqueteFijoNoAceptaComidaNiPersonas() {
        assertThatThrownBy(() -> precios.precioDeLista(renta, 10L, null, null))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("precio fijo");
        assertThatThrownBy(() -> precios.precioDeLista(renta, null, 100, null))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("precio fijo");
    }

    @Test
    void paqueteConTablaTomaElRenglonDeComidaYPersonas() {
        when(paqueteRepository.findById(2L)).thenReturn(Optional.of(full));

        assertThat(precios.calcular(new SolicitudPrecioPaquete(2L, 10L, 100, null)).precio()).isEqualTo(23800);
        assertThat(precios.calcular(new SolicitudPrecioPaquete(2L, 11L, 100, null)).precio()).isEqualTo(23400);
    }

    @Test
    void paqueteConTablaPideComidaYPersonas() {
        assertThatThrownBy(() -> precios.precioDeLista(full, null, 100, null))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("comida");
        assertThatThrownBy(() -> precios.precioDeLista(full, 10L, null, null))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("número de personas");
    }

    @Test
    void combinacionInexistenteDaMensajeClaro() {
        assertThatThrownBy(() -> precios.precioDeLista(full, 11L, 80, null))
                .isInstanceOf(CotizacionException.class)
                .hasMessageContaining("\"Taquiza\"").hasMessageContaining("80 personas");
        assertThatThrownBy(() -> precios.precioDeLista(full, 99L, 80, null))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("esa comida");
    }

    @Test
    void paqueteInactivoNoSeOfreceParaSeleccionesNuevas() {
        renta.setActivo(false);
        assertThatThrownBy(() -> precios.precioDeLista(renta, null, null, null))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("ya no se ofrece");
    }

    @Test
    void redondeaACentavos() {
        Paquete raro = paqueteFijo(3, "Raro", 100.005);
        assertThat(precios.precioDeLista(raro, null, null, null)).isEqualTo(100.01);
    }

    @Test
    void mismaSeleccionRespetaElPrecioPactadoAunqueCambieLaTabla() {
        Cotizacion cot = cotizacion(50, full, barbacoa, 100, 23800.0);
        full.getPrecios().get(1).setPrecio(25000); // subió el precio después de cotizar
        when(cotizacionRepository.findById(50L)).thenReturn(Optional.of(cot));

        PrecioPaquete p = precios.calcularParaCotizacion(50L, new SolicitudPrecioPaquete(2L, 10L, 100, null));

        assertThat(p.precio()).isEqualTo(23800);
        assertThat(p.pactado()).isTrue();
    }

    @Test
    void mismaSeleccionEnPaqueteDadoDeBajaSigueConSuPrecio() {
        Cotizacion cot = cotizacion(51, renta, null, null, 8800.0);
        renta.setActivo(false);

        assertThat(precios.precioParaCotizacion(cot, renta, null, null, null)).isEqualTo(8800);
    }

    @Test
    void cambiarPersonasVuelveACotizar() {
        Cotizacion cot = cotizacion(52, full, barbacoa, 100, 23800.0);
        when(cotizacionRepository.findById(52L)).thenReturn(Optional.of(cot));
        when(paqueteRepository.findById(2L)).thenReturn(Optional.of(full));

        PrecioPaquete p = precios.calcularParaCotizacion(52L, new SolicitudPrecioPaquete(2L, 10L, 80, null));

        assertThat(p.precio()).isEqualTo(22600);
        assertThat(p.pactado()).isFalse();
    }

    @Test
    void cotizacionAntiguaSinPrecioPactadoLoDeduceDelTotal() {
        Cotizacion cot = cotizacion(53, renta, null, null, null);
        cot.setTotal(10700);
        extra(cot, servicio(20, "Piñata", 400, CategoriaServicio.EXTRA), 1, 400);
        extra(cot, servicio(21, "Mesero(a)", 500, CategoriaServicio.EXTRA), 1, 500);

        assertThat(CalcularPrecioPaqueteUseCase.precioPactado(cot)).isEqualTo(9800);
        assertThat(precios.precioParaCotizacion(cot, renta, null, null, null)).isEqualTo(9800);
    }

    @Test
    void sinCotizacionEsComoCalcular() {
        when(paqueteRepository.findById(1L)).thenReturn(Optional.of(renta));

        assertThat(precios.calcularParaCotizacion(null, new SolicitudPrecioPaquete(1L, null, null, null)).precio())
                .isEqualTo(9800);
    }
}
