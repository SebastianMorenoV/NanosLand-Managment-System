/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.persistencia.dominio;

import com.mycompany.persistencia.dominio.enums.EstadoLogistica;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalTime getHoraRequerida() {
        return horaRequerida;
    }

    public void setHoraRequerida(LocalTime horaRequerida) {
        this.horaRequerida = horaRequerida;
    }

    public String getEspecificaciones() {
        return especificaciones;
    }

    public void setEspecificaciones(String especificaciones) {
        this.especificaciones = especificaciones;
    }

    public String getDesgloseOpciones() {
        return desgloseOpciones;
    }

    public void setDesgloseOpciones(String desgloseOpciones) {
        this.desgloseOpciones = desgloseOpciones;
    }

    public String getUbicacionMontaje() {
        return ubicacionMontaje;
    }

    public void setUbicacionMontaje(String ubicacionMontaje) {
        this.ubicacionMontaje = ubicacionMontaje;
    }

    public String getResponsableTurno() {
        return responsableTurno;
    }

    public void setResponsableTurno(String responsableTurno) {
        this.responsableTurno = responsableTurno;
    }

    public EstadoLogistica getEstado() {
        return estado;
    }

    public void setEstado(EstadoLogistica estado) {
        this.estado = estado;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public void setServicio(Servicio servicio) {
        this.servicio = servicio;
    }

    public Evento getEvento() {
        return evento;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }
    
    
}