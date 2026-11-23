/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.persistencia.dominio;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Entity
@Table(name = "detalles_cotizacion")
@Data
@NoArgsConstructor
public class DetalleCotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cotizacion_id")
    private Cotizacion cotizacion;

    private double precioUnitario;
    private int cantidad;
    private double subtotal;

    @Column(name = "hora_sugerida")
    private LocalTime horaSugerida;

    @Column(name = "especificaciones_cliente", length = 500)
    private String especificacionesCliente;

    @Column(name = "desglose_opciones", length = 500)
    private String desgloseOpciones;

    @Column(name = "ubicacion_montaje")
    private String ubicacionMontaje;

}
