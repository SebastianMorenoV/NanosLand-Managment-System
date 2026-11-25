package com.mycompany.persistencia.repository;

import com.mycompany.persistencia.dominio.Usuario;
import com.mycompany.persistencia.dominio.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    /**
     * Ventas con fecha desde {@code inicio} y antes de {@code fin}, las más recientes primero.
     * El usuario va con LEFT JOIN: las ventas de un usuario eliminado no tienen autor.
     */
    @Query("SELECT DISTINCT v FROM Venta v " +
           "LEFT JOIN FETCH v.usuario " +
           "LEFT JOIN FETCH v.detalles d " +
           "LEFT JOIN FETCH d.producto " +
           "WHERE v.fechaHora >= :inicio AND v.fechaHora < :fin " +
           "ORDER BY v.fechaHora DESC, v.id DESC")
    List<Venta> findConDetallesEntre(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    boolean existsByFolio(String folio);

    @Query("SELECT COUNT(v) FROM Venta v WHERE v.folio LIKE CONCAT('VEN-', :anio, '-%')")
    long contarPorAnio(@Param("anio") int anio);

    /** Antes de eliminar un usuario: sus ventas se conservan, sin autor. */
    @Modifying
    @Query("UPDATE Venta v SET v.usuario = NULL WHERE v.usuario = :usuario")
    int quitarUsuario(@Param("usuario") Usuario usuario);

    /** Antes de eliminar un usuario: las ventas que canceló se conservan, sin quién las canceló. */
    @Modifying
    @Query("UPDATE Venta v SET v.usuarioCancelacion = NULL WHERE v.usuarioCancelacion = :usuario")
    int quitarUsuarioCancelacion(@Param("usuario") Usuario usuario);
}
