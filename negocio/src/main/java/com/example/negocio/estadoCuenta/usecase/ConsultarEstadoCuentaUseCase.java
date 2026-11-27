package com.example.negocio.estadoCuenta.usecase;

import com.example.negocio.cotizacion.usecase.ExtrasCotizacion;
import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.CargoExtraDTO;
import com.mycompany.common.dtos.EstadoCuentaDTO;
import com.mycompany.common.dtos.PagoDTO;
import com.mycompany.common.dtos.ServicioExtraDTO;
import com.mycompany.common.mapper.CargoExtraMapper;
import com.mycompany.common.mapper.PagoMapper;
import com.mycompany.persistencia.dominio.CargoExtra;
import com.mycompany.persistencia.dominio.Cotizacion;
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

        // Paquete Base, con su comida y personas (el precio pactado se asigna abajo)
        if (cotizacion.getPaquete() != null) {
            dto.setNombrePaqueteBase(com.mycompany.common.util.DescripcionPaquete.de(
                    cotizacion.getPaquete().getNombre(),
                    cotizacion.getComida() != null ? cotizacion.getComida().getNombre() : null,
                    cotizacion.getPersonas()));
        } else {
            dto.setNombrePaqueteBase("Sin Paquete");
        }

        // Servicios Extras de la Cotización
        List<ServicioExtraDTO> extrasCotizacion = new ArrayList<>();
        double totalExtras = 0.0;
        // Un renglón por servicio: las cotizaciones viejas guardaban cada unidad aparte.
        for (ExtrasCotizacion.Renglon r : ExtrasCotizacion.paraMostrar(cotizacion.getServiciosExtra())) {
            ServicioExtraDTO sDto = new ServicioExtraDTO();
            sDto.setNombre(r.servicio() != null ? r.servicio().getNombre() : "Servicio Custom");
            sDto.setPrecioUnitario(r.precioUnitario());
            sDto.setCantidad(r.cantidad());
            sDto.setSubtotal(r.subtotal());
            extrasCotizacion.add(sDto);
            totalExtras += r.subtotal();
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
        // Se parte del total GUARDADO en la cotización (lo pactado con el cliente), no del
        // precio actual del paquete: así un cambio posterior en el catálogo no altera
        // lo que debe un evento ya vendido. El paquete se muestra a su precio pactado.
        if (cotizacion.getPaquete() != null) {
            var precio = com.example.negocio.precio.CalcularPrecioPaqueteUseCase.pactado(cotizacion);
            dto.setPrecioPaqueteBase(precio.precio());
            dto.setPrecioListaPaquete(precio.precioLista());
            dto.setDescuentoPaqueteNombre(precio.descuento());
            dto.setDescuentoPaqueteMonto(precio.montoDescuento());
        }
        double granTotal = cotizacion.getTotal() + totalCargos;
        dto.setGranTotal(granTotal);
        dto.setSaldoPendiente(granTotal - totalPagado);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<Evento> obtenerEventosCobranza() {
        // Un borrador (TENTATIVO) todavía no es una venta: se maneja desde Cotizaciones.
        List<Evento> eventos = eventoRepository.findByEstadoIn(List.of(
            com.mycompany.persistencia.enums.EstadoEvento.CONFIRMADO,
            com.mycompany.persistencia.enums.EstadoEvento.EN_CURSO,
            com.mycompany.persistencia.enums.EstadoEvento.FINALIZADO,
            com.mycompany.persistencia.enums.EstadoEvento.CANCELADO_TARDIO));
        eventos.forEach(e -> {
            if (e.getCotizacion() != null) {
                org.hibernate.Hibernate.initialize(e.getCotizacion());
                if (e.getCotizacion().getCliente() != null) {
                    org.hibernate.Hibernate.initialize(e.getCotizacion().getCliente());
                }
            }
        });
        return eventos;
    }
}
