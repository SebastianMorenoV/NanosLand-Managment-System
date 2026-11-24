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

    /** Cotizaciones en el estado dado cuya fecha de evento es {@code desde} o posterior. */
    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(c) FROM Cotizacion c JOIN c.evento e WHERE c.estado = :estado AND e.fecha >= :desde")
    long contarPorEstadoDesde(
        @org.springframework.data.repository.query.Param("estado") EstadoCotizacion estado,
        @org.springframework.data.repository.query.Param("desde") java.time.LocalDate desde);

    /**
     * Indica si el paquete tiene cotizaciones todavía abiertas: en alguno de los
     * estados dados y cuyo evento aún no ha finalizado. Una venta ya realizada
     * no impide modificar el paquete.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(c) > 0 FROM Cotizacion c LEFT JOIN c.evento e " +
        "WHERE c.paquete.id = :paqueteId AND c.estado IN :estados " +
        "AND (e IS NULL OR e.estado <> :finalizado)")
    boolean existeCotizacionAbiertaConPaquete(
        @org.springframework.data.repository.query.Param("paqueteId") Long paqueteId,
        @org.springframework.data.repository.query.Param("estados") Collection<EstadoCotizacion> estados,
        @org.springframework.data.repository.query.Param("finalizado") com.mycompany.persistencia.enums.EstadoEvento finalizado);

    /**
     * Folio, id de comida y personas de las cotizaciones todavía abiertas del paquete
     * (mismo criterio que {@link #existeCotizacionAbiertaConPaquete}). Cada fila es
     * {@code Object[]{String folio, Long comidaId, Integer personas}}; comida y personas
     * son nulas en cotizaciones de un paquete de precio fijo.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT c.folio, co.id, c.personas FROM Cotizacion c LEFT JOIN c.comida co LEFT JOIN c.evento e " +
        "WHERE c.paquete.id = :paqueteId AND c.estado IN :estados " +
        "AND (e IS NULL OR e.estado <> :finalizado) ORDER BY c.folio")
    List<Object[]> combinacionesAbiertasDePaquete(
        @org.springframework.data.repository.query.Param("paqueteId") Long paqueteId,
        @org.springframework.data.repository.query.Param("estados") Collection<EstadoCotizacion> estados,
        @org.springframework.data.repository.query.Param("finalizado") com.mycompany.persistencia.enums.EstadoEvento finalizado);

    /** Cotizaciones anteriores al precio pactado, para la corrección al arrancar. */
    List<Cotizacion> findByPrecioPaqueteIsNull();

    /**
     * NOTA: Las consultas de disponibilidad por fecha y turno han sido
     * movidas a EventoRepository, ya que Evento ahora es la entidad dueña de la fecha.
     */
}
