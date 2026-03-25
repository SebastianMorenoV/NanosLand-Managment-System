package com.mycompany.persistencia.repository;

import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.TurnoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author skyro
 */
public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    Optional<Cotizacion> findByFolio(String folio);
    List<Cotizacion> findByClienteId(Long clienteId);
    List<Cotizacion> findByEstado(EstadoCotizacion estado);

    /**
     * Verifica si ya existe una cotización para una fecha y turno dados,
     * excluyendo los estados indicados (ej. CANCELADA, ELIMINADA).
     * Usado para detectar conflictos de turno antes de crear una nueva cotización.
     */
    boolean existsByFechaAndTurnoAndEstadoNotIn(LocalDate fecha, TurnoEvento turno, Collection<EstadoCotizacion> estados);
    
    List<Cotizacion> findByFechaBetween(LocalDate inicio, LocalDate fin);
    List<Cotizacion> findByFecha(LocalDate fecha);
}
