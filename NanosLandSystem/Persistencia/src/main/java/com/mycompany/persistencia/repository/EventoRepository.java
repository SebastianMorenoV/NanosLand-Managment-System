/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.persistencia.repository;

import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.enums.TurnoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author skyro
 */
public interface EventoRepository extends JpaRepository<Evento, Long>{

    List<Evento> findByFechaHoraInicioAndTurno(LocalDateTime fechaHoraInicio, TurnoEvento turno);
    Optional<Evento> findByCotizacionId(Long cotizacionId);
    List<Evento> findByFechaHoraInicioBetween(LocalDateTime inicio, LocalDateTime fin);
}
