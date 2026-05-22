/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.common.mapper;

import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.common.dtos.EventoDTO;

/**
 *
 * @author josee
 */
public class EventoMapper {

    public static EventoDTO toDTO(Evento evento) {
        if (evento == null) return null;
        EventoDTO dto = new EventoDTO();
        dto.setId(evento.getId());
        dto.setFecha(evento.getFecha());
        dto.setHoraInicio(evento.getHoraInicio());
        dto.setHoraFin(evento.getHoraFin());
        dto.setTurno(evento.getTurno());
        dto.setNombreFestejado(evento.getNombreFestejado());
        dto.setTematica(evento.getTematica());
        dto.setNotas(evento.getNotas());
        if (evento.getCotizacion() != null) {
            dto.setCotizacionId(evento.getCotizacion().getId());
            dto.setFolioCotizacion(evento.getCotizacion().getFolio());
            dto.setClienteNombre(evento.getCotizacion().getCliente() != null ? evento.getCotizacion().getCliente().getNombre() : "Desconocido");
            dto.setPaqueteNombre(evento.getCotizacion().getPaquete() != null ? evento.getCotizacion().getPaquete().getNombre() : "Personalizado");
            dto.setEstadoEvento(evento.getEstado());
            dto.setTotalCotizacion(evento.getCotizacion().getTotal());
        }
        if (evento.getCargosExtras() != null) {
            double totalExtras = evento.getCargosExtras().stream()
                .mapToDouble(com.mycompany.persistencia.dominio.CargoExtra::getSubtotal)
                .sum();
            dto.setTotalCargosExtras(totalExtras);
        } else {
            dto.setTotalCargosExtras(0.0);
        }
        return dto;
    }

    public static Evento toEntity(EventoDTO dto) {
        if (dto == null) return null;
        Evento evento = new Evento();
        evento.setId(dto.getId());
        evento.setFecha(dto.getFecha());
        evento.setHoraInicio(dto.getHoraInicio());
        evento.setHoraFin(dto.getHoraFin());
        evento.setTurno(dto.getTurno());
        evento.setNombreFestejado(dto.getNombreFestejado());
        evento.setTematica(dto.getTematica());
        evento.setNotas(dto.getNotas());
        return evento;
    }

    public static java.util.List<EventoDTO> toDTOList(java.util.List<Evento> eventos) {
        if (eventos == null) return null;
        return eventos.stream()
                .map(EventoMapper::toDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    public static java.util.List<Evento> toEntityList(java.util.List<EventoDTO> dtos) {
        if (dtos == null) return null;
        return dtos.stream()
                .map(EventoMapper::toEntity)
                .collect(java.util.stream.Collectors.toList());
    }
}