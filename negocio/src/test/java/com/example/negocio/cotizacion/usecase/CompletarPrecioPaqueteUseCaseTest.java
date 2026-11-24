package com.example.negocio.cotizacion.usecase;

import static com.example.negocio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.enums.CategoriaServicio;
import com.mycompany.persistencia.repository.CotizacionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompletarPrecioPaqueteUseCaseTest {

    @Mock CotizacionRepository cotizacionRepository;

    @Test
    void llenaElPrecioPactadoConElTotalMenosLosExtras() {
        Paquete renta = paqueteFijo(1, "Renta de local", 12000);
        Cotizacion vieja = cotizacion(1, renta, null, null, null);
        vieja.setTotal(10700);
        extra(vieja, servicio(20, "Piñata", 400, CategoriaServicio.EXTRA), 1, 400);
        extra(vieja, servicio(21, "Mesero(a)", 500, CategoriaServicio.EXTRA), 1, 500);
        Cotizacion sinExtras = cotizacion(2, renta, null, null, null);
        sinExtras.setTotal(8800);
        when(cotizacionRepository.findByPrecioPaqueteIsNull()).thenReturn(List.of(vieja, sinExtras));

        int completadas = new CompletarPrecioPaqueteUseCase(cotizacionRepository).completar();

        assertThat(completadas).isEqualTo(2);
        assertThat(vieja.getPrecioPaquete()).isEqualTo(9800);
        assertThat(sinExtras.getPrecioPaquete()).isEqualTo(8800);
    }
}
