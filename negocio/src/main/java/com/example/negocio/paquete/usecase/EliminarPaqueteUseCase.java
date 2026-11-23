package com.example.negocio.paquete.usecase;

import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.repository.PaqueteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;
import com.example.negocio.exception.CotizacionException;

@Service
@Transactional
public class EliminarPaqueteUseCase {

    @Autowired
    private PaqueteRepository paqueteRepository;

    @Autowired
    private com.mycompany.persistencia.repository.CotizacionRepository cotizacionRepository;

    public void eliminarPaqueteLogico(Long id) {
        Optional<Paquete> opt = paqueteRepository.findById(id);
        if (opt.isEmpty() || !opt.get().isActivo()) {
            throw new CotizacionException("El paquete no existe o ya está eliminado.");
        }

        boolean enUso = cotizacionRepository.existsByPaqueteIdAndEstadoIn(id, 
            java.util.List.of(com.mycompany.persistencia.enums.EstadoCotizacion.BORRADOR, 
                              com.mycompany.persistencia.enums.EstadoCotizacion.VIGENTE));
        
        if (enUso) {
            throw new CotizacionException("No se puede eliminar el paquete porque tiene cotizaciones activas o vigentes asociadas.");
        }

        Paquete p = opt.get();
        p.setActivo(false);
        p.getServicios().clear(); // Desvincula los servicios asociados
        paqueteRepository.save(p);
    }
}
