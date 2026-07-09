package com.example.negocio.reporte.usecase;

import com.mycompany.common.dtos.OportunidadDTO;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GenerarReporteOportunidadesUseCase {

    private final EventoRepository eventoRepository;

    public List<OportunidadDTO> generarReporte(int year, int month) {
        // Buscamos los eventos de hace exactamente un año (o el año pasado en ese mes)
        // Pero la lógica de recompra suele ser: "En este mes (ej. Julio), ¿quiénes tuvieron evento en Julio del año pasado?"
        // Así que el year y month ingresados serán los del evento original (ej. 2025, Julio).
        
        YearMonth ym = YearMonth.of(year, month);
        LocalDate inicio = ym.atDay(1);
        LocalDate fin = ym.atEndOfMonth();

        // Solo nos interesan eventos FINALIZADOS (clientes satisfechos)
        List<Evento> eventosPasados = eventoRepository.findByEstadoAndFechaBetween(EstadoEvento.FINALIZADO, inicio, fin);
        
        List<OportunidadDTO> oportunidades = new ArrayList<>();
        
        for (Evento e : eventosPasados) {
            OportunidadDTO dto = new OportunidadDTO();
            
            if (e.getCotizacion() != null) {
                if (e.getCotizacion().getCliente() != null) {
                    dto.setClienteNombre(e.getCotizacion().getCliente().getNombre());
                    dto.setClienteTelefono(e.getCotizacion().getCliente().getTelefono());
                } else {
                    dto.setClienteNombre("Desconocido");
                    dto.setClienteTelefono("N/A");
                }
                
                if (e.getCotizacion().getPaquete() != null) {
                    dto.setNombrePaquete(e.getCotizacion().getPaquete().getNombre());
                } else {
                    dto.setNombrePaquete("Sin paquete");
                }
                
                // Un aproximado del monto que gastó la vez pasada (solo cotización base)
                dto.setMontoGastado(e.getCotizacion().getTotal());
            }
            
            dto.setFechaEventoPasado(e.getFecha());
            
            oportunidades.add(dto);
        }
        
        // Ordenar por fecha del evento pasado
        oportunidades.sort((a, b) -> a.getFechaEventoPasado().compareTo(b.getFechaEventoPasado()));
        
        return oportunidades;
    }
}
