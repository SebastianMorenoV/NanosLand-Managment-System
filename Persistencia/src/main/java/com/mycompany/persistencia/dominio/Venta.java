package com.mycompany.persistencia.dominio;

import com.mycompany.persistencia.enums.EstadoVenta;
import com.mycompany.persistencia.enums.MetodoPago;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Venta extra: ingreso del negocio que no pertenece a ningún evento, por lo que
 * no afecta cotizaciones, estados de cuenta ni adeudos.
 */
@Entity
@Table(name = "ventas")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String folio;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    // varchar y no ENUM nativo: ddl-auto=update no modifica columnas ENUM existentes,
    // así que agregar un valor nuevo más adelante fallaría al guardar.
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, columnDefinition = "varchar(20)")
    private MetodoPago metodoPago;

    @Column(nullable = false)
    private double total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private EstadoVenta estado = EstadoVenta.REGISTRADA;

    /** Quién registró la venta. */
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    private LocalDateTime fechaCancelacion;

    /** Quién canceló la venta, si se canceló. */
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_cancelacion_id")
    private Usuario usuarioCancelacion;

    @ToString.Exclude
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();
}
