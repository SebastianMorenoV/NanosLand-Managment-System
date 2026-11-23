/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.persistencia.repository;

import com.mycompany.persistencia.dominio.LogisticaServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 *
 * @author skyro
 */
public interface LogisticaServicioRepository extends JpaRepository<LogisticaServicio, Long> {

    List<LogisticaServicio> findByEventoId(Long eventoId);

    List<LogisticaServicio> findByEventoIdOrderByIdAsc(Long eventoId);

    @Query("SELECT ls FROM LogisticaServicio ls " +
           "LEFT JOIN FETCH ls.servicio " +
           "LEFT JOIN FETCH ls.evento " +
           "WHERE ls.evento.id = :eventoId " +
           "ORDER BY ls.id ASC")
    List<LogisticaServicio> findByEventoIdWithServicio(@Param("eventoId") Long eventoId);
}
