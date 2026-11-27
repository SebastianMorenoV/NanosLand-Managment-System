package com.example.negocio.cotizacion.usecase;

import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.CotizacionServicioIncluido;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.dominio.PaqueteServicio;
import com.mycompany.persistencia.dominio.Servicio;

import java.util.ArrayList;
import java.util.List;

/**
 * Lo que incluía el paquete de una cotización cuando se vendió: su nombre, su descripción y sus
 * servicios incluidos. Si la cotización guarda su copia del paquete se usa esa; si no (cotizaciones
 * de antes de la copia), el paquete como está en el catálogo, que es lo que se hacía antes.
 */
public final class PaqueteCotizado {

    /** Un servicio incluido en el paquete, con su cantidad. */
    public record Incluido(Servicio servicio, String nombre, int cantidad) {
    }

    private PaqueteCotizado() {
    }

    /** Copia en la cotización lo que incluye hoy su paquete, reemplazando la copia anterior. */
    public static void guardarCopia(Cotizacion cotizacion) {
        Paquete paquete = cotizacion.getPaquete();
        if (paquete == null) {
            return;
        }
        cotizacion.setPaqueteNombre(paquete.getNombre());
        cotizacion.setPaqueteDescripcion(paquete.getDescripcion());
        if (cotizacion.getServiciosIncluidos() == null) {
            cotizacion.setServiciosIncluidos(new ArrayList<>());
        }
        List<CotizacionServicioIncluido> copia = cotizacion.getServiciosIncluidos();
        copia.clear();
        if (paquete.getServicios() != null) {
            for (PaqueteServicio ps : paquete.getServicios()) {
                if (ps == null || ps.getServicio() == null) continue;
                CotizacionServicioIncluido incluido = new CotizacionServicioIncluido();
                incluido.setCotizacion(cotizacion);
                incluido.setServicio(ps.getServicio());
                incluido.setNombre(ps.getServicio().getNombre());
                incluido.setCantidad(ps.getCantidad());
                copia.add(incluido);
            }
        }
    }

    /** Nombre del paquete vendido. */
    public static String nombre(Cotizacion cotizacion) {
        if (cotizacion.tieneCopiaDelPaquete()) {
            return cotizacion.getPaqueteNombre();
        }
        return cotizacion.getPaquete() != null ? cotizacion.getPaquete().getNombre() : null;
    }

    /** Descripción del paquete vendido (la de «Su renta o paquete incluye» del contrato). */
    public static String descripcion(Cotizacion cotizacion) {
        if (cotizacion.tieneCopiaDelPaquete()) {
            return cotizacion.getPaqueteDescripcion();
        }
        return cotizacion.getPaquete() != null ? cotizacion.getPaquete().getDescripcion() : null;
    }

    /** Servicios que incluía el paquete vendido, con su cantidad. */
    public static List<Incluido> serviciosIncluidos(Cotizacion cotizacion) {
        List<Incluido> incluidos = new ArrayList<>();
        if (cotizacion.tieneCopiaDelPaquete()) {
            if (cotizacion.getServiciosIncluidos() != null) {
                for (CotizacionServicioIncluido copia : cotizacion.getServiciosIncluidos()) {
                    if (copia == null) continue;
                    incluidos.add(new Incluido(copia.getServicio(), copia.getNombre(), copia.getCantidad()));
                }
            }
        } else if (cotizacion.getPaquete() != null && cotizacion.getPaquete().getServicios() != null) {
            for (PaqueteServicio ps : cotizacion.getPaquete().getServicios()) {
                if (ps == null || ps.getServicio() == null) continue;
                incluidos.add(new Incluido(ps.getServicio(), ps.getServicio().getNombre(), ps.getCantidad()));
            }
        }
        return incluidos;
    }
}
