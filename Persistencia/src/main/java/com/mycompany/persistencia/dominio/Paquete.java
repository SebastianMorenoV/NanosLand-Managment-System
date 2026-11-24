/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.persistencia.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import lombok.ToString;

@Entity
@Table(name = "paquetes")
// Sin @Data: su equals/hashCode por valor recorre las relaciones (ciclos entre
// Cotizacion y Evento, y carga perezosa fuera de transacción).
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Paquete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    /**
     * Precio de los paquetes de precio fijo (p. ej. la renta del local). Los paquetes con
     * tabla de precios lo dejan en 0. Para cotizar use CalcularPrecioPaqueteUseCase, que
     * decide entre este precio y la tabla.
     */
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

    /**
     * Tabla de precios por comida y número de personas. Si está vacía, el paquete
     * es de precio fijo ({@link #costo}).
     */
    @ToString.Exclude
    @OneToMany(mappedBy = "paquete", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PaquetePrecio> precios = new ArrayList<>();

    /** Indica si el precio sale de la tabla (comida y personas) y no de {@link #costo}. */
    public boolean tieneTablaDePrecios() {
        return precios != null && !precios.isEmpty();
    }

}
