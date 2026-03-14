/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.negocio.catalogo.usecase;

import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.repository.PaqueteRepository;
import com.mycompany.persistencia.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public List<Paquete> obtenerPaquetes() {
        return paqueteRepository.findAll();
    }

    public List<Servicio> obtenerServicios() {
        return servicioRepository.findAll();
    }
}
