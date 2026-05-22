package com.example.negocio.cotizacion.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.dominio.Pago;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.enums.MetodoPago;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.EventoRepository;
import com.mycompany.persistencia.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrarAnticipoUseCase {
    private final CotizacionRepository cotizacionRepository;
    private final PagoRepository pagoRepository;
    private final EventoRepository eventoRepository;

    @Transactional
    public Pago registrarAnticipo(Long cotizacionId, double cantidad, MetodoPago tipoPago) {
        if (cotizacionId == null) throw new CotizacionException("Debe indicar la cotización.");
        if (cantidad <= 0) throw new CotizacionException("El abono debe ser mayor a 0.");

        MetodoPago metodo = tipoPago != null ? tipoPago : MetodoPago.EFECTIVO;
        Cotizacion cotizacion = cotizacionRepository.findById(cotizacionId)
            .orElseThrow(() -> new CotizacionException("No existe la cotización indicada."));

        List<Pago> pagosExistentes = pagoRepository.findByCotizacionId(cotizacionId);
        double anticipoExistente = pagosExistentes == null ? 0.0 : pagosExistentes.stream().mapToDouble(Pago::getCantidad).sum();

        double saldo = cotizacion.getTotal() - anticipoExistente;
        if (cantidad > saldo) {
            throw new CotizacionException("El abono excede el saldo pendiente.");
        }

        Pago pago = new Pago();
        pago.setCotizacion(cotizacion);
        pago.setCantidad(cantidad);
        pago.setFechaHora(LocalDateTime.now());
        pago.setTipo(metodo);
        pago.setFolioPago(generarFolioPago());

        Pago pagoGuardado = pagoRepository.save(pago);

        // Si la suma de pagos alcanza o supera los $3,000, Confirmamos la Cotizacion y el Evento
        double totalAbonado = anticipoExistente + cantidad;
        if (totalAbonado >= 3000.0 && cotizacion.getEstado() != EstadoCotizacion.VIGENTE) {
            cotizacion.setEstado(EstadoCotizacion.VIGENTE);
            cotizacionRepository.save(cotizacion);

            Evento evento = cotizacion.getEvento();
            if (evento != null && evento.getEstado() == EstadoEvento.TENTATIVO) {
                evento.setEstado(EstadoEvento.CONFIRMADO);
                
                // Asignar horarios por defecto del turno si no los tiene
                if (evento.getTurno() == TurnoEvento.MATUTINO) {
                    if (evento.getHoraInicio() == null) evento.setHoraInicio(LocalTime.of(9, 0));
                    if (evento.getHoraFin() == null) evento.setHoraFin(LocalTime.of(14, 0));
                } else if (evento.getTurno() == TurnoEvento.VESPERTINO) {
                    if (evento.getHoraInicio() == null) evento.setHoraInicio(LocalTime.of(15, 0));
                    if (evento.getHoraFin() == null) evento.setHoraFin(LocalTime.of(20, 0));
                }
                eventoRepository.save(evento);
            }
        }
        return pagoGuardado;
    }

    public double obtenerTotalAbonado(Long cotizacionId) {
        List<Pago> pagosExistentes = pagoRepository.findByCotizacionId(cotizacionId);
        return pagosExistentes == null ? 0.0 : pagosExistentes.stream().mapToDouble(Pago::getCantidad).sum();
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