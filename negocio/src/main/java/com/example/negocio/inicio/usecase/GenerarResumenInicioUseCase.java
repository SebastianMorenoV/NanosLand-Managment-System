package com.example.negocio.inicio.usecase;

import com.example.negocio.cotizacion.usecase.RegistrarAnticipoUseCase;
import com.example.negocio.evento.usecase.ActualizarEstadosPorFechaUseCase;
import com.example.negocio.reporte.usecase.GenerarReporteAdeudosUseCase;
import com.example.negocio.reporte.usecase.GenerarReporteIngresosUseCase;
import com.mycompany.common.dtos.AdeudoDTO;
import com.mycompany.common.dtos.IngresoDTO;
import com.mycompany.common.dtos.ReporteLogisticaSemanalDTO;
import com.mycompany.common.dtos.ResumenInicioDTO;
import com.mycompany.common.dtos.ResumenInicioDTO.TipoPendiente;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.enums.EstadoLogistica;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.EventoRepository;
import com.mycompany.persistencia.repository.LogisticaServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Arma la pantalla de Inicio: qué hay hoy, qué viene en los próximos 7 días y
 * qué requiere atención (cobrar, confirmar o contactar servicios).
 */
@Service
@RequiredArgsConstructor
public class GenerarResumenInicioUseCase {

    private static final int DIAS_PROXIMOS = 7;

    private final EventoRepository eventoRepository;
    private final CotizacionRepository cotizacionRepository;
    private final LogisticaServicioRepository logisticaRepository;
    private final RegistrarAnticipoUseCase registrarAnticipoUseCase;
    private final GenerarReporteIngresosUseCase generarReporteIngresosUseCase;
    private final GenerarReporteAdeudosUseCase generarReporteAdeudosUseCase;
    private final ActualizarEstadosPorFechaUseCase actualizarEstadosPorFechaUseCase;

    /**
     * @param incluirDinero {@code true} solo para el dueño: ingresos del mes y total por cobrar.
     */
    @Transactional(readOnly = true)
    public ResumenInicioDTO generar(boolean incluirDinero) {
        actualizarEstadosPorFechaUseCase.actualizar();

        LocalDate hoy = LocalDate.now();
        ResumenInicioDTO resumen = new ResumenInicioDTO();
        resumen.setCotizacionesPorConfirmar((int) cotizacionRepository
            .contarPorEstadoDesde(EstadoCotizacion.BORRADOR, hoy));

        List<Evento> eventos = new ArrayList<>(
            eventoRepository.findByFechaBetweenWithRelations(hoy, hoy.plusDays(DIAS_PROXIMOS)));
        eventos.removeIf(e -> e.getCotizacion() == null);
        eventos.sort(Comparator
            .comparing(Evento::getFecha)
            .thenComparing(e -> e.getTurno() == null ? Integer.MAX_VALUE : e.getTurno().ordinal())
            .thenComparing(e -> e.getHoraInicio() == null ? LocalTime.MAX : e.getHoraInicio())
            .thenComparing(Evento::getId));

        for (Evento evento : eventos) {
            Cotizacion cotizacion = evento.getCotizacion();
            if (evento.getEstado() == EstadoEvento.CONFIRMADO || evento.getEstado() == EstadoEvento.EN_CURSO) {
                agregarEventoConfirmado(resumen, evento, cotizacion, hoy);
            } else if (evento.getEstado() == EstadoEvento.TENTATIVO
                    && cotizacion.getEstado() == EstadoCotizacion.BORRADOR) {
                resumen.getPendientes().add(pendiente(TipoPendiente.CONFIRMAR, evento, "Falta confirmar"));
            }
        }

        if (incluirDinero) {
            resumen.setIngresosMes(generarReporteIngresosUseCase
                .generarReporteIngresos(hoy.withDayOfMonth(1), hoy).stream()
                .mapToDouble(IngresoDTO::getCantidad).sum());
            resumen.setPorCobrar(generarReporteAdeudosUseCase.generarReporteAdeudos().stream()
                .mapToDouble(AdeudoDTO::getSaldoPendiente).sum());
        }
        return resumen;
    }

    private void agregarEventoConfirmado(ResumenInicioDTO resumen, Evento evento, Cotizacion cotizacion, LocalDate hoy) {
        if (evento.getFecha().equals(hoy)) {
            resumen.setEventosHoy(resumen.getEventosHoy() + 1);
        } else {
            resumen.setEventosProximos(resumen.getEventosProximos() + 1);
        }

        ResumenInicioDTO.EventoProximo proximo = new ResumenInicioDTO.EventoProximo();
        proximo.setCotizacionId(cotizacion.getId());
        proximo.setFecha(evento.getFecha());
        proximo.setHorario(horario(evento));
        proximo.setDescripcion(descripcion(evento));
        resumen.getEventos().add(proximo);

        double saldo = registrarAnticipoUseCase.obtenerSaldoPendiente(cotizacion.getId());
        if (saldo > 0.005) {
            resumen.getPendientes().add(pendiente(TipoPendiente.COBRAR, evento,
                String.format("Cobrar $%,.2f", saldo)));
        }

        int porContactar = (int) logisticaRepository.countByEventoIdAndEstado(
            evento.getId(), EstadoLogistica.POR_CONTACTAR);
        if (porContactar > 0) {
            resumen.setServiciosPorContactar(resumen.getServiciosPorContactar() + porContactar);
            resumen.getPendientes().add(pendiente(TipoPendiente.CONTACTAR, evento,
                porContactar + (porContactar == 1 ? " servicio por contactar" : " servicios por contactar")));
        }
    }

    private static ResumenInicioDTO.Pendiente pendiente(TipoPendiente tipo, Evento evento, String titulo) {
        ResumenInicioDTO.Pendiente pendiente = new ResumenInicioDTO.Pendiente();
        pendiente.setTipo(tipo);
        pendiente.setCotizacionId(evento.getCotizacion().getId());
        pendiente.setFecha(evento.getFecha());
        pendiente.setTitulo(titulo);
        pendiente.setDetalle(descripcion(evento));
        return pendiente;
    }

    private static String descripcion(Evento evento) {
        String festejado = evento.getNombreFestejado();
        String fiesta = festejado == null || festejado.isBlank() ? "Fiesta" : "Fiesta de " + festejado.trim();
        var cliente = evento.getCotizacion().getCliente();
        return cliente == null ? fiesta : fiesta + " · " + cliente.getNombre();
    }

    private static String horario(Evento evento) {
        if (evento.getHoraInicio() != null && evento.getHoraFin() != null) {
            return ReporteLogisticaSemanalDTO.horaTexto(evento.getHoraInicio())
                + " a " + ReporteLogisticaSemanalDTO.horaTexto(evento.getHoraFin());
        }
        return evento.getTurno() == TurnoEvento.MATUTINO ? "Turno de la mañana" : "Turno de la tarde";
    }
}
