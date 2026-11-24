package com.mycompany.common.dtos;

import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.TurnoEvento;
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
    private TurnoEvento turno;
    private Long paqueteId;
    private String nombrePaquete;
    /** Comida elegida (paquetes con tabla de precios). */
    private Long comidaId;
    private String comidaNombre;
    /** Rango de personas elegido (paquetes con tabla de precios). */
    private Integer personas;
    /** Precio pactado del paquete. Solo de salida: el servidor lo calcula y no lo toma del DTO. */
    private Double precioPaquete;
    private String tematica;
    private Long usuarioId;
    private List<DetalleCotizacionDTO> detalles;
}
