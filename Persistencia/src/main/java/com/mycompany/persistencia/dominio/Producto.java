package com.mycompany.persistencia.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Artículo que el negocio vende aparte de los eventos (por ejemplo, calcetines
 * para los juegos). No tiene relación con servicios, paquetes ni cotizaciones.
 */
@Entity
@Table(name = "productos")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private double precio;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean activo = true;
}
