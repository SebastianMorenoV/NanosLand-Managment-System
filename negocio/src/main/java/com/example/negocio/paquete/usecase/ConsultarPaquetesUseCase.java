package com.example.negocio.paquete.usecase;

import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.mapper.PaqueteMapper;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.PaqueteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultarPaquetesUseCase {

    private final PaqueteRepository paqueteRepository;
    private final CotizacionRepository cotizacionRepository;

    public List<PaqueteDTO> obtenerPaquetesActivos() {
        List<Paquete> paquetes = paqueteRepository.findByActivoTrue();
        return PaqueteMapper.toDTOList(paquetes);
    }

    public Optional<PaqueteDTO> obtenerPaquetePorId(Long id) {
        return paqueteRepository.findById(id).map(PaqueteMapper::toDTO);
    }

    /**
     * Cotizaciones abiertas del paquete que todavía no guardan su copia del paquete. Mientras haya,
     * sus servicios incluidos no se pueden cambiar (los precios sí).
     */
    public List<String> cotizacionesAbiertasSinCopia(Long paqueteId) {
        return paqueteId == null ? List.of() : cotizacionRepository.foliosAbiertosSinCopiaDelPaquete(
            paqueteId, ActualizarPaqueteUseCase.ESTADOS_ABIERTOS, EstadoEvento.FINALIZADO);
    }

    /** Cuántas cotizaciones abiertas tiene el paquete: conservan lo que se les vendió aunque el paquete cambie. */
    public long contarCotizacionesAbiertas(Long paqueteId) {
        return paqueteId == null ? 0 : cotizacionRepository.contarAbiertasDePaquete(
            paqueteId, ActualizarPaqueteUseCase.ESTADOS_ABIERTOS, EstadoEvento.FINALIZADO);
    }
}
