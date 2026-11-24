package com.example.negocio.cotizacion.usecase;

import com.example.negocio.precio.Dinero;
import com.mycompany.common.dtos.ServicioDTO;
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
     * @param serviciosExtras un elemento por unidad de servicio extra
     */
    public double ejecutar(double precioPaquete, List<ServicioDTO> serviciosExtras) {
        double total = precioPaquete;
        if (serviciosExtras != null) {
            for (ServicioDTO servicio : serviciosExtras) {
                total += servicio.getPrecio();
            }
        }
        return Dinero.aCentavos(total);
    }
}
