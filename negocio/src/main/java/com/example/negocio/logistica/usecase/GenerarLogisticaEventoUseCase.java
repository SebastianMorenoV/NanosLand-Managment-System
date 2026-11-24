package com.example.negocio.logistica.usecase;

import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.DetalleCotizacion;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.dominio.LogisticaServicio;
import com.mycompany.persistencia.dominio.PaqueteServicio;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.enums.EstadoLogistica;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Genera y mantiene la bitácora de logística de un evento confirmado: un
 * registro por cada servicio contratado, ya sea parte del paquete, la comida
 * elegida o un extra.
 *
 * Debe llamarse dentro de la transacción que confirma o modifica el evento.
 */
@Service
public class GenerarLogisticaEventoUseCase {

    /** Datos de un servicio contratado, reunidos del paquete y de los extras. */
    public static final class Contratado {
        public Servicio servicio;
        public int cantidad;
        public DetalleCotizacion detalle; // primer extra con datos operativos; null si solo viene del paquete
    }

    /**
     * Crea los registros de logística que falten.
     *
     * @param evento       evento ya gestionado por la sesión actual.
     * @param soloSiVacia  si es {@code true} solo genera cuando el evento no tiene
     *                     ningún registro (para completar eventos confirmados antes
     *                     de que existiera esta generación, sin tocar los que ya
     *                     tienen bitácora). Si es {@code false} además quita los
     *                     registros de servicios que ya no están contratados.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void sincronizar(Evento evento, boolean soloSiVacia) {
        if (evento == null
                || (evento.getEstado() != EstadoEvento.CONFIRMADO && evento.getEstado() != EstadoEvento.EN_CURSO)) {
            return;
        }
        Cotizacion cotizacion = evento.getCotizacion();
        if (cotizacion == null) {
            return;
        }
        if (evento.getLogisticaServicios() == null) {
            evento.setLogisticaServicios(new ArrayList<>());
        }
        List<LogisticaServicio> registros = evento.getLogisticaServicios();
        if (soloSiVacia && !registros.isEmpty()) {
            return;
        }

        Map<Long, Contratado> contratados = serviciosContratados(cotizacion);

        if (!soloSiVacia) {
            // Servicios que se quitaron de la cotización: su registro ya no aplica.
            registros.removeIf(r -> r.getServicio() == null || !contratados.containsKey(r.getServicio().getId()));
        }

        for (Contratado c : contratados.values()) {
            LogisticaServicio existente = registros.stream()
                .filter(r -> r.getServicio() != null && r.getServicio().getId().equals(c.servicio.getId()))
                .findFirst().orElse(null);
            if (existente != null) {
                if (!soloSiVacia) {
                    // La cantidad pudo cambiar (p. ej. otro número de personas): se actualiza
                    // sin tocar lo demás que se haya escrito en las especificaciones.
                    existente.setEspecificaciones(conCantidad(existente.getEspecificaciones(), c.cantidad));
                }
                continue;
            }
            LogisticaServicio registro = new LogisticaServicio();
            registro.setEvento(evento);
            registro.setServicio(c.servicio);
            registro.setEstado(EstadoLogistica.POR_CONTACTAR);
            String especificaciones = null;
            if (c.detalle != null) {
                registro.setHoraRequerida(c.detalle.getHoraSugerida());
                registro.setDesgloseOpciones(c.detalle.getDesgloseOpciones());
                registro.setUbicacionMontaje(c.detalle.getUbicacionMontaje());
                especificaciones = c.detalle.getEspecificacionesCliente();
            }
            registro.setEspecificaciones(conCantidad(especificaciones, c.cantidad));
            registros.add(registro);
        }
    }

    private static final java.util.regex.Pattern PREFIJO_CANTIDAD =
        java.util.regex.Pattern.compile("^Cantidad: \\d+\\.?\\s*");

    /** Pone (o quita, si es 1) el prefijo "Cantidad: N. " de las especificaciones. */
    static String conCantidad(String especificaciones, int cantidad) {
        String resto = especificaciones == null ? "" : PREFIJO_CANTIDAD.matcher(especificaciones).replaceFirst("");
        if (cantidad <= 1) {
            return resto.isBlank() ? null : resto;
        }
        String prefijo = "Cantidad: " + cantidad;
        return resto.isBlank() ? prefijo : prefijo + ". " + resto;
    }

    /** Servicios contratados en una cotización (paquete + comida + extras), por id de servicio. */
    public Map<Long, Contratado> serviciosContratados(Cotizacion cotizacion) {
        Map<Long, Contratado> contratados = new LinkedHashMap<>();

        if (cotizacion.getPaquete() != null && cotizacion.getPaquete().getServicios() != null) {
            for (PaqueteServicio ps : cotizacion.getPaquete().getServicios()) {
                if (ps == null || ps.getServicio() == null) continue;
                Contratado c = contratados.computeIfAbsent(ps.getServicio().getId(), id -> new Contratado());
                c.servicio = ps.getServicio();
                c.cantidad += Math.max(1, ps.getCantidad());
            }
        }

        // La comida elegida (paquetes con tabla de precios): una porción por persona.
        if (cotizacion.getComida() != null) {
            Servicio comida = cotizacion.getComida();
            Contratado c = contratados.computeIfAbsent(comida.getId(), id -> new Contratado());
            c.servicio = comida;
            c.cantidad += Math.max(1, cotizacion.getPersonas() != null ? cotizacion.getPersonas() : 0);
        }

        if (cotizacion.getServiciosExtra() != null) {
            for (DetalleCotizacion detalle : cotizacion.getServiciosExtra()) {
                if (detalle == null || detalle.getServicio() == null) continue;
                Contratado c = contratados.computeIfAbsent(detalle.getServicio().getId(), id -> new Contratado());
                c.servicio = detalle.getServicio();
                c.cantidad += Math.max(1, detalle.getCantidad());
                if (c.detalle == null || !tieneDatosOperativos(c.detalle)) {
                    c.detalle = detalle;
                }
            }
        }
        return contratados;
    }

    private static boolean tieneDatosOperativos(DetalleCotizacion d) {
        return d.getHoraSugerida() != null
            || (d.getEspecificacionesCliente() != null && !d.getEspecificacionesCliente().isBlank())
            || (d.getDesgloseOpciones() != null && !d.getDesgloseOpciones().isBlank())
            || (d.getUbicacionMontaje() != null && !d.getUbicacionMontaje().isBlank());
    }
}
