/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.common.mapper;



import com.mycompany.common.dtos.PaquetePrecioDTO;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.persistencia.dominio.PaquetePrecio;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PaqueteMapper {

    public static PaqueteDTO toDTO(Paquete paquete) {
        if (paquete == null) return null;
        PaqueteDTO dto = new PaqueteDTO();
        dto.setId(paquete.getId());
        dto.setNombre(paquete.getNombre());
        dto.setDescripcion(paquete.getDescripcion());
        dto.setCostoBase(paquete.getCosto());
        dto.setActivo(paquete.isActivo());
        if (paquete.getServicios() != null) {
            dto.setServicios(paquete.getServicios().stream()
                .map(ps -> {
                    com.mycompany.common.dtos.PaqueteServicioDTO psDTO = new com.mycompany.common.dtos.PaqueteServicioDTO();
                    psDTO.setId(ps.getId());
                    psDTO.setCantidad(ps.getCantidad());
                    psDTO.setSubtotal(ps.getSubtotal());
                    psDTO.setServicio(ServicioMapper.toDTO(ps.getServicio()));
                    return psDTO;
                })
                .collect(Collectors.toList()));
        }
        dto.setPrecios(paquete.getPrecios() == null ? List.of() : paquete.getPrecios().stream()
                .map(PaqueteMapper::toDTO)
                .sorted(Comparator.comparing((PaquetePrecioDTO p) -> p.getComidaNombre(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparingInt(PaquetePrecioDTO::getPersonas))
                .collect(Collectors.toList()));
        return dto;
    }

    public static PaquetePrecioDTO toDTO(PaquetePrecio precio) {
        if (precio == null) return null;
        PaquetePrecioDTO dto = new PaquetePrecioDTO();
        dto.setId(precio.getId());
        if (precio.getComida() != null) {
            dto.setComidaId(precio.getComida().getId());
            dto.setComidaNombre(precio.getComida().getNombre());
        }
        dto.setPersonas(precio.getPersonas());
        dto.setPrecio(precio.getPrecio());
        return dto;
    }

    /** Solo los datos generales; los servicios y precios los arman los casos de uso. */
    public static Paquete toEntity(PaqueteDTO dto) {
        if (dto == null) return null;
        Paquete paquete = new Paquete();
        paquete.setId(dto.getId());
        paquete.setNombre(dto.getNombre());
        paquete.setDescripcion(dto.getDescripcion());
        paquete.setCosto(dto.getCostoBase());
        return paquete;
    }

    public static List<PaqueteDTO> toDTOList(List<Paquete> paquetes) {
        if (paquetes == null) return null;
        return paquetes.stream()
                .map(PaqueteMapper::toDTO)
                .collect(Collectors.toList());
    }

    public static List<Paquete> toEntityList(List<PaqueteDTO> dtos) {
        if (dtos == null) return null;
        return dtos.stream()
                .map(PaqueteMapper::toEntity)
                .collect(Collectors.toList());
    }
}
