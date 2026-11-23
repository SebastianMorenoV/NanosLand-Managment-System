package com.mycompany.common.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class IngresoDTO {
    private String folioPago;
    private LocalDateTime fechaHora;
    private String tipo;
    private double cantidad;
    private String clienteNombre;
    private String folioCotizacion;
}
