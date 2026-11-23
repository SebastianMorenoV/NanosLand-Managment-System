package com.example.negocio.cotizacion.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EliminarCotizacionUseCase {

    private final CotizacionRepository cotizacionRepository;
    private final PagoRepository pagoRepository;

    @Transactional
    public void eliminarCotizacion(Long cotizacionId) {
        if (cotizacionId == null) {
            throw new CotizacionException("Debe indicar la cotización a eliminar.");
        }

        Cotizacion existente = cotizacionRepository.findById(cotizacionId)
            .orElseThrow(() -> new CotizacionException("No existe la cotización indicada."));

        // Solo se elimina un borrador sin pagos: una cotización con dinero
        // recibido no debe desaparecer junto con su evento.
        if (existente.getEstado() != EstadoCotizacion.BORRADOR) {
            throw new CotizacionException("Solo se pueden eliminar cotizaciones en borrador.");
        }
        if (!pagoRepository.findByCotizacionId(cotizacionId).isEmpty()) {
            throw new CotizacionException(
                "La cotización tiene pagos registrados y no se puede eliminar.");
        }

        // 1. Ocultamos la cotización lógicamente
        existente.setEstado(EstadoCotizacion.ELIMINADA);

        // 2. Eliminamos físicamente el evento para liberar el calendario.
        //    Se desasocia de la cotización para que actúe orphanRemoval: borrar el
        //    evento con el repositorio mientras la cotización lo sigue referenciando
        //    hace que Hibernate cancele el borrado y el turno quede ocupado.
        existente.setEvento(null);
        cotizacionRepository.save(existente);
    }
}
