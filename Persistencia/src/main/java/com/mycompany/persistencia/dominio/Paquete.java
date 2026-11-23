/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.persistencia.dominio;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import lombok.ToString;

@Entity
@Table(name = "paquetes")
@Data
@NoArgsConstructor
public class Paquete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    private double costo;
    
    /**
     * Indica si el paquete está activo.
     * Se usa para eliminación lógica (Flujo 2.2.3 del CU-06) para no 
     * afectar cotizaciones históricas que hayan usado este paquete.
     */
    @Column(nullable = false)
    private boolean activo = true;

    @ToString.Exclude
    @OneToMany(mappedBy = "paquete", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PaqueteServicio> servicios;

}
