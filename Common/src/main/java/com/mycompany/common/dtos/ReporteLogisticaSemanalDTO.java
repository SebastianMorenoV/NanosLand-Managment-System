package com.mycompany.common.dtos;

import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.enums.EstadoLogistica;
import com.mycompany.persistencia.enums.TurnoEvento;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Reporte semanal de logística: qué se ocupa cada día de la semana, evento por
 * evento, más un consolidado diario que suma lo mismo entre eventos.
 *
 * Los textos para mostrar (fechas, horarios, detalle de cada servicio) se arman
 * aquí para que la pantalla y el PDF digan exactamente lo mismo.
 */
@Data
@NoArgsConstructor
public class ReporteLogisticaSemanalDTO {

    private static final Locale ES = new Locale("es", "MX");

    /** Lunes de la semana. */
    private LocalDate inicio;
    /** Domingo de la semana. */
    private LocalDate fin;
    /** Los siete días, de lunes a domingo; los días sin eventos van con la lista vacía. */
    private List<Dia> dias = new ArrayList<>();

    private int totalEventos;
    /** Suma de las cantidades de todos los servicios de la semana. */
    private int totalServicios;

    /** Por ejemplo: "Del 23 al 29 de noviembre de 2026". */
    public String getRango() {
        if (inicio == null || fin == null) {
            return "";
        }
        DateTimeFormatter diaMes = DateTimeFormatter.ofPattern("d 'de' MMMM", ES);
        String desde = inicio.getMonth() == fin.getMonth()
            ? String.valueOf(inicio.getDayOfMonth()) : inicio.format(diaMes);
        return "Del " + desde + " al " + fin.format(diaMes) + " de " + fin.getYear();
    }

    /** Hora fácil de leer: "9:00 am", "2:30 pm". */
    public static String horaTexto(LocalTime hora) {
        if (hora == null) {
            return "";
        }
        int h = hora.getHour() % 12 == 0 ? 12 : hora.getHour() % 12;
        return String.format("%d:%02d %s", h, hora.getMinute(), hora.getHour() < 12 ? "am" : "pm");
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    @Data
    @NoArgsConstructor
    public static class Dia {
        private LocalDate fecha;
        private List<EventoLogistica> eventos = new ArrayList<>();
        /** Consolidado del día: cada servicio con su cantidad total entre todos los eventos. */
        private List<Resumen> resumen = new ArrayList<>();

        /** Por ejemplo: "Lunes 23 de noviembre". */
        public String getNombre() {
            String texto = fecha.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES));
            return texto.substring(0, 1).toUpperCase(ES) + texto.substring(1);
        }

        /** Por ejemplo: "3 Iluminación y Sonido, 2 Pintacaritas VIP". */
        public String getResumenTexto() {
            return resumen.stream()
                .map(r -> r.getCantidad() + " " + r.getNombre())
                .collect(Collectors.joining(", "));
        }
    }

    @Data
    @NoArgsConstructor
    public static class EventoLogistica {
        private Long eventoId;
        private String folio;
        private String cliente;
        private String telefono;
        private String festejado;
        private String tematica;
        private String paquete;
        private String notas;
        private TurnoEvento turno;
        private LocalTime horaInicio;
        private LocalTime horaFin;
        private EstadoEvento estado;
        private List<Servicio> servicios = new ArrayList<>();

        /** Por ejemplo: "Mañana, 9:00 am a 2:00 pm". */
        public String getHorario() {
            String nombreTurno = turno == TurnoEvento.MATUTINO ? "Mañana"
                : turno == TurnoEvento.VESPERTINO ? "Tarde" : "";
            if (horaInicio == null || horaFin == null) {
                return nombreTurno;
            }
            return nombreTurno + ", " + horaTexto(horaInicio) + " a " + horaTexto(horaFin);
        }

        /** Por ejemplo: "Fiesta de Sofía · Unicornios · Cliente: Juan Pérez". */
        public String getDescripcion() {
            List<String> partes = new ArrayList<>();
            partes.add(tieneTexto(festejado) ? "Fiesta de " + festejado.trim() : "Fiesta");
            if (tieneTexto(tematica)) {
                partes.add(tematica.trim());
            }
            if (tieneTexto(cliente)) {
                partes.add("Cliente: " + cliente.trim());
            }
            return String.join(" · ", partes);
        }
    }

    @Data
    @NoArgsConstructor
    public static class Servicio {
        private String nombre;
        private int cantidad;
        private LocalTime hora;
        private String ubicacion;
        private String especificaciones;
        private String desglose;
        private String responsable;
        /** Null si el evento no tiene bitácora de logística (por ejemplo, datos antiguos). */
        private EstadoLogistica estado;

        /** Ya está montado o en el salón. */
        public boolean isListo() {
            return estado == EstadoLogistica.LISTO;
        }

        /** Solo lo que haya capturado: "10:30 am · Jardín · Color rosa". */
        public String getDetalle() {
            List<String> partes = new ArrayList<>();
            if (hora != null) {
                partes.add(horaTexto(hora));
            }
            for (String valor : new String[] {ubicacion, especificaciones, desglose}) {
                if (tieneTexto(valor)) {
                    partes.add(valor.trim());
                }
            }
            return String.join(" · ", partes);
        }
    }

    @Data
    @NoArgsConstructor
    public static class Resumen {
        private String nombre;
        private int cantidad;
        /** En cuántos eventos del día se ocupa. */
        private int eventos;
    }
}
