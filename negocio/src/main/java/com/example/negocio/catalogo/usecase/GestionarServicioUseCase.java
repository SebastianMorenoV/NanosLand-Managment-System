package com.example.negocio.catalogo.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.ServicioDTO;
import com.mycompany.common.mapper.ServicioMapper;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.repository.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GestionarServicioUseCase {

    private final ServicioRepository servicioRepository;

    @Transactional(readOnly = true)
    public List<ServicioDTO> obtenerServiciosActivos() {
        return servicioRepository.findByActivoTrue().stream()
                .map(ServicioMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ServicioDTO crearServicio(String nombre, double precio, String descripcion) {
        if (nombre == null || nombre.isBlank()) {
            throw new CotizacionException("El nombre del servicio es obligatorio.");
        }
        if (precio < 0) {
            throw new CotizacionException("El precio no puede ser negativo.");
        }

        Servicio entidad = new Servicio();
        entidad.setNombre(nombre.trim());
        entidad.setPrecio(precio);
        entidad.setDescripcion(descripcion != null && !descripcion.isBlank() ? descripcion.trim() : "Sin descripción");
        entidad.setActivo(true);
        entidad = servicioRepository.save(entidad);
        return ServicioMapper.toDTO(entidad);
    }

    @Transactional
    public ServicioDTO actualizarServicio(Long id, String nombre, double precio, String descripcion) {
        if (nombre == null || nombre.isBlank()) {
            throw new CotizacionException("El nombre del servicio es obligatorio.");
        }
        if (precio < 0) {
            throw new CotizacionException("El precio no puede ser negativo.");
        }

        Optional<Servicio> opt = servicioRepository.findById(id);
        if (opt.isEmpty() || !opt.get().isActivo()) {
            throw new CotizacionException("El servicio no existe o ya está eliminado.");
        }

        Servicio entidad = opt.get();
        entidad.setNombre(nombre.trim());
        entidad.setPrecio(precio);
        entidad.setDescripcion(descripcion != null && !descripcion.isBlank() ? descripcion.trim() : "Sin descripción");
        entidad = servicioRepository.save(entidad);
        return ServicioMapper.toDTO(entidad);
    }

    @Transactional
    public void eliminarServicioLogico(Long id) {
        Optional<Servicio> opt = servicioRepository.findById(id);
        if (opt.isEmpty() || !opt.get().isActivo()) {
            throw new CotizacionException("El servicio no existe o ya está eliminado.");
        }

        Servicio servicio = opt.get();
        servicio.setActivo(false);
        servicioRepository.save(servicio);
    }
}
