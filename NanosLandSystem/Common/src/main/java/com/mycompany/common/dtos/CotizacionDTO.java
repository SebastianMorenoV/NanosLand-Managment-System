package com.mycompany.common.dtos;

import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.TurnoEvento;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CotizacionDTO {
    private Long id;
    private String folio;
    private LocalDate fecha;
    private Long clienteId;
    private String nombreCliente;
    private String nombreFestejado;
    private String tematica;
    private double total;
    private String notas;
    private EstadoCotizacion estado;
    private TurnoEvento turno;
    private Long paqueteId;
    private String nombrePaquete;
    private Long usuarioId;
    private List<DetalleCotizacionDTO> detalles;
}