package com.example.negocio.logistica.usecase;

import com.example.negocio.evento.usecase.ActualizarEstadosPorFechaUseCase;
import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.ReporteLogisticaSemanalDTO;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.dominio.LogisticaServicio;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Genera el reporte semanal de logística: para la semana (lunes a domingo) de una
 * fecha dada, qué se ocupa cada día, evento por evento, y el consolidado diario.
 *
 * Incluye los eventos confirmados, en curso y finalizados. Los finalizados entran
 * para que el reporte de la semana en curso no pierda los días que ya pasaron; los
 * borradores sin anticipo y los cancelados no aparecen.
 */
@Service
@RequiredArgsConstructor
public class GenerarReporteLogisticaSemanalUseCase {

    private static final List<EstadoEvento> ESTADOS_INCLUIDOS =
        List.of(EstadoEvento.CONFIRMADO, EstadoEvento.EN_CURSO, EstadoEvento.FINALIZADO);

    private final EventoRepository eventoRepository;
    private final GenerarLogisticaEventoUseCase generarLogisticaEventoUseCase;
    private final ActualizarEstadosPorFechaUseCase actualizarEstadosPorFechaUseCase;

    /**
     * @param fechaDeLaSemana cualquier día de la semana que se quiere consultar.
     */
    @Transactional
    public ReporteLogisticaSemanalDTO generar(LocalDate fechaDeLaSemana) {
        if (fechaDeLaSemana == null) {
            throw new CotizacionException("Debe indicar la semana del reporte.");
        }
        // Pone al día los estados (en curso / finalizado) según la fecha de hoy.
        actualizarEstadosPorFechaUseCase.actualizar();

        LocalDate lunes = fechaDeLaSemana.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate domingo = lunes.plusDays(6);

        ReporteLogisticaSemanalDTO reporte = new ReporteLogisticaSemanalDTO();
        reporte.setInicio(lunes);
        reporte.setFin(domingo);

        Map<LocalDate, ReporteLogisticaSemanalDTO.Dia> dias = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            ReporteLogisticaSemanalDTO.Dia dia = new ReporteLogisticaSemanalDTO.Dia();
            dia.setFecha(lunes.plusDays(i));
            dias.put(dia.getFecha(), dia);
        }

        List<Evento> eventos = new ArrayList<>(eventoRepository.findByFechaBetweenWithRelations(lunes, domingo));
        eventos.removeIf(e -> !ESTADOS_INCLUIDOS.contains(e.getEstado()) || e.getCotizacion() == null);
        // Dentro de cada día: primero el turno matutino, y por hora de inicio.
        eventos.sort(Comparator
            .comparing((Evento e) -> e.getTurno() == null ? Integer.MAX_VALUE : e.getTurno().ordinal())
            .thenComparing(e -> e.getHoraInicio() == null ? LocalTime.MAX : e.getHoraInicio())
            .thenComparing(Evento::getId));

        int totalServicios = 0;
        for (Evento evento : eventos) {
            ReporteLogisticaSemanalDTO.Dia dia = dias.get(evento.getFecha());
            if (dia == null) {
                continue;
            }
            ReporteLogisticaSemanalDTO.EventoLogistica dto = aDTO(evento);
            dia.getEventos().add(dto);
            for (ReporteLogisticaSemanalDTO.Servicio s : dto.getServicios()) {
                totalServicios += s.getCantidad();
            }
        }

        for (ReporteLogisticaSemanalDTO.Dia dia : dias.values()) {
            dia.setResumen(consolidar(dia.getEventos()));
            reporte.getDias().add(dia);
        }
        reporte.setTotalEventos(eventos.size());
        reporte.setTotalServicios(totalServicios);
        return reporte;
    }

    private ReporteLogisticaSemanalDTO.EventoLogistica aDTO(Evento evento) {
        Cotizacion cotizacion = evento.getCotizacion();
        ReporteLogisticaSemanalDTO.EventoLogistica dto = new ReporteLogisticaSemanalDTO.EventoLogistica();
        dto.setEventoId(evento.getId());
        dto.setFolio(cotizacion.getFolio());
        if (cotizacion.getCliente() != null) {
            dto.setCliente(cotizacion.getCliente().getNombre());
            dto.setTelefono(cotizacion.getCliente().getTelefono());
        }
        dto.setFestejado(evento.getNombreFestejado());
        dto.setTematica(evento.getTematica());
        dto.setPaquete(cotizacion.getPaquete() != null ? cotizacion.getPaquete().getNombre() : null);
        dto.setNotas(evento.getNotas() != null && !evento.getNotas().isBlank()
            ? evento.getNotas() : cotizacion.getNotas());
        dto.setTurno(evento.getTurno());
        dto.setHoraInicio(evento.getHoraInicio());
        dto.setHoraFin(evento.getHoraFin());
        dto.setEstado(evento.getEstado());

        // Cantidad contratada de cada servicio (paquete + extras).
        Map<Long, GenerarLogisticaEventoUseCase.Contratado> contratados =
            generarLogisticaEventoUseCase.serviciosContratados(cotizacion);

        // Un evento confirmado antes de que existiera la bitácora automática la recibe aquí.
        generarLogisticaEventoUseCase.sincronizar(evento, true);
        List<LogisticaServicio> bitacora = evento.getLogisticaServicios();

        List<ReporteLogisticaSemanalDTO.Servicio> servicios = new ArrayList<>();
        if (bitacora != null && !bitacora.isEmpty()) {
            for (LogisticaServicio registro : bitacora) {
                ReporteLogisticaSemanalDTO.Servicio s = new ReporteLogisticaSemanalDTO.Servicio();
                Long idServicio = registro.getServicio() != null ? registro.getServicio().getId() : null;
                s.setNombre(registro.getServicio() != null ? registro.getServicio().getNombre() : "Servicio");
                GenerarLogisticaEventoUseCase.Contratado c = idServicio != null ? contratados.get(idServicio) : null;
                s.setCantidad(c != null ? Math.max(1, c.cantidad) : 1);
                s.setHora(registro.getHoraRequerida());
                s.setUbicacion(registro.getUbicacionMontaje());
                s.setEspecificaciones(sinCantidad(registro.getEspecificaciones()));
                s.setDesglose(registro.getDesgloseOpciones());
                s.setResponsable(registro.getResponsableTurno());
                s.setEstado(registro.getEstado());
                servicios.add(s);
            }
        } else {
            // Sin bitácora (evento finalizado con datos antiguos): se listan los servicios
            // contratados en la cotización, sin estado ni responsable.
            for (GenerarLogisticaEventoUseCase.Contratado c : contratados.values()) {
                ReporteLogisticaSemanalDTO.Servicio s = new ReporteLogisticaSemanalDTO.Servicio();
                s.setNombre(c.servicio.getNombre());
                s.setCantidad(Math.max(1, c.cantidad));
                if (c.detalle != null) {
                    s.setHora(c.detalle.getHoraSugerida());
                    s.setUbicacion(c.detalle.getUbicacionMontaje());
                    s.setEspecificaciones(c.detalle.getEspecificacionesCliente());
                    s.setDesglose(c.detalle.getDesgloseOpciones());
                }
                servicios.add(s);
            }
        }
        // Primero lo que tiene hora (en orden), luego el resto por nombre.
        servicios.sort(Comparator
            .comparing((ReporteLogisticaSemanalDTO.Servicio s) -> s.getHora() == null ? LocalTime.MAX : s.getHora())
            .thenComparing(s -> s.getNombre() == null ? "" : s.getNombre(), String.CASE_INSENSITIVE_ORDER));
        dto.setServicios(servicios);
        return dto;
    }

    /** La bitácora guarda "Cantidad: N" al inicio de las especificaciones; aquí va en su propia columna. */
    private static String sinCantidad(String especificaciones) {
        if (especificaciones == null) {
            return null;
        }
        String texto = especificaciones.replaceFirst("^Cantidad: \\d+\\.?\\s*", "").trim();
        return texto.isEmpty() ? null : texto;
    }

    /** Suma por servicio las cantidades de todos los eventos del día. */
    private static List<ReporteLogisticaSemanalDTO.Resumen> consolidar(
            List<ReporteLogisticaSemanalDTO.EventoLogistica> eventos) {
        Map<String, ReporteLogisticaSemanalDTO.Resumen> porServicio = new LinkedHashMap<>();
        for (ReporteLogisticaSemanalDTO.EventoLogistica evento : eventos) {
            for (ReporteLogisticaSemanalDTO.Servicio s : evento.getServicios()) {
                ReporteLogisticaSemanalDTO.Resumen r = porServicio.computeIfAbsent(s.getNombre(), nombre -> {
                    ReporteLogisticaSemanalDTO.Resumen nuevo = new ReporteLogisticaSemanalDTO.Resumen();
                    nuevo.setNombre(nombre);
                    return nuevo;
                });
                r.setCantidad(r.getCantidad() + s.getCantidad());
                r.setEventos(r.getEventos() + 1);
            }
        }
        List<ReporteLogisticaSemanalDTO.Resumen> resumen = new ArrayList<>(porServicio.values());
        resumen.sort(Comparator.comparing(ReporteLogisticaSemanalDTO.Resumen::getNombre, String.CASE_INSENSITIVE_ORDER));
        return resumen;
    }
}
