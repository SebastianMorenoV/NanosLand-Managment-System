package com.mycompany.common.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class AdeudoDTO {
    private Long eventoId;
    private String folioCotizacion;
    private String clienteNombre;
    private String clienteTelefono;
    private LocalDate fechaEvento;
    private String estadoEvento;
    
    private double granTotal;
    private double totalPagado;
    private double saldoPendiente;
}
