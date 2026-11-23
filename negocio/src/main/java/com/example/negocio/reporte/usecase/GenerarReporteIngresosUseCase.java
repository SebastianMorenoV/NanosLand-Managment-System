package com.example.negocio.reporte.usecase;

import com.mycompany.common.dtos.IngresoDTO;
import com.mycompany.persistencia.dominio.Pago;
import com.mycompany.persistencia.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GenerarReporteIngresosUseCase {

    private final PagoRepository pagoRepository;

    @Transactional(readOnly = true)
    public List<IngresoDTO> generarReporteIngresos(LocalDate inicio, LocalDate fin) {
        LocalDateTime fechaInicio = inicio != null ? inicio.atStartOfDay() : LocalDateTime.of(1900, 1, 1, 0, 0);
        LocalDateTime fechaFin = fin != null ? fin.atTime(23, 59, 59) : LocalDateTime.now().plusYears(100);

        List<Pago> pagos = pagoRepository.findByFechaHoraBetween(fechaInicio, fechaFin);
        List<IngresoDTO> ingresos = new ArrayList<>();

        for (Pago p : pagos) {
            IngresoDTO dto = new IngresoDTO();
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
        
        // Ordenar por fecha descendente
        ingresos.sort((a, b) -> b.getFechaHora().compareTo(a.getFechaHora()));
        
        return ingresos;
    }
}
