package com.example.negocio.reporte.usecase;

import com.mycompany.common.dtos.IngresoDTO;
import com.mycompany.common.dtos.VentaProductoDTO;
import com.mycompany.common.mapper.VentaMapper;
import com.mycompany.persistencia.dominio.DetalleVenta;
import com.mycompany.persistencia.dominio.Pago;
import com.mycompany.persistencia.dominio.Venta;
import com.mycompany.persistencia.enums.EstadoVenta;
import com.mycompany.persistencia.repository.PagoRepository;
import com.mycompany.persistencia.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GenerarReporteIngresosUseCase {

    private final PagoRepository pagoRepository;
    private final VentaRepository ventaRepository;

    /** Dinero que entró en el periodo: pagos de eventos y ventas extra no canceladas. */
    @Transactional(readOnly = true)
    public List<IngresoDTO> generarReporteIngresos(LocalDate inicio, LocalDate fin) {
        LocalDateTime fechaInicio = inicio != null ? inicio.atStartOfDay() : LocalDateTime.of(1900, 1, 1, 0, 0);
        if (inicio != null && fin != null && inicio.isAfter(fin)) {
            throw new com.example.negocio.exception.CotizacionException(
                "La fecha de inicio no puede ser posterior a la fecha final.");
        }
        // Hasta el último instante del día final (los pagos se guardan con fracciones de segundo).
        LocalDateTime fechaFin = fin != null
            ? fin.plusDays(1).atStartOfDay().minusNanos(1000)
            : LocalDateTime.now().plusYears(100);

        List<Pago> pagos = pagoRepository.findByFechaHoraBetween(fechaInicio, fechaFin);
        List<IngresoDTO> ingresos = new ArrayList<>();

        for (Pago p : pagos) {
            IngresoDTO dto = new IngresoDTO();
            dto.setOrigen(IngresoDTO.ORIGEN_EVENTO);
            dto.setFolioPago(p.getFolioPago());
            dto.setFechaHora(p.getFechaHora());
            dto.setTipo(p.getTipo() != null ? p.getTipo().name() : "N/A");
            dto.setCantidad(p.getCantidad());

            if (p.getCotizacion() != null) {
                dto.setFolioCotizacion(p.getCotizacion().getFolio());
                if (p.getCotizacion().getCliente() != null) {
                    dto.setClienteNombre(p.getCotizacion().getCliente().getNombre());
                } else {
                    dto.setClienteNombre("Sin Cliente");
                }
            } else {
                dto.setFolioCotizacion("N/A");
                dto.setClienteNombre("N/A");
            }

            ingresos.add(dto);
        }

        for (Venta v : ventasRegistradas(inicio, fin)) {
            IngresoDTO dto = new IngresoDTO();
            dto.setOrigen(IngresoDTO.ORIGEN_VENTA_EXTRA);
            dto.setFolioPago(v.getFolio());
            dto.setFechaHora(v.getFechaHora());
            dto.setTipo(v.getMetodoPago().name());
            dto.setCantidad(v.getTotal());
            dto.setFolioCotizacion("-");
            dto.setClienteNombre(VentaMapper.toDTO(v).getResumen());
            ingresos.add(dto);
        }

        // Ordenar por fecha descendente
        ingresos.sort((a, b) -> b.getFechaHora().compareTo(a.getFechaHora()));

        return ingresos;
    }

    /** Cuánto se vendió de cada producto en el periodo; primero lo que más dinero dejó. */
    @Transactional(readOnly = true)
    public List<VentaProductoDTO> generarVentasPorProducto(LocalDate inicio, LocalDate fin) {
        Map<Long, VentaProductoDTO> porProducto = new LinkedHashMap<>();
        for (Venta venta : ventasRegistradas(inicio, fin)) {
            for (DetalleVenta detalle : venta.getDetalles()) {
                VentaProductoDTO dto = porProducto.computeIfAbsent(detalle.getProducto().getId(), id -> {
                    VentaProductoDTO nuevo = new VentaProductoDTO();
                    nuevo.setNombre(detalle.getProducto().getNombre());
                    return nuevo;
                });
                dto.setCantidad(dto.getCantidad() + detalle.getCantidad());
                dto.setTotal(Math.round((dto.getTotal() + detalle.getSubtotal()) * 100.0) / 100.0);
            }
        }
        List<VentaProductoDTO> resultado = new ArrayList<>(porProducto.values());
        resultado.sort(Comparator.comparingDouble(VentaProductoDTO::getTotal).reversed());
        return resultado;
    }

    private List<Venta> ventasRegistradas(LocalDate inicio, LocalDate fin) {
        LocalDateTime desde = inicio != null ? inicio.atStartOfDay() : LocalDateTime.of(1900, 1, 1, 0, 0);
        LocalDateTime hasta = fin != null ? fin.plusDays(1).atStartOfDay() : LocalDateTime.now().plusYears(100);
        return ventaRepository.findConDetallesEntre(desde, hasta).stream()
            .filter(v -> v.getEstado() == EstadoVenta.REGISTRADA)
            .toList();
    }
}
