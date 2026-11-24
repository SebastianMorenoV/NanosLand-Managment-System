package com.mycompany.persistencia.dominio;

import com.mycompany.persistencia.enums.TipoDescuento;
import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Descuento automático al precio de los paquetes, según el día de la semana o la
 * temporada del evento (p. ej. "Entre semana: 15 % de lunes a jueves").
 *
 * Al cotizar se aplica solo el descuento que más rebaja. Las cotizaciones no apuntan a
 * esta fila: guardan el nombre y el monto del descuento, así que se puede editar o borrar
 * sin tocar cotizaciones hechas.
 */
@Entity
@Table(name = "descuentos")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Descuento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDescuento tipo;

    /** Porcentaje (1 a 100) o pesos, según {@link #tipo}. */
    @Column(nullable = false)
    private double valor;

    /** Días de la semana del evento en que aplica. Vacío: todos los días. */
    @ElementCollection
    @CollectionTable(name = "descuentos_dias", joinColumns = @JoinColumn(name = "descuento_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "dia", nullable = false)
    private Set<DayOfWeek> dias = new HashSet<>();

    /** Primer día de eventos de la temporada. Nulo: sin fecha de inicio. */
    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    /** Último día de eventos de la temporada. Nulo: sin fecha de fin. */
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    /** Último día en que se puede cotizar con este descuento. Nulo: sin límite. */
    @Column(name = "cotizar_hasta")
    private LocalDate cotizarHasta;

    /** Si aplica a todos los paquetes; si no, solo a {@link #paquetes}. */
    @Column(name = "todos_los_paquetes", nullable = false)
    private boolean todosLosPaquetes = true;

    @ToString.Exclude
    @ManyToMany
    @JoinTable(name = "descuentos_paquetes",
            joinColumns = @JoinColumn(name = "descuento_id"),
            inverseJoinColumns = @JoinColumn(name = "paquete_id"))
    private Set<Paquete> paquetes = new HashSet<>();

    /** Interruptor para pausar el descuento sin borrarlo. */
    @Column(nullable = false)
    private boolean activo = true;
}
