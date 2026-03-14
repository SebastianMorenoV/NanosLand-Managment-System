/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.common.mapper;

import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.common.dtos.ServicioDTO;

public class ServicioMapper {

    public static ServicioDTO toDTO(Servicio servicio) {
        if (servicio == null) return null;
        ServicioDTO dto = new ServicioDTO();
        dto.setId(servicio.getId());
        dto.setNombre(servicio.getNombre());
        dto.setDescripcion(servicio.getDescripcion());
        dto.setPrecio(servicio.getPrecio());
        return dto;
    }

    public static Servicio toEntity(ServicioDTO dto) {
        if (dto == null) return null;
        Servicio servicio = new Servicio();
        servicio.setId(dto.getId());
        servicio.setNombre(dto.getNombre());
        servicio.setDescripcion(dto.getDescripcion());
        servicio.setPrecio(dto.getPrecio());
        return servicio;
    }
}