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
        dto.setNotas(evento.getNotas());
        if (evento.getCotizacion() != null) {
            dto.setCotizacionId(evento.getCotizacion().getId());
            dto.setFolioCotizacion(evento.getCotizacion().getFolio());
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
        evento.setNotas(dto.getNotas());
        return evento;
    }
}