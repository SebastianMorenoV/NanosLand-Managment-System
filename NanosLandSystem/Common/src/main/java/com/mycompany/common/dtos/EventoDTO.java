package com.mycompany.common.dtos;

import com.mycompany.persistencia.enums.TurnoEvento;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EventoDTO {
    private Long id;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private TurnoEvento turno;
    private String notas;
    private Long cotizacionId;
    private String folioCotizacion;
    private String nombreFestejado;
}