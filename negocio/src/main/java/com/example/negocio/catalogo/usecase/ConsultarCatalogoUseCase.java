/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.negocio.catalogo.usecase;

import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.dtos.ServicioDTO;
import com.mycompany.common.mapper.PaqueteMapper;
import com.mycompany.common.mapper.ServicioMapper;
import com.mycompany.persistencia.enums.CategoriaServicio;
import com.mycompany.persistencia.repository.PaqueteRepository;
import com.mycompany.persistencia.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Catálogo que usa la pantalla de cotización: paquetes activos y servicios extra.
 *
 * @author skyro
 */
@Service
@RequiredArgsConstructor
public class ConsultarCatalogoUseCase {

    private final PaqueteRepository paqueteRepository;
    private final ServicioRepository servicioRepository;

    @Transactional(readOnly = true)
    public List<PaqueteDTO> obtenerTodosLosPaquetes() {
        return paqueteRepository.findByActivoTrue().stream()
                .map(PaqueteMapper::toDTO)
                .collect(Collectors.toList());
    }

    /** Incluye paquetes dados de baja, para mostrar cotizaciones que los usan. */
    @Transactional(readOnly = true)
    public PaqueteDTO obtenerPaquetePorId(Long id) {
        return paqueteRepository.findById(id)
                .map(PaqueteMapper::toDTO)
                .orElse(null);
    }

    /** Servicios que se pueden agregar como extra a una cotización. */
    @Transactional(readOnly = true)
    public List<ServicioDTO> obtenerServiciosExtra() {
        return servicioRepository.findByActivoTrueAndCategoriaOrderByNombreAsc(CategoriaServicio.EXTRA).stream()
                .map(ServicioMapper::toDTO)
                .collect(Collectors.toList());
    }
}
