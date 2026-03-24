package com.mycompany.common.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetodoPagoDTO {
    private String id;          // e.g., "TRANSFERENCIA"
    private String nombre;      // e.g., "Transferencia Bancaria"
    private boolean requiereReferencia;
}