/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.negocio.cotizacion.usecase;

import com.example.negocio.exception.CotizacionException;
import com.example.negocio.logistica.usecase.GenerarLogisticaEventoUseCase;
import com.mycompany.common.dtos.CotizacionDTO;
import com.mycompany.common.dtos.DetalleCotizacionDTO;
import com.mycompany.common.mapper.CotizacionMapper;
import com.mycompany.persistencia.dominio.Cliente;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.DetalleCotizacion;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.persistencia.repository.ClienteRepository;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.EventoRepository;
import com.mycompany.persistencia.repository.PaqueteRepository;
import com.mycompany.persistencia.repository.PagoRepository;
import com.mycompany.persistencia.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author skyro
 */
@Service
@RequiredArgsConstructor
public class ModificarCotizacionUseCase {

    private final CotizacionRepository cotizacionRepository;
    private final ClienteRepository clienteRepository;
    private final PaqueteRepository paqueteRepository;
    private final ServicioRepository servicioRepository;
    private final EventoRepository eventoRepository;
    private final PagoRepository pagoRepository;
    private final GenerarLogisticaEventoUseCase generarLogisticaEventoUseCase;

    @Transactional
    public CotizacionDTO modificarCotizacion(Long cotizacionId, CotizacionDTO dto) {
        if (cotizacionId == null) {
            throw new CotizacionException("Debe indicar la cotización a modificar.");
        }
        if (dto == null) {
            throw new CotizacionException("El objeto de cotización no puede ser nulo.");
        }

        Cotizacion existente = cotizacionRepository.findById(cotizacionId)
            .orElseThrow(() -> new CotizacionException("No existe la cotización indicada."));

        if (existente.getEstado() == EstadoCotizacion.CANCELADA
                || existente.getEstado() == EstadoCotizacion.ELIMINADA) {
            throw new CotizacionException("La cotización está cancelada y ya no se puede modificar.");
        }

        // ── Validaciones de campos obligatorios ─────────────────────────────
        if (dto.getClienteId() == null) {
            throw new CotizacionException("Debe seleccionar un cliente para la cotización.");
        }
        if (dto.getFecha() == null) {
            throw new CotizacionException("Debe seleccionar una fecha para el evento.");
        }
        if (dto.getTurno() == null) {
            throw new CotizacionException("Debe seleccionar un turno (Matutino o Vespertino).");
        }
        if (dto.getPaqueteId() == null) {
            throw new CotizacionException("Debe seleccionar un paquete base para la cotización.");
        }

        if (dto.getFecha().isBefore(LocalDate.now())) {
            throw new CotizacionException("La fecha del evento no puede ser en el pasado. Seleccione una fecha válida.");
        }

        Cliente cliente = clienteRepository.findById(dto.getClienteId())
            .orElseThrow(() -> new CotizacionException(
                "El cliente con ID " + dto.getClienteId() + " no existe en el sistema."
            ));

        Paquete paquete = paqueteRepository.findById(dto.getPaqueteId())
            .orElseThrow(() -> new CotizacionException(
                "El paquete con ID " + dto.getPaqueteId() + " no existe en el catálogo."
            ));

        // ── Validación: no duplicar turno (excluyendo la cotización actual) ─
        LocalDate fechaNueva = dto.getFecha();
        TurnoEvento turnoNuevo = dto.getTurno();
        
        var eventoExistente = existente.getEvento();
        if (eventoExistente == null) {
            throw new CotizacionException("La cotización no tiene un evento asociado válido.");
        }

        boolean cambioFechaTurno = eventoExistente.getFecha() == null
            || eventoExistente.getTurno() == null
            || !eventoExistente.getFecha().equals(fechaNueva)
            || !eventoExistente.getTurno().equals(turnoNuevo);

        if (cambioFechaTurno) {
            // Solo bloqueamos si la nueva fecha/turno ya tiene OTRO evento confirmado
            // (los borradores de otros clientes no apartan el turno).
            boolean turnoOcupado = eventoRepository.existsByFechaAndTurnoAndEstadoInAndIdNot(
                fechaNueva, turnoNuevo,
                com.example.negocio.agenda.usecase.VerificarDisponibilidadTurnoUseCase.ESTADOS_QUE_OCUPAN,
                eventoExistente.getId());

            if (turnoOcupado) {
                throw new CotizacionException("Ya existe un evento confirmado para esta fecha y turno. Seleccione otro turno.");
            }
        }
        // ── Actualización de campos base ───────────────────────────────────
        existente.setCliente(cliente);
        existente.setPaquete(paquete);
        
        boolean cambioTurno = eventoExistente.getTurno() != dto.getTurno();
        eventoExistente.setFecha(dto.getFecha());
        eventoExistente.setTurno(dto.getTurno());
        // Un evento que ya tiene horario (confirmado) toma el del nuevo turno.
        if (cambioTurno && eventoExistente.getHoraInicio() != null) {
            com.example.negocio.evento.usecase.HorarioTurno.asignar(eventoExistente);
        }
        eventoExistente.setNombreFestejado(dto.getNombreFestejado());
        eventoExistente.setTematica(dto.getTematica());
        
        existente.setNotas(dto.getNotas());

        // El estado NO se toma del DTO: solo cambia al registrar pagos (VIGENTE)
        // o al cancelar/eliminar. Así una cotización ya pagada no puede volver
        // a BORRADOR porque la pantalla tenga un estado desactualizado.

        // ── Persistencia de servicios extra + recálculo total ────────────
        List<DetalleCotizacionDTO> detallesDTO = dto.getDetalles();
        List<DetalleCotizacion> detallesEntidad = new ArrayList<>();
        double total = paquete.getCosto();

        if (detallesDTO != null && !detallesDTO.isEmpty()) {
            for (DetalleCotizacionDTO detalleDTO : detallesDTO) {
                if (detalleDTO == null) continue;
                if (detalleDTO.getServicioId() == null) {
                    throw new CotizacionException("Un servicio extra no tiene id asociado.");
                }

                var servicio = servicioRepository.findById(detalleDTO.getServicioId())
                    .orElseThrow(() -> new CotizacionException(
                        "El servicio con ID " + detalleDTO.getServicioId() + " no existe en el catálogo."
                    ));

                int cantidad = detalleDTO.getCantidad() > 0 ? detalleDTO.getCantidad() : 1;
                double precioUnitario = detalleDTO.getPrecioUnitario();
                double subtotal = detalleDTO.getSubtotal();
                if (subtotal <= 0) {
                    subtotal = precioUnitario * cantidad;
                }

                DetalleCotizacion detalle = new DetalleCotizacion();
                detalle.setServicio(servicio);
                detalle.setCotizacion(existente);
                detalle.setPrecioUnitario(precioUnitario);
                detalle.setCantidad(cantidad);
                detalle.setSubtotal(subtotal);
                detalle.setHoraSugerida(detalleDTO.getHoraSugerida());
                detalle.setEspecificacionesCliente(detalleDTO.getEspecificacionesCliente());
                detalle.setDesgloseOpciones(detalleDTO.getDesgloseOpciones());
                detalle.setUbicacionMontaje(detalleDTO.getUbicacionMontaje());

                detallesEntidad.add(detalle);
                total += subtotal;
            }
        }

        // Limpiar y agregar a la colección existente en lugar de reemplazarla (Evita el error de orphan deletion)
        if (existente.getServiciosExtra() != null) {
            existente.getServiciosExtra().clear();
            existente.getServiciosExtra().addAll(detallesEntidad);
        } else {
            existente.setServiciosExtra(detallesEntidad);
        }
        
        // Validación para cotizaciones Vigentes (que no baje del anticipo mínimo ni de lo ya abonado)
        if (existente.getEstado() == EstadoCotizacion.VIGENTE) {
            List<com.mycompany.persistencia.dominio.Pago> pagosExistentes = pagoRepository.findByCotizacionId(cotizacionId);
            double anticipoExistente = pagosExistentes == null ? 0.0 : pagosExistentes.stream().mapToDouble(com.mycompany.persistencia.dominio.Pago::getCantidad).sum();
            
            double minimoRequerido = Math.max(RegistrarAnticipoUseCase.anticipoMinimo(total), anticipoExistente);
            if (total < minimoRequerido) {
                throw new CotizacionException(String.format("El nuevo total de la cotización ($%,.2f) no puede ser menor al monto ya abonado o requerido ($%,.2f).", total, minimoRequerido));
            }
        }
        
        existente.setTotal(total);

        // Si el evento ya está confirmado, su logística sigue a los servicios contratados:
        // se agregan los nuevos y se quitan los que salieron de la cotización.
        generarLogisticaEventoUseCase.sincronizar(eventoExistente, false);

        Cotizacion guardada = cotizacionRepository.save(existente);
        return CotizacionMapper.toDTO(guardada);
    }
}
