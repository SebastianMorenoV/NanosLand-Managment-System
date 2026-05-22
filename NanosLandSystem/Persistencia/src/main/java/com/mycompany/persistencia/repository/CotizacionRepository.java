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
    boolean existsByPaqueteIdAndEstadoIn(Long paqueteId, Collection<EstadoCotizacion> estados);

    /**
     * NOTA: Las consultas de disponibilidad por fecha y turno han sido
     * movidas a EventoRepository, ya que Evento ahora es la entidad dueña de la fecha.
     */
}
