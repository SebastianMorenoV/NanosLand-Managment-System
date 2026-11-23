/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.persistencia.dominio;

/**
 *
 * @author Sebastian Moreno 
 */
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clientes")
// Sin @Data: su equals/hashCode por valor recorre las relaciones (ciclos entre
// Cotizacion y Evento, y carga perezosa fuera de transacción).
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(length = 10)
    private String telefono;

    private String correo;

    /**
     * Dirección del cliente embebida en la misma tabla.
     * Los campos calle/colonia/ciudad/codigo_postal se almacenan como
     * columnas directas en "clientes" — no se crea ninguna tabla extra.
     */
    @Embedded
    private Direccion direccion;

    /**
     * Indica si el cliente está activo en el sistema.
     * Se usa para eliminación lógica (soft-delete) preservando el historial
     * de cotizaciones asociadas al cliente sin violar restricciones de FK.
     */
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean activo = true;
}
