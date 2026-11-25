package com.mycompany.common.dtos;

import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.TurnoEvento;
import java.time.LocalDate;
import java.time.LocalTime;
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
    /** Horario real del evento (el que sale en el contrato). */
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Long paqueteId;
    private String nombrePaquete;
    /** Comida elegida (paquetes con tabla de precios). */
    private Long comidaId;
    private String comidaNombre;
    /** Rango de personas elegido (paquetes con tabla de precios). */
    private Integer personas;
    /** Precio pactado del paquete. Solo de salida: el servidor lo calcula y no lo toma del DTO. */
    private Double precioPaquete;
    /** Solo de salida: precio de catálogo del paquete antes del descuento (nulo en cotizaciones antiguas). */
    private Double precioLista;
    /** Solo de salida: nombre del descuento aplicado al paquete, o nulo. */
    private String descuentoNombre;
    /** Solo de salida: pesos descontados al paquete, o nulo. */
    private Double descuentoMonto;
    private String tematica;
    private Long usuarioId;
    private List<DetalleCotizacionDTO> detalles;
}
