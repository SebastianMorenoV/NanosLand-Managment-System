package com.example.negocio.cotizacion.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.dominio.Pago;
import com.mycompany.persistencia.enums.EstadoCotizacion;
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

        // Si la suma de pagos alcanza o supera los $3,000, Confirmamos y creamos el Evento
        double totalAbonado = anticipoExistente + cantidad;
        if (totalAbonado >= 3000.0 && cotizacion.getEstado() != EstadoCotizacion.VIGENTE) {
            cotizacion.setEstado(EstadoCotizacion.VIGENTE);
            cotizacionRepository.save(cotizacion);

            if (eventoRepository.findByCotizacionId(cotizacion.getId()).isEmpty()) {
                Evento evento = new Evento();
                evento.setCotizacion(cotizacion);
                evento.setFecha(cotizacion.getFecha());
                evento.setTurno(cotizacion.getTurno());
                evento.setNotas(cotizacion.getNotas());
                
                // Asignar horarios por defecto del turno
                if (cotizacion.getTurno() == TurnoEvento.MATUTINO) {
                    evento.setHoraInicio(LocalTime.of(9, 0));
                    evento.setHoraFin(LocalTime.of(14, 0));
                } else {
                    evento.setHoraInicio(LocalTime.of(15, 0));
                    evento.setHoraFin(LocalTime.of(20, 0));
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

    private String generarFolioPago() {
        int anio = Year.now().getValue();
        long base = pagoRepository.count() + 1;
        return String.format("PAY-%d-%04d", anio, base);
    }
}