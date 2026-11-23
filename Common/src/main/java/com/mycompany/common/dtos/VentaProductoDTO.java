package com.mycompany.common.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Lo vendido de un producto en un periodo. */
@Data
@NoArgsConstructor
public class VentaProductoDTO {
    private String nombre;
    private int cantidad;
    private double total;
}
