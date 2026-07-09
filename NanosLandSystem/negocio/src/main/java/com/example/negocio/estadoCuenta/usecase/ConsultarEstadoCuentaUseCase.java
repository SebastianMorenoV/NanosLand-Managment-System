package com.example.negocio.estadoCuenta.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.CargoExtraDTO;
import com.mycompany.common.dtos.EstadoCuentaDTO;
import com.mycompany.common.dtos.PagoDTO;
import com.mycompany.common.dtos.ServicioExtraDTO;
import com.mycompany.common.mapper.CargoExtraMapper;
import com.mycompany.common.mapper.PagoMapper;
import com.mycompany.persistencia.dominio.CargoExtra;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.DetalleCotizacion;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.dominio.Pago;
import com.mycompany.persistencia.repository.CargoExtraRepository;
import com.mycompany.persistencia.repository.EventoRepository;
import com.mycompany.persistencia.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsultarEstadoCuentaUseCase {

    private final EventoRepository eventoRepository;
    private final PagoRepository pagoRepository;
    private final CargoExtraRepository cargoExtraRepository;

    @Transactional(readOnly = true)
    public EstadoCuentaDTO generarEstadoCuenta(Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new CotizacionException("No se encontró el evento con ID: " + eventoId));

        Cotizacion cotizacion = evento.getCotizacion();
        if (cotizacion == null) {
            throw new CotizacionException("El evento no tiene una cotización asociada.");
        }

        EstadoCuentaDTO dto = new EstadoCuentaDTO();
        dto.setEventoId(evento.getId());
        dto.setClienteNombre(cotizacion.getCliente().getNombre());
        dto.setFolioCotizacion(cotizacion.getFolio());
        if (evento.getFecha() != null) {
            dto.setFechaEventoFormateada(evento.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        if (evento.getEstado() != null) {
            dto.setEstadoEvento(evento.getEstado().name());
        }

        // Paquete Base
        if (cotizacion.getPaquete() != null) {
            dto.setNombrePaqueteBase(cotizacion.getPaquete().getNombre());
            dto.setPrecioPaqueteBase(cotizacion.getPaquete().getCosto());
        } else {
            dto.setNombrePaqueteBase("Sin Paquete");
            dto.setPrecioPaqueteBase(0.0);
        }

        // Servicios Extras de la Cotización
        List<ServicioExtraDTO> extrasCotizacion = new ArrayList<>();
        double totalExtras = 0.0;
        if (cotizacion.getServiciosExtra() != null) {
            for (DetalleCotizacion det : cotizacion.getServiciosExtra()) {
                ServicioExtraDTO sDto = new ServicioExtraDTO();
                if (det.getServicio() != null) {
                    sDto.setNombre(det.getServicio().getNombre());
                } else {
                    sDto.setNombre("Servicio Custom");
                }
                sDto.setPrecioUnitario(det.getPrecioUnitario());
                sDto.setCantidad(det.getCantidad());
                sDto.setSubtotal(det.getSubtotal());
                extrasCotizacion.add(sDto);
                totalExtras += det.getSubtotal();
            }
        }
        dto.setServiciosExtrasOriginales(extrasCotizacion);
        dto.setTotalServiciosExtrasOriginales(totalExtras);

        // Cargos Extras añadidos posteriormente
        List<CargoExtra> cargos = cargoExtraRepository.findByEventoId(evento.getId());
        dto.setCargosExtras(CargoExtraMapper.toDTOList(cargos));
        double totalCargos = cargos.stream().mapToDouble(CargoExtra::getSubtotal).sum();
        dto.setTotalCargosExtras(totalCargos);

        // Pagos realizados
        List<Pago> pagos = pagoRepository.findByCotizacionId(cotizacion.getId());
        dto.setPagosRealizados(PagoMapper.toDTOList(pagos));
        double totalPagado = pagos.stream().mapToDouble(Pago::getCantidad).sum();
        dto.setTotalPagado(totalPagado);

        // Totales Consolidados
        double granTotal = dto.getPrecioPaqueteBase() + totalExtras + totalCargos;
        dto.setGranTotal(granTotal);
        dto.setSaldoPendiente(granTotal - totalPagado);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<Evento> obtenerEventosCobranza() {
        return eventoRepository.findByEstadoNot(com.mycompany.persistencia.enums.EstadoEvento.CANCELADO);
    }
}
