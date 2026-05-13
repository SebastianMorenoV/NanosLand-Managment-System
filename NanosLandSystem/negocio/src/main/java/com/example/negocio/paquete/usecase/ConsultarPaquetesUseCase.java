package com.example.negocio.paquete.usecase;

import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.mapper.PaqueteMapper;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.repository.PaqueteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConsultarPaquetesUseCase {

    @Autowired
    private PaqueteRepository paqueteRepository;

    public List<PaqueteDTO> obtenerPaquetesActivos() {
        List<Paquete> paquetes = paqueteRepository.findByActivoTrue();
        return PaqueteMapper.toDTOList(paquetes);
    }
    
    public Optional<PaqueteDTO> obtenerPaquetePorId(Long id) {
        return paqueteRepository.findById(id).map(PaqueteMapper::toDTO);
    }
}
