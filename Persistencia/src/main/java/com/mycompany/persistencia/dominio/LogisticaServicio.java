/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.persistencia.dominio;

import com.mycompany.persistencia.enums.EstadoLogistica;
import jakarta.persistence.*;
import java.time.LocalTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "logistica_servicios")
@Data
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

}
