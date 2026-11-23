package com.mycompany.common.dtos;

import com.mycompany.persistencia.enums.EstadoLogistica;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * DTO para la bitácora de logística de servicios (CU-06).
 */
@Data
@NoArgsConstructor
public class LogisticaDTO {

    private Long id;
    private Long eventoId;
    private Long servicioId;
    private String nombreServicio;
    private String especificaciones;
    private String desgloseOpciones;
    private String ubicacionMontaje;
    private String responsableTurno;
    private LocalTime horaRequerida;
    private EstadoLogistica estado;
}
