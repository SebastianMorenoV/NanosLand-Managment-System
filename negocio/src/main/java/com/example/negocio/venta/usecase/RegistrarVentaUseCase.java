package com.example.negocio.venta.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.DetalleVentaDTO;
import com.mycompany.common.dtos.VentaDTO;
import com.mycompany.common.mapper.VentaMapper;
import com.mycompany.persistencia.dominio.DetalleVenta;
import com.mycompany.persistencia.dominio.Producto;
import com.mycompany.persistencia.dominio.Usuario;
import com.mycompany.persistencia.dominio.Venta;
import com.mycompany.persistencia.enums.EstadoVenta;
import com.mycompany.persistencia.enums.MetodoPago;
import com.mycompany.persistencia.repository.ProductoRepository;
import com.mycompany.persistencia.repository.UsuarioRepository;
import com.mycompany.persistencia.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registra una venta extra. No toca cotizaciones ni eventos: el dinero entra
 * directo a los ingresos del negocio.
 */
@Service
@RequiredArgsConstructor
public class RegistrarVentaUseCase {

    /** Límite por producto, para detectar errores de captura. */
    public static final int CANTIDAD_MAXIMA = 999;

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * @param productos  producto y cantidad de cada renglón; el precio se toma del catálogo.
     * @param metodoPago cómo se recibió el dinero.
     * @param usuarioId  usuario con la sesión abierta.
     */
    @Transactional
    public VentaDTO registrarVenta(List<DetalleVentaDTO> productos, MetodoPago metodoPago, Long usuarioId) {
        if (productos == null || productos.isEmpty()) {
            throw new CotizacionException("Agrega al menos un producto a la venta.");
        }
        if (metodoPago == null) {
            throw new CotizacionException("Elige cómo se pagó.");
        }
        Usuario usuario = usuarioId == null ? null : usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null || !usuario.isActivo()) {
            throw new CotizacionException("Inicia sesión para registrar la venta.");
        }

        // El mismo producto en dos renglones se guarda como uno solo.
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (DetalleVentaDTO renglon : productos) {
            if (renglon == null || renglon.getProductoId() == null) {
                throw new CotizacionException("Uno de los productos no es válido.");
            }
            if (renglon.getCantidad() <= 0) {
                throw new CotizacionException("La cantidad debe ser mayor a cero.");
            }
            cantidades.merge(renglon.getProductoId(), renglon.getCantidad(), Integer::sum);
        }

        Venta venta = new Venta();
        venta.setFechaHora(LocalDateTime.now());
        venta.setMetodoPago(metodoPago);
        venta.setEstado(EstadoVenta.REGISTRADA);
        venta.setUsuario(usuario);

        double total = 0.0;
        for (Map.Entry<Long, Integer> renglon : cantidades.entrySet()) {
            Producto producto = productoRepository.findById(renglon.getKey())
                .filter(Producto::isActivo)
                .orElseThrow(() -> new CotizacionException(
                    "Uno de los productos ya no está disponible. Revisa la venta."));
            if (renglon.getValue() > CANTIDAD_MAXIMA) {
                throw new CotizacionException("La cantidad de " + producto.getNombre()
                    + " no puede ser mayor a " + CANTIDAD_MAXIMA + ".");
            }
            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(venta);
            detalle.setProducto(producto);
            detalle.setCantidad(renglon.getValue());
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(aCentavos(producto.getPrecio() * renglon.getValue()));
            venta.getDetalles().add(detalle);
            total += detalle.getSubtotal();
        }
        venta.setTotal(aCentavos(total));
        venta.setFolio(generarFolio());

        return VentaMapper.toDTO(ventaRepository.save(venta));
    }

    /** Folio VEN-AAAA-NNNN; las canceladas conservan el suyo. */
    private String generarFolio() {
        int anio = Year.now().getValue();
        long numero = ventaRepository.contarPorAnio(anio) + 1;
        String folio;
        do {
            folio = String.format("VEN-%d-%04d", anio, numero);
            numero++;
        } while (ventaRepository.existsByFolio(folio));
        return folio;
    }

    private static double aCentavos(double monto) {
        return Math.round(monto * 100.0) / 100.0;
    }
}
