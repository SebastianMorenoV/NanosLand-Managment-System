package com.example.negocio.logistica.usecase;

import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.DetalleCotizacion;
import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.dominio.LogisticaServicio;
import com.mycompany.persistencia.dominio.PaqueteServicio;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.enums.EstadoLogistica;
import com.mycompany.persistencia.enums.OrigenLogistica;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Genera y mantiene la bitácora de logística de un evento confirmado. Lleva un registro por cada
 * servicio que incluye el paquete (con la comida elegida) y, aparte, uno por cada servicio extra con
 * sus propios datos: 100 bollitos extra temáticos para las 9:30 son otra entrega que los 80 bollitos
 * del paquete, aunque sea el mismo servicio.
 *
 * Debe llamarse dentro de la transacción que confirma o modifica el evento.
 */
@Service
public class GenerarLogisticaEventoUseCase {

    /** Datos de un servicio contratado, reunidos del paquete y de los extras (total por servicio). */
    public static final class Contratado {
        public Servicio servicio;
        public int cantidad;
        public DetalleCotizacion detalle; // primer extra con datos operativos; null si solo viene del paquete
    }

    /**
     * Una parte de lo contratado que lleva su propio registro de logística: un servicio del paquete,
     * o un extra (los renglones de extras del mismo servicio y con los mismos datos van juntos).
     *
     * @param detalle solo de un extra: el renglón de la cotización con sus datos
     * @param clave   solo de un extra: sus datos, para reconocer su registro
     */
    public record Parte(OrigenLogistica origen, Servicio servicio, int cantidad, DetalleCotizacion detalle, String clave) {
    }

    /** Separa los datos de un extra dentro de su clave. */
    private static final String SEPARADOR = "\u001F";

    /**
     * Crea los registros que falten y, si no es {@code soloSiVacia}, también actualiza las cantidades,
     * quita los registros de lo que ya no está contratado y separa los registros de antes de que lo
     * del paquete y los extras fueran aparte.
     *
     * @param evento       evento ya gestionado por la sesión actual.
     * @param soloSiVacia  si es {@code true} solo genera cuando el evento no tiene ningún registro
     *                     (para completar eventos confirmados antes de que existiera esta generación,
     *                     sin tocar los que ya tienen bitácora).
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

        List<Parte> partes = partesContratadas(cotizacion);
        if (!soloSiVacia) {
            asignarOrigenAAnteriores(registros, partes);
        }

        Set<LogisticaServicio> usados = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Parte> pendientes = new ArrayList<>();
        // Cada parte con su registro: lo del paquete por servicio; un extra, por sus datos.
        for (Parte parte : partes) {
            LogisticaServicio registro = registros.stream()
                .filter(r -> !usados.contains(r) && corresponde(r, parte))
                .findFirst().orElse(null);
            if (registro == null) {
                pendientes.add(parte);
            } else {
                usados.add(registro);
                actualizarCantidad(registro, parte.cantidad());
            }
        }

        // Un extra al que en la cotización le cambiaron los datos (otra hora, otra ubicación...):
        // si es el único de su servicio sin registro y hay un solo registro de ese servicio sin
        // parte, es el mismo. Se actualiza lo que cambió y conserva su estado y responsable.
        Map<Long, List<Parte>> extrasSinRegistro = new LinkedHashMap<>();
        for (Parte parte : pendientes) {
            if (parte.origen() == OrigenLogistica.EXTRA) {
                extrasSinRegistro.computeIfAbsent(parte.servicio().getId(), id -> new ArrayList<>()).add(parte);
            }
        }
        for (List<Parte> mismoServicio : extrasSinRegistro.values()) {
            Parte parte = mismoServicio.get(0);
            List<LogisticaServicio> libres = registros.stream()
                .filter(r -> !usados.contains(r) && r.getOrigen() == OrigenLogistica.EXTRA && esDe(r, parte.servicio()))
                .toList();
            if (mismoServicio.size() == 1 && libres.size() == 1) {
                usados.add(libres.get(0));
                actualizarDesdeExtra(libres.get(0), parte);
                pendientes.remove(parte);
            }
        }

        if (!soloSiVacia) {
            // Lo que ya no está contratado: su registro ya no aplica.
            registros.removeIf(r -> !usados.contains(r));
        }
        for (Parte parte : pendientes) {
            registros.add(nuevoRegistro(evento, parte));
        }
    }

    /**
     * Corrección de los registros de antes (sin origen) de un evento, con el menor cambio posible:
     * les asigna su origen y separa solo los servicios contratados en más de una parte (del paquete y
     * extra, o varios extras), que antes iban sumados en un registro; la parte que falte se crea con
     * los datos de la cotización. Los demás registros conservan su cantidad y su texto, y no se quita
     * nada: recalcular y quitar lo hace, como siempre, la siguiente edición de la cotización.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void separarRegistrosAnteriores(Evento evento) {
        if (evento == null || evento.getCotizacion() == null || evento.getLogisticaServicios() == null) {
            return;
        }
        List<LogisticaServicio> registros = evento.getLogisticaServicios();
        List<LogisticaServicio> anteriores = registros.stream().filter(r -> r.getOrigen() == null).toList();
        if (anteriores.isEmpty()) {
            return;
        }
        List<Parte> partes = partesContratadas(evento.getCotizacion());
        asignarOrigenAAnteriores(registros, partes);

        Set<Long> sumados = new java.util.HashSet<>();
        for (LogisticaServicio anterior : anteriores) {
            if (anterior.getServicio() == null) continue;
            Long id = anterior.getServicio().getId();
            if (partes.stream().filter(p -> Objects.equals(p.servicio().getId(), id)).count() > 1) {
                sumados.add(id);
            }
        }
        for (Parte parte : partes) {
            if (!sumados.contains(parte.servicio().getId())) continue;
            LogisticaServicio registro = registros.stream().filter(r -> corresponde(r, parte)).findFirst().orElse(null);
            if (registro == null) {
                registros.add(nuevoRegistro(evento, parte));
            } else {
                actualizarCantidad(registro, parte.cantidad());
            }
        }
    }

    /**
     * Lo contratado, en las partes que llevan cada una su registro: un servicio del paquete (con la
     * comida elegida, una porción por persona) o un extra con sus datos.
     */
    public List<Parte> partesContratadas(Cotizacion cotizacion) {
        Map<Long, Servicio> serviciosPaquete = new LinkedHashMap<>();
        Map<Long, Integer> cantidadesPaquete = new LinkedHashMap<>();
        if (cotizacion.getPaquete() != null && cotizacion.getPaquete().getServicios() != null) {
            for (PaqueteServicio ps : cotizacion.getPaquete().getServicios()) {
                if (ps == null || ps.getServicio() == null) continue;
                serviciosPaquete.put(ps.getServicio().getId(), ps.getServicio());
                cantidadesPaquete.merge(ps.getServicio().getId(), Math.max(1, ps.getCantidad()), Integer::sum);
            }
        }
        if (cotizacion.getComida() != null) {
            Servicio comida = cotizacion.getComida();
            serviciosPaquete.put(comida.getId(), comida);
            cantidadesPaquete.merge(comida.getId(),
                Math.max(1, cotizacion.getPersonas() != null ? cotizacion.getPersonas() : 0), Integer::sum);
        }

        List<Parte> partes = new ArrayList<>();
        serviciosPaquete.forEach((id, servicio) ->
            partes.add(new Parte(OrigenLogistica.PAQUETE, servicio, cantidadesPaquete.get(id), null, null)));

        Map<String, DetalleCotizacion> extras = new LinkedHashMap<>();
        Map<String, Integer> cantidadesExtra = new LinkedHashMap<>();
        if (cotizacion.getServiciosExtra() != null) {
            for (DetalleCotizacion detalle : cotizacion.getServiciosExtra()) {
                if (detalle == null || detalle.getServicio() == null) continue;
                String clave = clave(detalle);
                extras.putIfAbsent(clave, detalle);
                cantidadesExtra.merge(clave, Math.max(1, detalle.getCantidad()), Integer::sum);
            }
        }
        extras.forEach((clave, detalle) ->
            partes.add(new Parte(OrigenLogistica.EXTRA, detalle.getServicio(), cantidadesExtra.get(clave), detalle, clave)));
        return partes;
    }

    /** Servicios contratados en una cotización (paquete + comida + extras), sumados por servicio. */
    public Map<Long, Contratado> serviciosContratados(Cotizacion cotizacion) {
        Map<Long, Contratado> contratados = new LinkedHashMap<>();
        for (Parte parte : partesContratadas(cotizacion)) {
            Contratado c = contratados.computeIfAbsent(parte.servicio().getId(), id -> new Contratado());
            c.servicio = parte.servicio();
            c.cantidad += parte.cantidad();
            if (parte.detalle() != null && (c.detalle == null || !tieneDatosOperativos(c.detalle))) {
                c.detalle = parte.detalle();
            }
        }
        return contratados;
    }

    // ── Registros ────────────────────────────────────────────────────────────

    private static LogisticaServicio nuevoRegistro(Evento evento, Parte parte) {
        LogisticaServicio registro = new LogisticaServicio();
        registro.setEvento(evento);
        registro.setServicio(parte.servicio());
        registro.setEstado(EstadoLogistica.POR_CONTACTAR);
        registro.setOrigen(parte.origen());
        registro.setClaveExtra(parte.clave());
        String especificaciones = null;
        if (parte.detalle() != null) {
            registro.setHoraRequerida(parte.detalle().getHoraSugerida());
            registro.setDesgloseOpciones(parte.detalle().getDesgloseOpciones());
            registro.setUbicacionMontaje(parte.detalle().getUbicacionMontaje());
            especificaciones = parte.detalle().getEspecificacionesCliente();
        }
        registro.setEspecificaciones(especificaciones);
        actualizarCantidad(registro, parte.cantidad());
        return registro;
    }

    /** La cantidad pudo cambiar (p. ej. otro número de personas); lo demás que se escribió se conserva. */
    private static void actualizarCantidad(LogisticaServicio registro, int cantidad) {
        registro.setCantidad(cantidad);
        registro.setEspecificaciones(conCantidad(registro.getEspecificaciones(), cantidad));
    }

    /** Pasa al registro solo los datos del extra que cambiaron en la cotización. */
    private static void actualizarDesdeExtra(LogisticaServicio registro, Parte parte) {
        String[] antes = registro.getClaveExtra() == null ? null : registro.getClaveExtra().split(SEPARADOR, -1);
        String[] ahora = parte.clave().split(SEPARADOR, -1);
        DetalleCotizacion d = parte.detalle();
        if (antes != null && antes.length == ahora.length) {
            if (!antes[1].equals(ahora[1])) registro.setHoraRequerida(d.getHoraSugerida());
            if (!antes[2].equals(ahora[2])) registro.setEspecificaciones(d.getEspecificacionesCliente());
            if (!antes[3].equals(ahora[3])) registro.setDesgloseOpciones(d.getDesgloseOpciones());
            if (!antes[4].equals(ahora[4])) registro.setUbicacionMontaje(d.getUbicacionMontaje());
        }
        registro.setClaveExtra(parte.clave());
        actualizarCantidad(registro, parte.cantidad());
    }

    private static boolean corresponde(LogisticaServicio registro, Parte parte) {
        if (registro.getOrigen() != parte.origen() || !esDe(registro, parte.servicio())) {
            return false;
        }
        return parte.origen() == OrigenLogistica.PAQUETE || Objects.equals(registro.getClaveExtra(), parte.clave());
    }

    private static boolean esDe(LogisticaServicio registro, Servicio servicio) {
        return registro.getServicio() != null && servicio != null
            && Objects.equals(registro.getServicio().getId(), servicio.getId());
    }

    /**
     * Antes, lo del paquete y los extras de un mismo servicio iban sumados en un solo registro, que
     * tomaba los datos del extra si ya estaba al confirmar. Cada registro sin origen se asigna a una
     * parte (conserva su estado, responsable y lo escrito) y lo que quede sin registro se crea aparte:
     * <ul>
     *   <li>si tiene los mismos datos que un extra de su servicio, se creó con ese extra: es del extra;</li>
     *   <li>si no, y el paquete incluye ese servicio, es del paquete;</li>
     *   <li>si no, es del primer extra de su servicio que quede sin registro;</li>
     *   <li>si nada de su servicio sigue contratado, se queda sin origen y se quita.</li>
     * </ul>
     */
    private static void asignarOrigenAAnteriores(List<LogisticaServicio> registros, List<Parte> partes) {
        for (LogisticaServicio registro : registros) {
            if (registro.getOrigen() != null || registro.getServicio() == null) continue;
            Parte extraConSusDatos = partes.stream()
                .filter(p -> p.origen() == OrigenLogistica.EXTRA && esDe(registro, p.servicio())
                    && sinRegistro(registros, p) && tieneDatosOperativos(p.detalle()) && mismosDatos(registro, p.detalle()))
                .findFirst().orElse(null);
            Parte delPaquete = partes.stream()
                .filter(p -> p.origen() == OrigenLogistica.PAQUETE && esDe(registro, p.servicio()) && sinRegistro(registros, p))
                .findFirst().orElse(null);
            Parte extra = partes.stream()
                .filter(p -> p.origen() == OrigenLogistica.EXTRA && esDe(registro, p.servicio()) && sinRegistro(registros, p))
                .findFirst().orElse(null);
            Parte asignada = extraConSusDatos != null ? extraConSusDatos : delPaquete != null ? delPaquete : extra;
            if (asignada != null) {
                registro.setOrigen(asignada.origen());
                registro.setClaveExtra(asignada.clave());
            }
        }
    }

    /** Ningún registro tiene ya asignada esa parte. */
    private static boolean sinRegistro(List<LogisticaServicio> registros, Parte parte) {
        return registros.stream().noneMatch(r -> corresponde(r, parte));
    }

    private static boolean mismosDatos(LogisticaServicio registro, DetalleCotizacion d) {
        return Objects.equals(registro.getHoraRequerida(), d.getHoraSugerida())
            && Objects.equals(texto(conCantidad(registro.getEspecificaciones(), 1)), texto(d.getEspecificacionesCliente()))
            && Objects.equals(texto(registro.getDesgloseOpciones()), texto(d.getDesgloseOpciones()))
            && Objects.equals(texto(registro.getUbicacionMontaje()), texto(d.getUbicacionMontaje()));
    }

    /** Servicio y datos de un extra; los del mismo servicio con los mismos datos van en un solo registro. */
    static String clave(DetalleCotizacion d) {
        LocalTime hora = d.getHoraSugerida();
        return String.join(SEPARADOR,
            String.valueOf(d.getServicio().getId()),
            hora == null ? "" : hora.toString(),
            texto(d.getEspecificacionesCliente()),
            texto(d.getDesgloseOpciones()),
            texto(d.getUbicacionMontaje()));
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
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

    private static boolean tieneDatosOperativos(DetalleCotizacion d) {
        return d != null && (d.getHoraSugerida() != null
            || (d.getEspecificacionesCliente() != null && !d.getEspecificacionesCliente().isBlank())
            || (d.getDesgloseOpciones() != null && !d.getDesgloseOpciones().isBlank())
            || (d.getUbicacionMontaje() != null && !d.getUbicacionMontaje().isBlank()));
    }
}
