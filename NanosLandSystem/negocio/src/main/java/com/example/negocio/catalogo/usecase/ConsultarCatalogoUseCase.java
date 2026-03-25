/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.negocio.catalogo.usecase;

import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.dtos.ServicioDTO;
import com.mycompany.common.mapper.PaqueteMapper;
import com.mycompany.common.mapper.ServicioMapper;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.repository.PaqueteRepository;
import com.mycompany.persistencia.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
        return paqueteRepository.findAll().stream()
                .map(PaqueteMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<Servicio> obtenerServicios() {
        return servicioRepository.findAll();
    }

    public List<ServicioDTO> obtenerTodosLosServicios() {
        return servicioRepository.findAll().stream()
                // AQUÍ ESTÁ LA MAGIA: Usamos 'ServicioMapper' con S mayúscula
                .map(ServicioMapper::toDTO)
                .collect(Collectors.toList());
    }

    public ServicioDTO guardarNuevoServicio(String nombre, Double precio, String descripcion) {
        com.mycompany.persistencia.dominio.Servicio entidad = new com.mycompany.persistencia.dominio.Servicio();
        entidad.setNombre(nombre);
        entidad.setPrecio(precio);

        // Guardamos la descripción que puso el usuario (o "Sin descripción" si la dejó en blanco)
        entidad.setDescripcion(descripcion != null && !descripcion.isBlank() ? descripcion : "Sin descripción");

        // Guardamos en MySQL
        entidad = servicioRepository.save(entidad);

        // Retornamos el DTO para que la interfaz lo pueda usar
        return ServicioMapper.toDTO(entidad);
    }

    public PaqueteDTO guardarNuevoPaquete(String nombre, Double costoBase, String descripcion) {
        Paquete entidad = new Paquete();
        entidad.setNombre(nombre);
        entidad.setCosto(costoBase);
        entidad.setDescripcion(descripcion != null && !descripcion.isBlank() ? descripcion : "Sin descripción");
        entidad = paqueteRepository.save(entidad);
        return PaqueteMapper.toDTO(entidad);
    }
}
