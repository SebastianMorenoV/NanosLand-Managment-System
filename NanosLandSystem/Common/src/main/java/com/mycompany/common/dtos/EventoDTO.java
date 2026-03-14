/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.common.dtos;

import com.mycompany.persistencia.enums.TurnoEvento;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author skyro
 */
@Data
@NoArgsConstructor
public class EventoDTO {
    private Long id;
    private LocalDateTime fechaHoraInicio;
    private TurnoEvento turno;
    private String notas;
    private Long cotizacionId;
    private String folioCotizacion;
}
