package com.mycompany.common.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Datos clave de la pantalla de Inicio. */
@Data
@NoArgsConstructor
public class ResumenInicioDTO {

    public enum TipoPendiente { COBRAR, CONFIRMAR, CONTACTAR }

    private int eventosHoy;
    /** Eventos de mañana a dentro de 7 días. */
    private int eventosProximos;
    private int cotizacionesPorConfirmar;
    /** Servicios de los eventos de hoy a 7 días que aún no se contactan. */
    private int serviciosPorContactar;

    /** Dinero: solo se llena para el dueño; null para los demás roles. */
    private Double ingresosMes;
    private Double porCobrar;

    private List<EventoProximo> eventos = new ArrayList<>();
    private List<Pendiente> pendientes = new ArrayList<>();

    @Data
    @NoArgsConstructor
    public static class EventoProximo {
        private Long cotizacionId;
        private LocalDate fecha;
        private String horario;
        private String descripcion;
    }

    @Data
    @NoArgsConstructor
    public static class Pendiente {
        private TipoPendiente tipo;
        private Long cotizacionId;
        private LocalDate fecha;
        private String titulo;
        private String detalle;
    }
}
