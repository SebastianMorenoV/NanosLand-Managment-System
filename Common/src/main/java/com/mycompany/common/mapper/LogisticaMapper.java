package com.mycompany.common.mapper;

import com.mycompany.common.dtos.LogisticaDTO;
import com.mycompany.persistencia.dominio.LogisticaServicio;

import java.util.List;
import java.util.stream.Collectors;

public class LogisticaMapper {

    public static LogisticaDTO toDTO(LogisticaServicio entidad) {
        if (entidad == null) return null;
        LogisticaDTO dto = new LogisticaDTO();
        dto.setId(entidad.getId());
        dto.setHoraRequerida(entidad.getHoraRequerida());
        dto.setEspecificaciones(entidad.getEspecificaciones());
        dto.setDesgloseOpciones(entidad.getDesgloseOpciones());
        dto.setUbicacionMontaje(entidad.getUbicacionMontaje());
        dto.setResponsableTurno(entidad.getResponsableTurno());
        dto.setEstado(entidad.getEstado());

        if (entidad.getEvento() != null) {
            dto.setEventoId(entidad.getEvento().getId());
        }
        if (entidad.getServicio() != null) {
            dto.setServicioId(entidad.getServicio().getId());
            dto.setNombreServicio(entidad.getServicio().getNombre());
        }
        return dto;
    }

    public static List<LogisticaDTO> toDTOList(List<LogisticaServicio> entidades) {
        if (entidades == null) return null;
        return entidades.stream()
                .map(LogisticaMapper::toDTO)
                .collect(Collectors.toList());
    }
}
