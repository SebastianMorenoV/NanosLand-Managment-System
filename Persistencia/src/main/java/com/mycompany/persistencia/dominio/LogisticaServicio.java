/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.persistencia.dominio;

import com.mycompany.persistencia.enums.EstadoLogistica;
import com.mycompany.persistencia.enums.OrigenLogistica;
import jakarta.persistence.*;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "logistica_servicios")
// Sin @Data: su equals/hashCode por valor recorre las relaciones (ciclos entre
// Cotizacion y Evento, y carga perezosa fuera de transacción).
@Getter
@Setter
@ToString
@NoArgsConstructor
public class LogisticaServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hora_requerida")
    private LocalTime horaRequerida;

    @Column(length = 500)
    private String especificaciones;

    @Column(name = "desglose_opciones", length = 500)
    private String desgloseOpciones;

    @Column(name = "ubicacion_montaje")
    private String ubicacionMontaje;

    @Column(name = "responsable_turno")
    private String responsableTurno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoLogistica estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id")
    private Evento evento;

    // Lo que incluye el paquete y cada extra van en registros separados, cada uno con sus datos.
    // Las tres columnas son opcionales: los registros de antes de separarlos no las tienen hasta
    // que la corrección al arrancar (o la siguiente edición de la cotización) los separa.

    /** Del paquete o de un extra. Nulo en registros de antes de separarlos. */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private OrigenLogistica origen;

    /** Cantidad de este registro (la que va también al inicio de las especificaciones). */
    private Integer cantidad;

    /**
     * Solo de un extra: sus datos tal como estaban en la cotización (servicio, hora, especificaciones,
     * desglose y ubicación). Así se reconoce el extra aunque aquí se editen esos datos.
     */
    @Column(name = "clave_extra", length = 1500)
    private String claveExtra;
}
