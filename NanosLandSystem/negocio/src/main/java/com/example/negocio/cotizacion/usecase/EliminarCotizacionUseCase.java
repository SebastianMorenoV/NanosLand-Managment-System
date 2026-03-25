package com.example.negocio.cotizacion.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EliminarCotizacionUseCase {

    private final CotizacionRepository cotizacionRepository;
    private final EventoRepository eventoRepository;

    @Transactional
    public void eliminarCotizacion(Long cotizacionId) {
        if (cotizacionId == null) {
            throw new CotizacionException("Debe indicar la cotización a eliminar.");
        }

        Cotizacion existente = cotizacionRepository.findById(cotizacionId)
            .orElseThrow(() -> new CotizacionException("No existe la cotización indicada."));

        // 1. Ocultamos/Cancelamos la cotización lógicamente
        existente.setEstado(EstadoCotizacion.ELIMINADA);
        cotizacionRepository.save(existente);

        // 2. Buscamos y ELIMINAMOS físicamente el evento para liberar el calendario
        Optional<Evento> eventoAsociado = eventoRepository.findByCotizacionId(cotizacionId);
        eventoAsociado.ifPresent(eventoRepository::delete);
    }
}