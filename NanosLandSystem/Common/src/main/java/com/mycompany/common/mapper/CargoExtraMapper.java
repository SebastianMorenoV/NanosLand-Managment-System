package com.mycompany.common.mapper;

import com.mycompany.common.dtos.CargoExtraDTO;
import com.mycompany.persistencia.dominio.CargoExtra;

import java.util.List;
import java.util.stream.Collectors;

public class CargoExtraMapper {

    public static CargoExtraDTO toDTO(CargoExtra entity) {
        if (entity == null) {
            return null;
        }
        CargoExtraDTO dto = new CargoExtraDTO();
        dto.setId(entity.getId());
        if (entity.getEvento() != null) {
            dto.setEventoId(entity.getEvento().getId());
        }
        if (entity.getServicio() != null) {
            dto.setServicioId(entity.getServicio().getId());
        }
        dto.setDescripcion(entity.getDescripcion());
        dto.setCantidad(entity.getCantidad());
        dto.setPrecioUnitario(entity.getPrecioUnitario());
        dto.setSubtotal(entity.getSubtotal());
        dto.setFechaHoraCargo(entity.getFechaHoraCargo());
        return dto;
    }

    public static List<CargoExtraDTO> toDTOList(List<CargoExtra> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream().map(CargoExtraMapper::toDTO).collect(Collectors.toList());
    }
}
