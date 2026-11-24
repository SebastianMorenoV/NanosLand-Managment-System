package com.example.negocio.cotizacion.usecase;

import com.example.negocio.exception.CotizacionException;
import com.example.negocio.precio.Dinero;
import com.mycompany.common.dtos.DetalleCotizacionDTO;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.DetalleCotizacion;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.enums.CategoriaServicio;
import com.mycompany.persistencia.repository.ServicioRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Arma los renglones de servicios extra de una cotización a partir de lo capturado en pantalla. */
final class LineasExtraCotizacion {

    private LineasExtraCotizacion() {
    }

    /**
     * @param yaContratados servicios que la cotización ya tenía como extra: se aceptan aunque
     *                      hoy ya no sean de categoría Extra, para poder seguir editándola
     */
    static List<DetalleCotizacion> construir(List<DetalleCotizacionDTO> detallesDTO, Cotizacion cotizacion,
                                             ServicioRepository servicioRepository, Set<Long> yaContratados) {
        List<DetalleCotizacion> detalles = new ArrayList<>();
        if (detallesDTO == null) return detalles;

        for (DetalleCotizacionDTO detalleDTO : detallesDTO) {
            if (detalleDTO == null) continue;
            if (detalleDTO.getServicioId() == null) {
                throw new CotizacionException("Un servicio extra no tiene id asociado.");
            }

            Servicio servicio = servicioRepository.findById(detalleDTO.getServicioId())
                .orElseThrow(() -> new CotizacionException(
                    "El servicio con ID " + detalleDTO.getServicioId() + " no existe en el catálogo."
                ));
            if (servicio.getCategoria() != CategoriaServicio.EXTRA && !yaContratados.contains(servicio.getId())) {
                throw new CotizacionException("\"" + servicio.getNombre()
                    + "\" no se vende como extra: solo va incluido en los paquetes.");
            }

            int cantidad = detalleDTO.getCantidad() > 0 ? detalleDTO.getCantidad() : 1;
            double precioUnitario = detalleDTO.getPrecioUnitario();
            double subtotal = detalleDTO.getSubtotal();
            if (subtotal <= 0) {
                subtotal = precioUnitario * cantidad;
            }

            DetalleCotizacion detalle = new DetalleCotizacion();
            detalle.setServicio(servicio);
            detalle.setCotizacion(cotizacion);
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setCantidad(cantidad);
            detalle.setSubtotal(Dinero.aCentavos(subtotal));
            detalle.setHoraSugerida(detalleDTO.getHoraSugerida());
            detalle.setEspecificacionesCliente(detalleDTO.getEspecificacionesCliente());
            detalle.setDesgloseOpciones(detalleDTO.getDesgloseOpciones());
            detalle.setUbicacionMontaje(detalleDTO.getUbicacionMontaje());
            detalles.add(detalle);
        }
        return detalles;
    }

    static double total(List<DetalleCotizacion> detalles) {
        return detalles.stream().mapToDouble(DetalleCotizacion::getSubtotal).sum();
    }
}
