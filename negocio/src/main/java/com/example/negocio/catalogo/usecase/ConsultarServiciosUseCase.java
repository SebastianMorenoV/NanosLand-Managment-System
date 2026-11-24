package com.example.negocio.catalogo.usecase;

import com.mycompany.common.dtos.ServicioDTO;
import com.mycompany.common.mapper.ServicioMapper;
import com.mycompany.persistencia.enums.CategoriaServicio;
import com.mycompany.persistencia.repository.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConsultarServiciosUseCase {

    @Autowired
    private ServicioRepository servicioRepository;

    public List<ServicioDTO> obtenerTodos() {
        return servicioRepository.findByActivoTrue().stream()
                .map(ServicioMapper::toDTO)
                .collect(Collectors.toList());
    }

    /** Servicios que pueden ir incluidos en un paquete (extras e incluidos; las comidas van en la tabla de precios). */
    @Transactional(readOnly = true)
    public List<ServicioDTO> obtenerParaComposicion() {
        return servicioRepository.findByActivoTrueAndCategoriaInOrderByNombreAsc(
                        EnumSet.of(CategoriaServicio.EXTRA, CategoriaServicio.INCLUIDO)).stream()
                .map(ServicioMapper::toDTO)
                .collect(Collectors.toList());
    }

    /** Comidas activas, para las tablas de precios de los paquetes. */
    @Transactional(readOnly = true)
    public List<ServicioDTO> obtenerComidas() {
        return servicioRepository.findByActivoTrueAndCategoriaOrderByNombreAsc(CategoriaServicio.COMIDA).stream()
                .map(ServicioMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<ServicioDTO> obtenerPorId(Long id) {
        return servicioRepository.findById(id).map(ServicioMapper::toDTO);
    }
}
