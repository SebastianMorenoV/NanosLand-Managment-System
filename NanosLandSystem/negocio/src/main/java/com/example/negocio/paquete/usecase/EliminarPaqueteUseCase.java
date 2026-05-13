package com.example.negocio.paquete.usecase;

import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.repository.PaqueteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EliminarPaqueteUseCase {

    @Autowired
    private PaqueteRepository paqueteRepository;

    public void eliminarPaqueteLogico(Long id) {
        Optional<Paquete> opt = paqueteRepository.findById(id);
        if (opt.isEmpty() || !opt.get().isActivo()) {
            throw new IllegalArgumentException("El paquete no existe o ya está eliminado.");
        }

        Paquete p = opt.get();
        p.setActivo(false);
        p.getServicios().clear(); // Desvincula los servicios asociados
        paqueteRepository.save(p);
    }
}
