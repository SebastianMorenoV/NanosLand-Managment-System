package com.example.negocio.cotizacion.usecase;

import com.mycompany.common.dtos.CotizacionDTO;
import com.mycompany.common.mapper.CotizacionMapper;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.repository.CotizacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListarCotizacionesUseCase {

    private final CotizacionRepository cotizacionRepository;

    @Transactional(readOnly = true)
    public List<CotizacionDTO> obtenerCotizaciones() {
        List<Cotizacion> todas = cotizacionRepository.findAll();
        return todas.stream()
            .filter(c -> c.getEstado() != EstadoCotizacion.ELIMINADA)
            .map(CotizacionMapper::toDTO)
            .collect(Collectors.toList());
    }
}

