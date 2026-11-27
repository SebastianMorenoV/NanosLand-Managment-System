package com.example.negocio.cotizacion.usecase;

import com.example.negocio.precio.Dinero;
import com.mycompany.common.dtos.DetalleCotizacionDTO;
import com.mycompany.persistencia.dominio.DetalleCotizacion;
import com.mycompany.persistencia.dominio.Servicio;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renglones de servicios extra de una cotización.
 *
 * <p>Cada renglón lleva su cantidad (20 bollos = un renglón de 20). Hasta noviembre de 2026
 * la pantalla guardaba cada unidad como un renglón aparte de cantidad 1, y esos renglones
 * siguen en la base: aquí se juntan para editarlos y para mostrarlos en los documentos.
 */
public final class ExtrasCotizacion {

    /** Mayor cantidad de un servicio que se puede agregar de una vez a la cotización. */
    public static final int CANTIDAD_MAXIMA = 999;

    /** Un servicio extra como se muestra en el contrato, el comprobante o el estado de cuenta. */
    public record Renglon(Servicio servicio, double precioUnitario, int cantidad, double subtotal) {
    }

    private ExtrasCotizacion() {
    }

    /** Suma de los renglones (precio unitario por cantidad), redondeada a centavos. */
    public static double subtotal(Collection<DetalleCotizacionDTO> renglones) {
        if (renglones == null) return 0;
        double total = 0;
        for (DetalleCotizacionDTO r : renglones) {
            if (r != null) total += r.getPrecioUnitario() * Math.max(1, r.getCantidad());
        }
        return Dinero.aCentavos(total);
    }

    /**
     * Junta en un solo renglón los del mismo servicio, al mismo precio y con los mismos datos
     * de logística, sumando sus cantidades. Los que difieren en algo quedan aparte para no
     * perder sus datos. Devuelve copias (sin id) y no modifica las recibidas; omite los
     * renglones sin servicio.
     */
    public static List<DetalleCotizacionDTO> juntarIguales(Collection<DetalleCotizacionDTO> renglones) {
        Map<List<Object>, DetalleCotizacionDTO> juntos = new LinkedHashMap<>();
        if (renglones == null) return new ArrayList<>();
        for (DetalleCotizacionDTO r : renglones) {
            if (r == null || r.getServicioId() == null) continue;
            List<Object> clave = Arrays.asList(r.getServicioId(), r.getNombreServicio(), r.getPrecioUnitario(),
                r.getHoraSugerida(), textoONull(r.getEspecificacionesCliente()),
                textoONull(r.getDesgloseOpciones()), textoONull(r.getUbicacionMontaje()));
            DetalleCotizacionDTO junto = juntos.get(clave);
            if (junto == null) {
                junto = new DetalleCotizacionDTO();
                junto.setServicioId(r.getServicioId());
                junto.setNombreServicio(r.getNombreServicio());
                junto.setPrecioUnitario(r.getPrecioUnitario());
                junto.setHoraSugerida(r.getHoraSugerida());
                junto.setEspecificacionesCliente(textoONull(r.getEspecificacionesCliente()));
                junto.setDesgloseOpciones(textoONull(r.getDesgloseOpciones()));
                junto.setUbicacionMontaje(textoONull(r.getUbicacionMontaje()));
                juntos.put(clave, junto);
            }
            junto.setCantidad(junto.getCantidad() + Math.max(1, r.getCantidad()));
            junto.setSubtotal(Dinero.aCentavos(junto.getPrecioUnitario() * junto.getCantidad()));
        }
        return new ArrayList<>(juntos.values());
    }

    /**
     * Un renglón por servicio y precio unitario, en el orden en que aparecen. Para los
     * documentos y el estado de cuenta, donde no importan los datos de logística.
     */
    public static List<Renglon> paraMostrar(Collection<DetalleCotizacion> detalles) {
        Map<List<Object>, Renglon> juntos = new LinkedHashMap<>();
        if (detalles == null) return new ArrayList<>();
        for (DetalleCotizacion d : detalles) {
            if (d == null) continue;
            Long servicioId = d.getServicio() != null ? d.getServicio().getId() : null;
            List<Object> clave = Arrays.asList(servicioId, d.getPrecioUnitario());
            int cantidad = Math.max(1, d.getCantidad());
            juntos.merge(clave, new Renglon(d.getServicio(), d.getPrecioUnitario(), cantidad, d.getSubtotal()),
                (a, b) -> new Renglon(a.servicio(), a.precioUnitario(), a.cantidad() + b.cantidad(),
                    Dinero.aCentavos(a.subtotal() + b.subtotal())));
        }
        return new ArrayList<>(juntos.values());
    }

    private static String textoONull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
