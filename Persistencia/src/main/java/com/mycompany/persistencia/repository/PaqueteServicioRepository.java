/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.persistencia.repository;

import com.mycompany.persistencia.dominio.PaqueteServicio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 *
 * @author skyro
 */
public interface PaqueteServicioRepository extends JpaRepository<PaqueteServicio, Long>{

    /** Nombres de los paquetes activos que incluyen el servicio. */
    @Query("SELECT DISTINCT ps.paquete.nombre FROM PaqueteServicio ps "
         + "WHERE ps.servicio.id = :servicioId AND ps.paquete.activo = true "
         + "ORDER BY ps.paquete.nombre")
    List<String> nombresPaquetesActivosConServicio(@Param("servicioId") Long servicioId);
}
