package com.example.negocio.cotizacion.usecase;

import com.example.negocio.precio.CalcularPrecioPaqueteUseCase;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.repository.CotizacionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Corrección al arrancar: las cotizaciones de antes del precio pactado no lo tienen
 * guardado. Se les asigna el total menos los extras, que es lo que siempre mostró el
 * estado de cuenta como precio del paquete. Solo toca las que lo tienen vacío, así que
 * se puede ejecutar cada vez que abre la aplicación.
 */
@Service
@RequiredArgsConstructor
public class CompletarPrecioPaqueteUseCase {

    private final CotizacionRepository cotizacionRepository;

    /** @return cuántas cotizaciones se completaron */
    @Transactional
    public int completar() {
        List<Cotizacion> pendientes = cotizacionRepository.findByPrecioPaqueteIsNull();
        for (Cotizacion cotizacion : pendientes) {
            cotizacion.setPrecioPaquete(CalcularPrecioPaqueteUseCase.precioPactado(cotizacion));
        }
        return pendientes.size();
    }
}
