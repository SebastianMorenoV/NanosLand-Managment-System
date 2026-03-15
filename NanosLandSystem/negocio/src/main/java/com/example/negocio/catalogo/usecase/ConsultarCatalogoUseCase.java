/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.negocio.catalogo.usecase;

import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.mapper.PaqueteMapper;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.repository.PaqueteRepository;
import com.mycompany.persistencia.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
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
        // 1. Buscamos en la base de datos
        List<Paquete> paquetesDb = paqueteRepository.findAll();

        // 2. Mapeamos a DTO AQUÍ ADENTRO.
        // Como estamos dentro de @Transactional, la sesión sigue viva.
        // Cuando PaqueteMapper llame a ServicioMapper.toDTO(), funcionará perfecto.
        return paquetesDb.stream()
                .map(PaqueteMapper::toDTO)
                .toList();
    }

    public List<Servicio> obtenerServicios() {
        return servicioRepository.findAll();
    }
}
