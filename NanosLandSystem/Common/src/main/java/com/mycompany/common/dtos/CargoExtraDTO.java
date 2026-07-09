package com.mycompany.common.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class CargoExtraDTO {
    private Long id;
    private Long eventoId;
    private Long servicioId; // opcional
    private String descripcion;
    private int cantidad;
    private double precioUnitario;
    private double subtotal;
    private LocalDateTime fechaHoraCargo;
}
