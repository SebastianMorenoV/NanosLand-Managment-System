package com.example.negocio.cotizacion.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.PagoDTO;
import com.mycompany.common.mapper.PagoMapper;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.dominio.Pago;
import com.mycompany.persistencia.enums.MetodoPago;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.EventoRepository;
import com.mycompany.persistencia.repository.PagoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RegistrarAnticipoUseCase {
    private final CotizacionRepository cotizacionRepository;
    private final EventoRepository eventoRepository;
    private final PagoRepository pagoRepository;

    // Inyectar Repositorios necesarios (PagoRepository, CotizacionRepository)

    @Transactional
    public void registrarAnticipo(Long cotizacionId, double monto, MetodoPago metodo, String referencia) {
        // 1. Buscar la cotización por ID
        Cotizacion cotizacion = cotizacionRepository.findById(cotizacionId)
                .orElseThrow(() -> new CotizacionException("Cotización no encontrada con ID: " + cotizacionId));

        // 2. Crear una nueva entidad de Pago
        Pago pago = new Pago();
        pago.setCantidad(monto);
        pago.setTipo(metodo);
        pago.setFolioPago(referencia);
        pago.setFechaHora(LocalDateTime.now());

        // 3. Asociar el Pago a la Cotización
        pago.setCotizacion(cotizacion);

        // 4. Cambiar el estado de la Cotización a VIGENTE
        if (cotizacion.getEstado() == EstadoCotizacion.BORRADOR) {
            cotizacion.setEstado(EstadoCotizacion.VIGENTE);
        }

        // 5. Crear el Evento asociado automáticamente
        // Se transfieren los datos clave de la cotización al nuevo evento
        Evento evento = new Evento();
        evento.setCotizacion(cotizacion);
        evento.setFecha(cotizacion.getFecha());
        evento.setTurno(cotizacion.getTurno());
        evento.setNotas(cotizacion.getNotas());

        // 6. Persistir los cambios en la base de datos
        pagoRepository.save(pago);
        eventoRepository.save(evento);
        cotizacionRepository.save(cotizacion);

        System.out.println("Anticipo registrado, estado actualizado a VIGENTE y evento creado para folio: " + cotizacion.getFolio());
    }

    @Transactional
    public void ejecutar(Long cotizacionId, PagoDTO pagoDTO) {
        // 1. Buscar la cotización
        Cotizacion cotizacion = cotizacionRepository.findById(cotizacionId)
                .orElseThrow(() -> new CotizacionException("Cotización no encontrada"));

        // 2. Cambiar estado de BORRADOR a VIGENTE
        if (cotizacion.getEstado() == EstadoCotizacion.BORRADOR) {
            cotizacion.setEstado(EstadoCotizacion.VIGENTE);
        }

        // 3. Crear el Evento asociado automáticamente
        Evento nuevoEvento = new Evento();
        nuevoEvento.setCotizacion(cotizacion);
        nuevoEvento.setFecha(cotizacion.getFecha()); // Usa la fecha de la cotización
        nuevoEvento.setTurno(cotizacion.getTurno());
        // Puedes definir horas por defecto según el turno si lo deseas
        eventoRepository.save(nuevoEvento);

        // 4. Registrar el Pago (Anticipo)
        Pago pago = PagoMapper.toEntity(pagoDTO);
        pago.setCotizacion(cotizacion);
        pago.setFechaHora(java.time.LocalDateTime.now());
        pagoRepository.save(pago);

        // 5. Guardar cambios en la cotización
        cotizacionRepository.save(cotizacion);
    }
}