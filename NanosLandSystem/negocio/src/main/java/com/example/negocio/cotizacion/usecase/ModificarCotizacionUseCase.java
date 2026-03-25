/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.negocio.cotizacion.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.CotizacionDTO;
import com.mycompany.common.dtos.DetalleCotizacionDTO;
import com.mycompany.common.mapper.CotizacionMapper;
import com.mycompany.persistencia.dominio.Cliente;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.DetalleCotizacion;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.persistencia.repository.ClienteRepository;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.PaqueteRepository;
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
        boolean cambioFechaTurno = existente.getFecha() == null
            || existente.getTurno() == null
            || !existente.getFecha().equals(fechaNueva)
            || !existente.getTurno().equals(turnoNuevo);

        if (cambioFechaTurno) {
            // Solo bloqueamos si la nueva fecha/turno ya tiene un evento VIGENTE
            boolean turnoOcupado = cotizacionRepository.existsByFechaAndTurnoAndEstadoNotIn(
                fechaNueva,
                turnoNuevo,
                List.of(EstadoCotizacion.BORRADOR, EstadoCotizacion.CANCELADA, EstadoCotizacion.ELIMINADA)
            );
            if (turnoOcupado) {
                throw new CotizacionException("Ya existe un evento confirmado para esta fecha y turno. Seleccione otro turno.");
            }
        }
        // ── Actualización de campos base ───────────────────────────────────
        existente.setCliente(cliente);
        existente.setPaquete(paquete);
        existente.setFecha(dto.getFecha());
        existente.setTurno(dto.getTurno());
        existente.setNotas(dto.getNotas());
        existente.setNombreFestejado(dto.getNombreFestejado());
        existente.setTematica(dto.getTematica());

        if (dto.getEstado() != null) {
            existente.setEstado(dto.getEstado());
        }

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
        
        existente.setTotal(total);

        Cotizacion guardada = cotizacionRepository.save(existente);
        return CotizacionMapper.toDTO(guardada);
    }
}
