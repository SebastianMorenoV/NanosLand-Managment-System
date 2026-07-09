package com.example.negocio.reporte.usecase;

import com.example.negocio.estadoCuenta.usecase.ConsultarEstadoCuentaUseCase;
import com.mycompany.common.dtos.AdeudoDTO;
import com.mycompany.common.dtos.EstadoCuentaDTO;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GenerarReporteAdeudosUseCase {

    private final EventoRepository eventoRepository;
    private final ConsultarEstadoCuentaUseCase consultarEstadoCuentaUseCase;

    public List<AdeudoDTO> generarReporteAdeudos() {
        // Obtenemos eventos que no estén cancelados
        List<Evento> eventosActivos = eventoRepository.findByEstadoNot(EstadoEvento.CANCELADO);
        List<AdeudoDTO> adeudos = new ArrayList<>();

        for (Evento evento : eventosActivos) {
            try {
                EstadoCuentaDTO estado = consultarEstadoCuentaUseCase.generarEstadoCuenta(evento.getId());
                
                // Si tiene saldo pendiente, lo agregamos a la lista de adeudos
                if (estado.getSaldoPendiente() > 0) {
                    AdeudoDTO adeudo = new AdeudoDTO();
                    adeudo.setEventoId(evento.getId());
                    adeudo.setFolioCotizacion(estado.getFolioCotizacion());
                    adeudo.setClienteNombre(estado.getClienteNombre());
                    
                    // Extraer teléfono del cliente si existe
                    if (evento.getCotizacion() != null && evento.getCotizacion().getCliente() != null) {
                        adeudo.setClienteTelefono(evento.getCotizacion().getCliente().getTelefono());
                    } else {
                        adeudo.setClienteTelefono("N/A");
                    }
                    
                    adeudo.setFechaEvento(evento.getFecha());
                    adeudo.setEstadoEvento(estado.getEstadoEvento());
                    
                    adeudo.setGranTotal(estado.getGranTotal());
                    adeudo.setTotalPagado(estado.getTotalPagado());
                    adeudo.setSaldoPendiente(estado.getSaldoPendiente());
                    
                    adeudos.add(adeudo);
                }
            } catch (Exception ex) {
                // Si hay error calculando un estado, simplemente se omite para no romper todo el reporte
                System.err.println("Error procesando adeudo para evento " + evento.getId() + ": " + ex.getMessage());
            }
        }
        
        // Ordenar por fecha del evento ascendente (los más antiguos primero)
        adeudos.sort((a, b) -> {
            if (a.getFechaEvento() == null) return 1;
            if (b.getFechaEvento() == null) return -1;
            return a.getFechaEvento().compareTo(b.getFechaEvento());
        });

        return adeudos;
    }
}
