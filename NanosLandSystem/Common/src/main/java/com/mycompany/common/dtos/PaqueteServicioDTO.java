package com.mycompany.common.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaqueteServicioDTO {
    private Long id; // ID del registro en la tabla intermedia (opcional)
    private ServicioDTO servicio;
    private int cantidad;
    private double subtotal;
}
