package com.mycompany.common.mapper;

import com.mycompany.common.dtos.PagoDTO;
import com.mycompany.persistencia.dominio.Pago;
import com.mycompany.persistencia.dominio.Cotizacion;

import java.util.List;
import java.util.stream.Collectors;

public class PagoMapper {

    public static PagoDTO toDTO(Pago pago) {
        if (pago == null) return null;
        
        PagoDTO dto = new PagoDTO();
        dto.setId(pago.getId());
        dto.setCantidad(pago.getCantidad());
        dto.setFechaHora(pago.getFechaHora());
        dto.setTipo(pago.getTipo());
        dto.setFolioPago(pago.getFolioPago());
        
        if (pago.getCotizacion() != null) {
            dto.setCotizacionId(pago.getCotizacion().getId());
        }
        
        return dto;
    }

    public static Pago toEntity(PagoDTO dto) {
        if (dto == null) return null;
        
        Pago pago = new Pago();
        pago.setId(dto.getId());
        pago.setCantidad(dto.getCantidad());
        pago.setFechaHora(dto.getFechaHora());
        pago.setTipo(dto.getTipo());
        pago.setFolioPago(dto.getFolioPago());
        
        if (dto.getCotizacionId() != null) {
            Cotizacion cotizacion = new Cotizacion();
            cotizacion.setId(dto.getCotizacionId());
            pago.setCotizacion(cotizacion);
        }
        
        return pago;
    }

    public static List<PagoDTO> toDTOList(List<Pago> pagos) {
        if (pagos == null) return null;
        return pagos.stream()
                .map(PagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    public static List<Pago> toEntityList(List<PagoDTO> dtos) {
        if (dtos == null) return null;
        return dtos.stream()
                .map(PagoMapper::toEntity)
                .collect(Collectors.toList());
    }
}