/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.persistencia.repository;

import com.mycompany.persistencia.dominio.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 *
 * @author skyro
 */
public interface PagoRepository extends JpaRepository<Pago, Long>{

    List<Pago> findByCotizacionId(Long cotizacionId);

    /**
     * Verifica si ya existe un pago con el folio dado.
     * Usado en generarFolioPago() para garantizar unicidad ante race conditions.
     */
    boolean existsByFolioPago(String folioPago);

    /**
     * Cuenta los pagos cuyo folio pertenece al año indicado.
     * Permite inicializar la secuencia por año sin depender del COUNT global,
     * evitando colisiones al cruzar el año nuevo.
     */
    @Query("SELECT COUNT(p) FROM Pago p WHERE p.folioPago LIKE CONCAT('PAY-', :anio, '-%')")
    long contarPagosPorAnio(@Param("anio") int anio);
}
