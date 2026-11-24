package com.mycompany.persistencia.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Precio de un paquete para una comida y un número de personas, como en las tablas
 * de los volantes (p. ej. Paquete Full con barbacoa para 100 personas).
 *
 * Las cotizaciones no apuntan a esta fila: guardan la comida, las personas y el precio
 * pactado, así que las filas se pueden editar o quitar sin tocar cotizaciones hechas.
 */
@Entity
@Table(name = "paquetes_precios", uniqueConstraints = @UniqueConstraint(
        name = "uk_paquete_precio_combinacion", columnNames = {"paquete_id", "comida_id", "personas"}))
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PaquetePrecio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paquete_id", nullable = false)
    private Paquete paquete;

    /** Servicio de categoría COMIDA. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "comida_id", nullable = false)
    private Servicio comida;

    @Column(nullable = false)
    private int personas;

    @Column(nullable = false)
    private double precio;
}
