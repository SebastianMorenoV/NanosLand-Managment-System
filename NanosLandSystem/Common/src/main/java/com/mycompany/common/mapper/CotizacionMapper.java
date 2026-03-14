/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.common.mapper;


import com.mycompany.common.dtos.CotizacionDTO;
import com.mycompany.persistencia.dominio.Cotizacion;
import java.util.stream.Collectors;

public class CotizacionMapper {

    public static CotizacionDTO toDTO(Cotizacion cotizacion) {
        if (cotizacion == null) return null;
        CotizacionDTO dto = new CotizacionDTO();
        dto.setId(cotizacion.getId());
        dto.setFolio(cotizacion.getFolio());
        dto.setFecha(cotizacion.getFecha());
        dto.setClienteId(cotizacion.getCliente().getId());
        dto.setNombreCliente(cotizacion.getCliente().getNombre());
        dto.setNombreFestejado(cotizacion.getNombreFestejado());
        dto.setTotal(cotizacion.getTotal());
        dto.setNotas(cotizacion.getNotas());
        dto.setEstado(cotizacion.getEstado());
        dto.setTematica(cotizacion.getTematica());
        if (cotizacion.getPaquete() != null) {
            dto.setPaqueteId(cotizacion.getPaquete().getId());
            dto.setNombrePaquete(cotizacion.getPaquete().getNombre());
        }
        if (cotizacion.getServiciosExtra() != null) {
            dto.setDetalles(cotizacion.getServiciosExtra().stream()
                .map(DetalleCotizacionMapper::toDTO)
                .collect(Collectors.toList()));
        }
        return dto;
    }

    public static Cotizacion toEntity(CotizacionDTO dto) {
        if (dto == null) return null;
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setId(dto.getId());
        cotizacion.setFolio(dto.getFolio());
        cotizacion.setFecha(dto.getFecha());
        cotizacion.setNombreFestejado(dto.getNombreFestejado());
        cotizacion.setTotal(dto.getTotal());
        cotizacion.setNotas(dto.getNotas());
        cotizacion.setEstado(dto.getEstado());
        cotizacion.setTematica(dto.getTematica());
        return cotizacion;
    }
}