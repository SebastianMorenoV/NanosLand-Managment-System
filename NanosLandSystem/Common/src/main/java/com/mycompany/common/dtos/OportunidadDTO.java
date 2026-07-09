package com.mycompany.common.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class OportunidadDTO {
    private String clienteNombre;
    private String clienteTelefono;
    private LocalDate fechaEventoPasado;
    private String nombrePaquete;
    private double montoGastado;
}
