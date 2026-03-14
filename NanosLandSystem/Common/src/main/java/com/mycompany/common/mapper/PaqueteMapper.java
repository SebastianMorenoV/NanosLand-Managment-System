/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.common.mapper;


import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.common.dtos.PaqueteDTO;
import java.util.stream.Collectors;

public class PaqueteMapper {

    public static PaqueteDTO toDTO(Paquete paquete) {
        if (paquete == null) return null;
        PaqueteDTO dto = new PaqueteDTO();
        dto.setId(paquete.getId());
        dto.setNombre(paquete.getNombre());
        dto.setDescripcion(paquete.getDescripcion());
        dto.setCostoBase(paquete.getCostoBase());
        if (paquete.getServicios() != null) {
            dto.setServicios(paquete.getServicios().stream()
                .map(ps -> ServicioMapper.toDTO(ps.getServicio()))
                .collect(Collectors.toList()));
        }
        return dto;
    }

    public static Paquete toEntity(PaqueteDTO dto) {
        if (dto == null) return null;
        Paquete paquete = new Paquete();
        paquete.setId(dto.getId());
        paquete.setNombre(dto.getNombre());
        paquete.setDescripcion(dto.getDescripcion());
        paquete.setCostoBase(dto.getCostoBase());
        return paquete;
    }
}