package com.mycompany.common.dtos;

import com.mycompany.persistencia.enums.EstadoCotizacion;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author skyro
 */
@Data
@NoArgsConstructor
public class CotizacionDTO {
    private Long id;
    private String folio;
    private LocalDate fecha;
    private Long clienteId;
    private String nombreCliente;
    private String nombreFestejado;
    private double total;
    private String notas;
    private EstadoCotizacion estado;
    private Long paqueteId;
    private String nombrePaquete;
    private String tematica;
    private List<DetalleCotizacionDTO> detalles;
}
