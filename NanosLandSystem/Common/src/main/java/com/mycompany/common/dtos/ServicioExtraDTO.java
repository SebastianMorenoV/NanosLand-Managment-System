package com.mycompany.common.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ServicioExtraDTO {
    private String nombre;
    private double precioUnitario;
    private int cantidad;
    private double subtotal;
}
