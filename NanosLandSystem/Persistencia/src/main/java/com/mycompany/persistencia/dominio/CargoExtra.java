/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.persistencia.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cargos_extras")
@Data
@NoArgsConstructor
public class CargoExtra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación al catálogo (Puede ser NULL si es un cargo inventado/manual en el momento)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = true)
    private Servicio servicio;

    // El texto libre que escriben en la pantalla (Ej. "Vaso roto" o "Bolsa de Hielo")
    @Column(length = 255)
    private String descripcion;

    // Se deja como double por si se cobran fracciones (Ej. 0.5 horas extra)
    @Column(nullable = false)
    private double cantidad;
    
    @Column(name = "precio_unitario", nullable = false)
    private double precioUnitario;
    
    @Column(nullable = false)
    private double subtotal;

    // Para saber en qué momento exacto de la fiesta se pidió o registró este cargo
    @Column(name = "fecha_hora_cargo")
    private LocalDateTime fechaHoraCargo;

    // ¡Crucial! Va ligado directamente al EVENTO físico, no a la cotización planeada
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;
}
