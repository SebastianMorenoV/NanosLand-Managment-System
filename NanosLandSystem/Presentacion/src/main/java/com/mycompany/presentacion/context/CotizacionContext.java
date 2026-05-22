package com.mycompany.presentacion.context;

import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.TurnoEvento;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Contexto de sesión de la cotización en edición.
 *
 * Fix #7 — Estado de sesión centralizado y limpiable:
 * Se amplió de solo guardar `fechaSeleccionada` a contener TODA la información
 * de estado de la sesión activa de cotización. Esto permite:
 * a) Un punto único de limpieza (limpiar()) llamado en nuevaCotizacion().
 * b) Eliminar campos sueltos del controlador (modoEdicion, cotizacionEnEdicionId, etc.)
 *    que antes nunca se reseteaban correctamente.
 * c) Que la fecha anterior no persista al regresar a la pantalla de cotización
 *    después de haber cancelado un flujo previo.
 */
@Service
@Data
public class CotizacionContext {

    // Estado de selección de fecha (Flujo SeleccionarFecha → Cotizacion)
    private LocalDate fechaSeleccionada;

    // Estado de la cotización en edición activa
    private Long cotizacionEnEdicionId;
    private EstadoCotizacion estadoEdicionActual;
    private LocalDate fechaEdicionActual;
    private TurnoEvento turnoEdicionActual;
    private boolean modoEdicion = false;

    /**
     * Limpia completamente el estado de sesión.
     * Debe llamarse al salir del modo edición, cancelar, o crear nueva cotización.
     */
    public void limpiar() {
        this.fechaSeleccionada = null;
        this.cotizacionEnEdicionId = null;
        this.estadoEdicionActual = null;
        this.fechaEdicionActual = null;
        this.turnoEdicionActual = null;
        this.modoEdicion = false;
    }
}
