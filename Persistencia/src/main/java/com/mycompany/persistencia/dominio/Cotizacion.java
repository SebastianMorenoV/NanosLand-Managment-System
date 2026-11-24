/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.persistencia.dominio;

import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.TurnoEvento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import lombok.ToString;

@Entity
@Table(name = "cotizaciones")
// Sin @Data: su equals/hashCode por valor recorre las relaciones (ciclos entre
// Cotizacion y Evento, y carga perezosa fuera de transacción).
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String folio;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    private double total;
    private String notas;

    @Enumerated(EnumType.STRING)
    private EstadoCotizacion estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paquete_id")
    private Paquete paquete;

    /** Comida elegida, solo en paquetes con tabla de precios (servicio de categoría COMIDA). */
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comida_id")
    private Servicio comida;

    /** Número de personas elegido (uno de los rangos de la tabla), solo en paquetes con tabla de precios. */
    private Integer personas;

    /**
     * Precio pactado del paquete. Se calcula al crear la cotización y no cambia aunque
     * después cambie el catálogo, salvo que se elija otro paquete, comida o número de personas.
     * Nulo solo en cotizaciones de antes de este campo, hasta que la corrección al arrancar lo llena.
     */
    @Column(name = "precio_paquete")
    private Double precioPaquete;

    /**
     * Precio de catálogo del paquete antes del descuento, tal como estaba al cotizar.
     * Nulo en cotizaciones de antes de los descuentos (su precio de lista es {@link #precioPaquete}).
     */
    @Column(name = "precio_lista")
    private Double precioLista;

    /** Nombre del descuento aplicado al paquete, como se llamaba al cotizar. Nulo si no tuvo descuento. */
    @Column(name = "descuento_nombre")
    private String descuentoNombre;

    /** Pesos que se descontaron al paquete: {@code precioLista - precioPaquete}. Nulo o 0 si no tuvo descuento. */
    @Column(name = "descuento_monto")
    private Double descuentoMonto;

    @ToString.Exclude
    @OneToOne(mappedBy = "cotizacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private Evento evento;

    @ToString.Exclude
    @OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleCotizacion> serviciosExtra;

}