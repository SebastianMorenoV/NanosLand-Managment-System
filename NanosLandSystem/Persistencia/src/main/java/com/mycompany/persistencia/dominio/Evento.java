/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.persistencia.dominio;

import com.mycompany.persistencia.enums.TurnoEvento;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
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

    private LocalDate fecha;
    
    private LocalTime horaInicio;
    
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    private TurnoEvento turno;

    @Column(length = 2000)
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

}
