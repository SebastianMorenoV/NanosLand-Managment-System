package com.mycompany.common.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Un renglón de la tabla de precios de un paquete: comida + personas → precio. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaquetePrecioDTO {
    private Long id;
    private Long comidaId;
    private String comidaNombre;
    private int personas;
    private double precio;
}
