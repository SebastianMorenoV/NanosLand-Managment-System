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
        if (cotizacion.getEvento() != null) {
            dto.setFecha(cotizacion.getEvento().getFecha());
            dto.setTurno(cotizacion.getEvento().getTurno());
            dto.setNombreFestejado(cotizacion.getEvento().getNombreFestejado());
            dto.setTematica(cotizacion.getEvento().getTematica());
        }
        if (cotizacion.getCliente() != null) {
            dto.setClienteId(cotizacion.getCliente().getId());
            dto.setNombreCliente(cotizacion.getCliente().getNombre());
        }
        dto.setTotal(cotizacion.getTotal());
        dto.setNotas(cotizacion.getNotas());
        dto.setEstado(cotizacion.getEstado());
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
        cotizacion.setTotal(dto.getTotal());
        cotizacion.setNotas(dto.getNotas());
        cotizacion.setEstado(dto.getEstado());
        return cotizacion;
    }
}