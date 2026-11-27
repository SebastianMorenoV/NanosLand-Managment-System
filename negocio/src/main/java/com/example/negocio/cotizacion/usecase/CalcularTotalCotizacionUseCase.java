package com.example.negocio.cotizacion.usecase;

import com.example.negocio.precio.Dinero;
import com.mycompany.common.dtos.DetalleCotizacionDTO;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Total estimado que muestra la pantalla de cotización mientras se captura.
 * El total que se guarda lo calculan Crear/ModificarCotizacionUseCase con las mismas reglas.
 */
@Service
public class CalcularTotalCotizacionUseCase {

    /**
     * @param precioPaquete   precio del paquete según CalcularPrecioPaqueteUseCase (0 si aún no se elige)
     * @param serviciosExtras renglones de servicios extra, cada uno con su precio unitario y cantidad
     */
    public double ejecutar(double precioPaquete, List<DetalleCotizacionDTO> serviciosExtras) {
        return Dinero.aCentavos(precioPaquete + ExtrasCotizacion.subtotal(serviciosExtras));
    }
}
