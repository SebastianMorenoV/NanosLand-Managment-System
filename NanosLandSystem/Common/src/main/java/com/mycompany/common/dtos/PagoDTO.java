package com.mycompany.common.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class PagoDTO {
    private Long id;
    private double cantidad;
    private LocalDateTime fechaHora;
    private String tipo; // MetodoPago enum name
    private Long cotizacionId;
    private String folioPago;
}
