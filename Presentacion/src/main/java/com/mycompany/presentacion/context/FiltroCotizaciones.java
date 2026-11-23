package com.mycompany.presentacion.context;

import com.mycompany.persistencia.enums.EstadoCotizacion;

import java.time.LocalDate;

/** Pestañas de la pantalla de Cotizaciones. */
public enum FiltroCotizaciones {
    /** Borrador con fecha de hoy en adelante. */
    POR_CONFIRMAR,
    /** Borrador cuya fecha ya pasó sin que se pagara el anticipo. */
    VENCIDAS,
    CONFIRMADAS,
    CANCELADAS;

    public static FiltroCotizaciones de(EstadoCotizacion estado, LocalDate fecha) {
        if (estado == EstadoCotizacion.VIGENTE) {
            return CONFIRMADAS;
        }
        if (estado == EstadoCotizacion.BORRADOR || estado == null) {
            return fecha != null && fecha.isBefore(LocalDate.now()) ? VENCIDAS : POR_CONFIRMAR;
        }
        return CANCELADAS;
    }
}
