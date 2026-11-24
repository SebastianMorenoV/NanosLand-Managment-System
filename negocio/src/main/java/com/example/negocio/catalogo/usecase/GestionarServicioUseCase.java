package com.example.negocio.catalogo.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.ServicioDTO;
import com.mycompany.common.mapper.ServicioMapper;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.enums.CategoriaServicio;
import com.mycompany.persistencia.repository.PaquetePrecioRepository;
import com.mycompany.persistencia.repository.PaqueteServicioRepository;
import com.mycompany.persistencia.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GestionarServicioUseCase {

    private final ServicioRepository servicioRepository;
    private final PaqueteServicioRepository paqueteServicioRepository;
    private final PaquetePrecioRepository paquetePrecioRepository;

    @Transactional(readOnly = true)
    public List<ServicioDTO> obtenerServiciosActivos() {
        return servicioRepository.findByActivoTrue().stream()
                .map(ServicioMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ServicioDTO crearServicio(String nombre, double precio, String descripcion, CategoriaServicio categoria) {
        CategoriaServicio cat = categoria != null ? categoria : CategoriaServicio.EXTRA;
        validar(nombre, precio);

        Servicio entidad = new Servicio();
        entidad.setNombre(nombre.trim());
        entidad.setCategoria(cat);
        entidad.setPrecio(precioSegunCategoria(precio, cat));
        entidad.setDescripcion(descripcion != null && !descripcion.isBlank() ? descripcion.trim() : "Sin descripción");
        entidad.setActivo(true);
        entidad = servicioRepository.save(entidad);
        return ServicioMapper.toDTO(entidad);
    }

    @Transactional
    public ServicioDTO actualizarServicio(Long id, String nombre, double precio, String descripcion, CategoriaServicio categoria) {
        validar(nombre, precio);
        Servicio entidad = obtenerActivo(id);
        CategoriaServicio nueva = categoria != null ? categoria : entidad.getCategoria();
        validarCambioDeCategoria(entidad, nueva);

        entidad.setNombre(nombre.trim());
        entidad.setCategoria(nueva);
        entidad.setPrecio(precioSegunCategoria(precio, nueva));
        entidad.setDescripcion(descripcion != null && !descripcion.isBlank() ? descripcion.trim() : "Sin descripción");
        entidad = servicioRepository.save(entidad);
        return ServicioMapper.toDTO(entidad);
    }

    @Transactional
    public void eliminarServicioLogico(Long id) {
        Servicio servicio = obtenerActivo(id);

        List<String> paquetes = paqueteServicioRepository.nombresPaquetesActivosConServicio(id);
        if (!paquetes.isEmpty()) {
            throw new CotizacionException("No se puede eliminar el servicio porque forma parte de "
                + enPaquetes(paquetes) + ". Quítelo de ahí primero.");
        }
        List<String> conTabla = paquetePrecioRepository.nombresPaquetesActivosConComida(id);
        if (!conTabla.isEmpty()) {
            throw new CotizacionException("No se puede eliminar la comida porque está en la tabla de precios de "
                + enPaquetes(conTabla) + ". Quítela de ahí primero.");
        }
        servicio.setActivo(false);
        servicioRepository.save(servicio);
    }

    /**
     * Una comida no puede ir como servicio incluido de un paquete (va en su tabla de precios),
     * y una comida usada en una tabla de precios no puede dejar de ser comida.
     */
    private void validarCambioDeCategoria(Servicio servicio, CategoriaServicio nueva) {
        CategoriaServicio actual = servicio.getCategoria();
        if (actual == nueva) return;
        if (nueva == CategoriaServicio.COMIDA) {
            List<String> paquetes = paqueteServicioRepository.nombresPaquetesActivosConServicio(servicio.getId());
            if (!paquetes.isEmpty()) {
                throw new CotizacionException("No se puede convertir en comida porque es un servicio incluido de "
                    + enPaquetes(paquetes) + ". Quítelo de ahí primero.");
            }
        }
        if (actual == CategoriaServicio.COMIDA) {
            List<String> paquetes = paquetePrecioRepository.nombresPaquetesActivosConComida(servicio.getId());
            if (!paquetes.isEmpty()) {
                throw new CotizacionException("No se puede cambiar la categoría porque es una comida de la tabla de precios de "
                    + enPaquetes(paquetes) + ". Quítela de ahí primero.");
            }
        }
    }

    private Servicio obtenerActivo(Long id) {
        return servicioRepository.findById(id)
                .filter(Servicio::isActivo)
                .orElseThrow(() -> new CotizacionException("El servicio no existe o ya está eliminado."));
    }

    private static void validar(String nombre, double precio) {
        if (nombre == null || nombre.isBlank()) {
            throw new CotizacionException("El nombre del servicio es obligatorio.");
        }
        if (Double.isNaN(precio) || Double.isInfinite(precio) || precio < 0) {
            throw new CotizacionException("El precio no puede ser negativo.");
        }
    }

    /** El precio de una comida lo pone la tabla de cada paquete; el del servicio queda en 0. */
    private static double precioSegunCategoria(double precio, CategoriaServicio categoria) {
        return categoria == CategoriaServicio.COMIDA ? 0 : Math.round(precio * 100) / 100.0;
    }

    private static String enPaquetes(List<String> nombres) {
        return (nombres.size() == 1 ? "el paquete " : "los paquetes ") + String.join(", ", nombres);
    }
}
