/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.persistencia.dominio;

import com.mycompany.persistencia.enums.CategoriaServicio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "servicios")
// Sin @Data: su equals/hashCode por valor recorre las relaciones (ciclos entre
// Cotizacion y Evento, y carga perezosa fuera de transacción).
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    private double precio;

    @Lob
    private byte[] imagen;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean activo = true;

    // varchar y no ENUM nativo: ddl-auto=update no modifica columnas ENUM existentes.
    // El default llena las filas que ya existían al agregar la columna; sin él MySQL
    // les pondría '' y Hibernate no podría leer esos servicios.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'EXTRA'")
    private CategoriaServicio categoria = CategoriaServicio.EXTRA;

}
