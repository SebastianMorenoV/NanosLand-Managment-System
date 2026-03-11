/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.persistencia.dominio;

import com.mycompany.persistencia.dominio.enums.TurnoEvento;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import lombok.ToString;

@Entity
@Table(name = "eventos")
@Data
@NoArgsConstructor
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fechaHoraInicio;

    @Enumerated(EnumType.STRING)
    private TurnoEvento turno;

    private String notas;

    @ToString.Exclude
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cotizacion_id")
    private Cotizacion cotizacion;
    
    @ToString.Exclude
    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LogisticaServicio> logisticaServicios;

    @ToString.Exclude
    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CargoExtra> cargosExtras;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(LocalDateTime fechaHoraInicio) {
        this.fechaHoraInicio = fechaHoraInicio;
    }

    public TurnoEvento getTurno() {
        return turno;
    }

    public void setTurno(TurnoEvento turno) {
        this.turno = turno;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public Cotizacion getCotizacion() {
        return cotizacion;
    }

    public void setCotizacion(Cotizacion cotizacion) {
        this.cotizacion = cotizacion;
    }

    public List<LogisticaServicio> getLogisticaServicios() {
        return logisticaServicios;
    }

    public void setLogisticaServicios(List<LogisticaServicio> logisticaServicios) {
        this.logisticaServicios = logisticaServicios;
    }

    public List<CargoExtra> getCargosExtras() {
        return cargosExtras;
    }

    public void setCargosExtras(List<CargoExtra> cargosExtras) {
        this.cargosExtras = cargosExtras;
    }
    
    
}