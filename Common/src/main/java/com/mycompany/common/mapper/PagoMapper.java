package com.mycompany.common.mapper;

import com.mycompany.common.dtos.PagoDTO;
import com.mycompany.persistencia.dominio.Pago;

import java.util.List;
import java.util.stream.Collectors;

public class PagoMapper {

    public static PagoDTO toDTO(Pago entity) {
        if (entity == null) {
            return null;
        }
        PagoDTO dto = new PagoDTO();
        dto.setId(entity.getId());
        dto.setCantidad(entity.getCantidad());
        dto.setFechaHora(entity.getFechaHora());
        if (entity.getTipo() != null) {
            dto.setTipo(entity.getTipo().name());
        }
        if (entity.getCotizacion() != null) {
            dto.setCotizacionId(entity.getCotizacion().getId());
        }
        dto.setFolioPago(entity.getFolioPago());
        return dto;
    }

    public static List<PagoDTO> toDTOList(List<Pago> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream().map(PagoMapper::toDTO).collect(Collectors.toList());
    }
}
