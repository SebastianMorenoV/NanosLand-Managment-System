package com.mycompany.persistencia.repository;

import com.mycompany.persistencia.dominio.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    /** Ventas con fecha desde {@code inicio} y antes de {@code fin}, las más recientes primero. */
    @Query("SELECT DISTINCT v FROM Venta v " +
           "JOIN FETCH v.usuario " +
           "LEFT JOIN FETCH v.detalles d " +
           "LEFT JOIN FETCH d.producto " +
           "WHERE v.fechaHora >= :inicio AND v.fechaHora < :fin " +
           "ORDER BY v.fechaHora DESC, v.id DESC")
    List<Venta> findConDetallesEntre(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    boolean existsByFolio(String folio);

    @Query("SELECT COUNT(v) FROM Venta v WHERE v.folio LIKE CONCAT('VEN-', :anio, '-%')")
    long contarPorAnio(@Param("anio") int anio);
}
