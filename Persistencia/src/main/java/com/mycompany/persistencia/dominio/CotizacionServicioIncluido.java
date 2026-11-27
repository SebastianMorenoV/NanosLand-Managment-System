package com.mycompany.persistencia.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Un servicio que incluía el paquete al momento de cotizar, con su cantidad. Es la copia de lo
 * vendido: si después cambian los servicios del paquete en el catálogo, la cotización (su
 * logística) conserva estos.
 */
@Entity
@Table(name = "cotizacion_servicios_incluidos")
// Sin @Data: su equals/hashCode por valor recorrería la cotización.
@Getter
@Setter
@ToString
@NoArgsConstructor
public class CotizacionServicioIncluido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cotizacion_id", nullable = false)
    private Cotizacion cotizacion;

    /** Servicio del catálogo; la logística del evento se coordina con él. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

    /** Nombre del servicio como se llamaba al cotizar. */
    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private int cantidad;
}
