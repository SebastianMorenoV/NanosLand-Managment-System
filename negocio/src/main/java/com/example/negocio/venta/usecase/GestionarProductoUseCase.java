package com.example.negocio.venta.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.ProductoDTO;
import com.mycompany.common.mapper.ProductoMapper;
import com.mycompany.persistencia.dominio.Producto;
import com.mycompany.persistencia.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/** Catálogo de productos que se venden aparte de los eventos. */
@Service
@RequiredArgsConstructor
public class GestionarProductoUseCase {

    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<ProductoDTO> obtenerProductosActivos() {
        return productoRepository.findByActivoTrueOrderByNombreAsc().stream()
            .map(ProductoMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public ProductoDTO crearProducto(String nombre, double precio) {
        validar(nombre, precio);
        if (productoRepository.existsByNombreIgnoreCaseAndActivoTrue(nombre.trim())) {
            throw new CotizacionException("Ya existe un producto con ese nombre.");
        }
        Producto producto = new Producto();
        producto.setNombre(nombre.trim());
        producto.setPrecio(aCentavos(precio));
        producto.setActivo(true);
        return ProductoMapper.toDTO(productoRepository.save(producto));
    }

    /** Cambiar el precio no afecta las ventas anteriores: cada venta guarda el suyo. */
    @Transactional
    public ProductoDTO actualizarProducto(Long id, String nombre, double precio) {
        validar(nombre, precio);
        Producto producto = obtenerActivo(id);
        if (productoRepository.existsByNombreIgnoreCaseAndActivoTrueAndIdNot(nombre.trim(), id)) {
            throw new CotizacionException("Ya existe otro producto con ese nombre.");
        }
        producto.setNombre(nombre.trim());
        producto.setPrecio(aCentavos(precio));
        return ProductoMapper.toDTO(producto);
    }

    /** Lo deja de ofrecer; las ventas anteriores conservan su producto. */
    @Transactional
    public void eliminarProducto(Long id) {
        obtenerActivo(id).setActivo(false);
    }

    private Producto obtenerActivo(Long id) {
        if (id == null) {
            throw new CotizacionException("Debe indicar el producto.");
        }
        return productoRepository.findById(id)
            .filter(Producto::isActivo)
            .orElseThrow(() -> new CotizacionException("El producto no existe o ya fue eliminado."));
    }

    private static void validar(String nombre, double precio) {
        if (nombre == null || nombre.isBlank()) {
            throw new CotizacionException("Escribe el nombre del producto.");
        }
        if (nombre.trim().length() > 255) {
            throw new CotizacionException("El nombre es demasiado largo (máximo 255 caracteres).");
        }
        if (Double.isNaN(precio) || Double.isInfinite(precio) || aCentavos(precio) <= 0) {
            throw new CotizacionException("El precio debe ser mayor a cero.");
        }
    }

    private static double aCentavos(double monto) {
        return Math.round(monto * 100.0) / 100.0;
    }
}
