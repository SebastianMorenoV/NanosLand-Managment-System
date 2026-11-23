package com.example.negocio.cotizacion.usecase;

import com.example.negocio.exception.CotizacionException;
import com.example.negocio.logistica.usecase.GenerarLogisticaEventoUseCase;
import com.mycompany.persistencia.dominio.CargoExtra;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.dominio.Pago;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.enums.MetodoPago;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.EventoRepository;
import com.mycompany.persistencia.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrarAnticipoUseCase {
    private final CotizacionRepository cotizacionRepository;
    private final PagoRepository pagoRepository;
    private final EventoRepository eventoRepository;
    private final GenerarLogisticaEventoUseCase generarLogisticaEventoUseCase;

    @Transactional
    public Pago registrarAnticipo(Long cotizacionId, double cantidad, MetodoPago tipoPago) {
        if (cotizacionId == null) throw new CotizacionException("Debe indicar la cotización.");
        if (cantidad <= 0) throw new CotizacionException("El abono debe ser mayor a 0.");

        MetodoPago metodo = tipoPago != null ? tipoPago : MetodoPago.EFECTIVO;
        Cotizacion cotizacion = cotizacionRepository.findById(cotizacionId)
            .orElseThrow(() -> new CotizacionException("No existe la cotización indicada."));

        // No se recibe dinero de una cotización cancelada/eliminada ni de un evento cancelado:
        // el turno pudo haberse asignado ya a otro cliente.
        if (cotizacion.getEstado() == EstadoCotizacion.CANCELADA
                || cotizacion.getEstado() == EstadoCotizacion.ELIMINADA) {
            throw new CotizacionException("La cotización está cancelada y no admite pagos.");
        }
        Evento eventoCotizacion = cotizacion.getEvento();
        if (eventoCotizacion != null
                && (eventoCotizacion.getEstado() == EstadoEvento.CANCELADO
                    || eventoCotizacion.getEstado() == EstadoEvento.CANCELADO_TARDIO)) {
            throw new CotizacionException("El evento de esta cotización está cancelado y no admite pagos.");
        }

        List<Pago> pagosExistentes = pagoRepository.findByCotizacionId(cotizacionId);
        double anticipoExistente = pagosExistentes == null ? 0.0 : pagosExistentes.stream().mapToDouble(Pago::getCantidad).sum();

        // El saldo incluye los cargos extra del evento, para que se puedan cobrar.
        double saldo = calcularSaldo(cotizacion, anticipoExistente);
        if (redondear(cantidad) > saldo) {
            throw new CotizacionException("El abono excede el saldo pendiente.");
        }

        // Con este pago la cotización puede quedar confirmada: como los borradores no
        // apartan la fecha, se comprueba aquí que nadie más haya confirmado ese turno.
        boolean seConfirma = anticipoExistente + cantidad >= anticipoMinimo(cotizacion.getTotal())
            && cotizacion.getEstado() != EstadoCotizacion.VIGENTE;
        if (seConfirma && eventoCotizacion != null && eventoCotizacion.getEstado() == EstadoEvento.TENTATIVO
                && eventoRepository.existsByFechaAndTurnoAndEstadoInAndIdNot(
                    eventoCotizacion.getFecha(), eventoCotizacion.getTurno(),
                    com.example.negocio.agenda.usecase.VerificarDisponibilidadTurnoUseCase.ESTADOS_QUE_OCUPAN,
                    eventoCotizacion.getId())) {
            throw new CotizacionException(
                "La fecha y el turno de esta cotización ya fueron confirmados para otro evento. "
                + "Cambie la fecha o el turno antes de registrar el pago.");
        }

        Pago pago = new Pago();
        pago.setCotizacion(cotizacion);
        pago.setCantidad(cantidad);
        pago.setFechaHora(LocalDateTime.now());
        pago.setTipo(metodo);
        pago.setFolioPago(generarFolioPago());

        Pago pagoGuardado = pagoRepository.save(pago);

        // Si la suma de pagos alcanza el anticipo mínimo, Confirmamos la Cotizacion y el Evento
        if (seConfirma) {
            cotizacion.setEstado(EstadoCotizacion.VIGENTE);
            cotizacionRepository.save(cotizacion);

            Evento evento = cotizacion.getEvento();
            if (evento != null && evento.getEstado() == EstadoEvento.TENTATIVO) {
                evento.setEstado(EstadoEvento.CONFIRMADO);
                
                // Asignar horarios por defecto del turno si no los tiene
                if (evento.getHoraInicio() == null || evento.getHoraFin() == null) {
                    com.example.negocio.evento.usecase.HorarioTurno.asignar(evento);
                }
                eventoRepository.save(evento);

                // Con el evento confirmado se abre su bitácora de logística:
                // un registro por cada servicio del paquete y cada extra.
                generarLogisticaEventoUseCase.sincronizar(evento, false);
            }
        }
        return pagoGuardado;
    }

    public double obtenerTotalAbonado(Long cotizacionId) {
        List<Pago> pagosExistentes = pagoRepository.findByCotizacionId(cotizacionId);
        return pagosExistentes == null ? 0.0 : pagosExistentes.stream().mapToDouble(Pago::getCantidad).sum();
    }

    /** Anticipo que confirma una cotización: $3,000, o el total si la cotización vale menos. */
    public static final double ANTICIPO_MINIMO = 3000.0;

    public static double anticipoMinimo(double totalCotizacion) {
        return Math.min(ANTICIPO_MINIMO, totalCotizacion);
    }

    /** Anticipo mínimo que confirma la cotización indicada. */
    @Transactional(readOnly = true)
    public double obtenerAnticipoMinimo(Long cotizacionId) {
        Cotizacion cotizacion = cotizacionRepository.findById(cotizacionId)
            .orElseThrow(() -> new CotizacionException("No existe la cotización indicada."));
        return anticipoMinimo(cotizacion.getTotal());
    }

    /**
     * Saldo pendiente de una cotización: total cotizado más los cargos extra
     * de su evento, menos lo ya abonado. Es el mismo saldo que limita los abonos.
     */
    @Transactional(readOnly = true)
    public double obtenerSaldoPendiente(Long cotizacionId) {
        Cotizacion cotizacion = cotizacionRepository.findById(cotizacionId)
            .orElseThrow(() -> new CotizacionException("No existe la cotización indicada."));
        return calcularSaldo(cotizacion, obtenerTotalAbonado(cotizacionId));
    }

    private double calcularSaldo(Cotizacion cotizacion, double abonado) {
        double cargosExtra = 0.0;
        Evento evento = cotizacion.getEvento();
        if (evento != null && evento.getCargosExtras() != null) {
            cargosExtra = evento.getCargosExtras().stream().mapToDouble(CargoExtra::getSubtotal).sum();
        }
        return Math.max(0.0, redondear(cotizacion.getTotal() + cargosExtra - abonado));
    }

    /** Redondea a centavos para que los residuos de punto flotante no alteren las comparaciones. */
    private static double redondear(double monto) {
        return Math.round(monto * 100.0) / 100.0;
    }

    /**
     * Genera un folio de pago único con formato PAY-YYYY-NNNN.
     *
     * Fix #2 — Implementación segura ante race conditions:
     * - Usa conteo por año (no COUNT global) para evitar colisiones al cruzar el año nuevo.
     * - Verifica unicidad con do-while (mismo patrón que folios de cotización).
     * - El constraint UNIQUE en Pago.folioPago actuará como barrera final en BD.
     */
    private String generarFolioPago() {
        int anio = Year.now().getValue();
        long base = pagoRepository.contarPagosPorAnio(anio) + 1;
        String folio;
        do {
            folio = String.format("PAY-%d-%04d", anio, base);
            base++;
        } while (pagoRepository.existsByFolioPago(folio));
        return folio;
    }
}