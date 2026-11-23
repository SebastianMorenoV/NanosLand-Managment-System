/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.common.mapper;


import com.mycompany.common.dtos.DetalleCotizacionDTO;
import com.mycompany.persistencia.dominio.DetalleCotizacion;

public class DetalleCotizacionMapper {

    public static DetalleCotizacionDTO toDTO(DetalleCotizacion detalle) {
        if (detalle == null) return null;
        DetalleCotizacionDTO dto = new DetalleCotizacionDTO();
        dto.setId(detalle.getId());
        dto.setServicioId(detalle.getServicio().getId());
        dto.setNombreServicio(detalle.getServicio().getNombre());
        dto.setPrecioUnitario(detalle.getPrecioUnitario());
        dto.setCantidad(detalle.getCantidad());
        dto.setSubtotal(detalle.getSubtotal());
        dto.setHoraSugerida(detalle.getHoraSugerida());
        dto.setEspecificacionesCliente(detalle.getEspecificacionesCliente());
        dto.setDesgloseOpciones(detalle.getDesgloseOpciones());
        dto.setUbicacionMontaje(detalle.getUbicacionMontaje());
        return dto;
    }

    public static DetalleCotizacion toEntity(DetalleCotizacionDTO dto) {
        if (dto == null) return null;
        DetalleCotizacion detalle = new DetalleCotizacion();
        detalle.setId(dto.getId());
        detalle.setPrecioUnitario(dto.getPrecioUnitario());
        detalle.setCantidad(dto.getCantidad());
        detalle.setSubtotal(dto.getSubtotal());
        detalle.setHoraSugerida(dto.getHoraSugerida());
        detalle.setEspecificacionesCliente(dto.getEspecificacionesCliente());
        detalle.setDesgloseOpciones(dto.getDesgloseOpciones());
        detalle.setUbicacionMontaje(dto.getUbicacionMontaje());
        return detalle;
    }
}
