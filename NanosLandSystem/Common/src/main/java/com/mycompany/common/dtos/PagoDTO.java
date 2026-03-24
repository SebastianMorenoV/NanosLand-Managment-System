package com.mycompany.common.dtos;

import com.mycompany.persistencia.enums.MetodoPago;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PagoDTO {
    private Long id;
    private double cantidad;
    private LocalDateTime fechaHora;
    private MetodoPago tipo;
    private String folioPago;    // Folio de autorización, referencia o código
    private Long cotizacionId;
}