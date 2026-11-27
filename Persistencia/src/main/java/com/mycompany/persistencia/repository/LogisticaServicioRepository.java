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

    long countByEventoIdAndEstado(Long eventoId, com.mycompany.persistencia.enums.EstadoLogistica estado);

    List<LogisticaServicio> findByEventoIdOrderByIdAsc(Long eventoId);

    @Query("SELECT ls FROM LogisticaServicio ls " +
           "LEFT JOIN FETCH ls.servicio " +
           "LEFT JOIN FETCH ls.evento " +
           "WHERE ls.evento.id = :eventoId " +
           "ORDER BY ls.id ASC")
    List<LogisticaServicio> findByEventoIdWithServicio(@Param("eventoId") Long eventoId);

    /**
     * Ids de los eventos en los estados dados con algún registro de antes de separar lo del
     * paquete y los extras (sin origen), para la corrección al arrancar.
     */
    @Query("SELECT DISTINCT ls.evento.id FROM LogisticaServicio ls " +
           "WHERE ls.origen IS NULL AND ls.evento.estado IN :estados")
    List<Long> eventosConRegistrosSinOrigen(
        @Param("estados") java.util.Collection<com.mycompany.persistencia.enums.EstadoEvento> estados);
}
