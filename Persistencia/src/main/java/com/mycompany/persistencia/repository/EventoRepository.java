/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.persistencia.repository;

import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.enums.TurnoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author skyro
 */
public interface EventoRepository extends JpaRepository<Evento, Long>{

    List<Evento> findByFechaAndTurno(LocalDate fecha, TurnoEvento turno);
    
    List<Evento> findByFecha(LocalDate fecha);
    
    Optional<Evento> findByCotizacionId(Long cotizacionId);
    
    List<Evento> findByFechaBetween(LocalDate inicio, LocalDate fin);

    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT e FROM Evento e " +
        "LEFT JOIN FETCH e.cotizacion c " +
        "LEFT JOIN FETCH c.cliente " +
        "LEFT JOIN FETCH c.paquete " +
        "LEFT JOIN FETCH e.cargosExtras " +
        "WHERE e.fecha BETWEEN :inicio AND :fin"
    )
    List<Evento> findByFechaBetweenWithRelations(
        @org.springframework.data.repository.query.Param("inicio") LocalDate inicio,
        @org.springframework.data.repository.query.Param("fin") LocalDate fin
    );

    List<Evento> findByEstadoNot(EstadoEvento estado);

    List<Evento> findByEstadoIn(java.util.Collection<EstadoEvento> estados);

    List<Evento> findByEstadoAndFechaBetween(EstadoEvento estado, LocalDate inicio, LocalDate fin);

    boolean existsByFechaAndTurnoAndEstadoNot(LocalDate fecha, TurnoEvento turno, EstadoEvento estado);

    /**
     * Fecha y turno de los eventos de un periodo que están en alguno de los estados dados.
     * Devuelve solo esas dos columnas: sirve para pintar un calendario con una sola consulta.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT e.fecha, e.turno FROM Evento e " +
        "WHERE e.fecha BETWEEN :inicio AND :fin AND e.estado IN :estados")
    List<Object[]> findFechaYTurnoOcupados(
        @org.springframework.data.repository.query.Param("inicio") LocalDate inicio,
        @org.springframework.data.repository.query.Param("fin") LocalDate fin,
        @org.springframework.data.repository.query.Param("estados") java.util.Collection<EstadoEvento> estados);

    boolean existsByFechaAndTurnoAndEstadoIn(LocalDate fecha, TurnoEvento turno,
                                             java.util.Collection<EstadoEvento> estados);

    boolean existsByFechaAndTurnoAndEstadoInAndIdNot(LocalDate fecha, TurnoEvento turno,
                                                     java.util.Collection<EstadoEvento> estados, Long id);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
        "UPDATE Evento e SET e.estado = :nuevo WHERE e.estado IN :actuales AND e.fecha < :hoy")
    int cambiarEstadoDeEventosAnterioresA(
        @org.springframework.data.repository.query.Param("hoy") LocalDate hoy,
        @org.springframework.data.repository.query.Param("actuales") java.util.Collection<EstadoEvento> actuales,
        @org.springframework.data.repository.query.Param("nuevo") EstadoEvento nuevo);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
        "UPDATE Evento e SET e.estado = :nuevo WHERE e.estado = :actual AND e.fecha = :hoy")
    int cambiarEstadoDeEventosDelDia(
        @org.springframework.data.repository.query.Param("hoy") LocalDate hoy,
        @org.springframework.data.repository.query.Param("actual") EstadoEvento actual,
        @org.springframework.data.repository.query.Param("nuevo") EstadoEvento nuevo);

    /**
     * Consulta base para generar el reporte de eventos (CU-07).
     * Aplica filtros opcionales de turno y estado, asegurando recuperar los datos
     * relacionados (cotización, cliente, paquete) en una sola consulta.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT e FROM Evento e " +
        "JOIN FETCH e.cotizacion c " +
        "JOIN FETCH c.cliente cl " +
        "JOIN FETCH c.paquete p " +
        "WHERE (:inicio IS NULL OR e.fecha >= :inicio) " +
        "AND (:fin IS NULL OR e.fecha <= :fin) " +
        "AND (:turno IS NULL OR e.turno = :turno) " +
        "AND (:estado IS NULL OR e.estado = :estado) " +
        "ORDER BY e.fecha DESC"
    )
    List<Evento> findReporteEventos(
        @org.springframework.data.repository.query.Param("inicio") LocalDate inicio,
        @org.springframework.data.repository.query.Param("fin") LocalDate fin,
        @org.springframework.data.repository.query.Param("turno") TurnoEvento turno,
        @org.springframework.data.repository.query.Param("estado") EstadoEvento estado
    );

    @org.springframework.data.jpa.repository.Query(
        "SELECT e FROM Evento e " +
        "JOIN FETCH e.cotizacion c " +
        "JOIN FETCH c.cliente cl " +
        "JOIN FETCH c.paquete p " +
        "WHERE (:turno IS NULL OR e.turno = :turno) " +
        "AND (:estado IS NULL OR e.estado = :estado) " +
        "ORDER BY e.fecha DESC"
    )
    List<Evento> findReporteEventosMasRecientes(
        @org.springframework.data.repository.query.Param("turno") TurnoEvento turno,
        @org.springframework.data.repository.query.Param("estado") EstadoEvento estado,
        org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT e FROM Evento e " +
        "LEFT JOIN FETCH e.cotizacion c " +
        "LEFT JOIN FETCH c.cliente cl " +
        "LEFT JOIN FETCH c.paquete p " +
        "LEFT JOIN FETCH e.cargosExtras ce " +
        "ORDER BY e.fecha DESC, e.id DESC"
    )
    List<Evento> findAllWithRelations();
}
