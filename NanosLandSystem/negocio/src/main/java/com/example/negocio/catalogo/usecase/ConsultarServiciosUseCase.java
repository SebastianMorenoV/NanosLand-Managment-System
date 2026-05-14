package com.example.negocio.catalogo.usecase;

import com.mycompany.common.dtos.ServicioDTO;
import com.mycompany.common.mapper.ServicioMapper;
import com.mycompany.persistencia.repository.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConsultarServiciosUseCase {

    @Autowired
    private ServicioRepository servicioRepository;

    public List<ServicioDTO> obtenerTodos() {
        return servicioRepository.findAll().stream()
                .map(ServicioMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<ServicioDTO> obtenerPorId(Long id) {
        return servicioRepository.findById(id).map(ServicioMapper::toDTO);
    }
}
