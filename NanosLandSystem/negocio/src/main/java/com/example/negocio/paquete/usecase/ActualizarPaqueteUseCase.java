package com.example.negocio.paquete.usecase;

import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.dtos.PaqueteServicioDTO;
import com.mycompany.common.mapper.PaqueteMapper;
import com.mycompany.common.mapper.ServicioMapper;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.dominio.PaqueteServicio;
import com.mycompany.persistencia.repository.PaqueteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ActualizarPaqueteUseCase {

    @Autowired
    private PaqueteRepository paqueteRepository;

    @Autowired
    private com.mycompany.persistencia.repository.CotizacionRepository cotizacionRepository;

    public PaqueteDTO actualizarPaquete(PaqueteDTO paqueteDTO) {
        if (paqueteDTO.getId() == null) {
            throw new IllegalArgumentException("El ID del paquete es obligatorio para actualizar.");
        }
        if (paqueteDTO.getNombre() == null || paqueteDTO.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del paquete es obligatorio.");
        }
        if (paqueteDTO.getCostoBase() < 0) {
            throw new IllegalArgumentException("El costo base no puede ser negativo.");
        }

        Optional<Paquete> opt = paqueteRepository.findById(paqueteDTO.getId());
        if (opt.isEmpty() || !opt.get().isActivo()) {
            throw new com.example.negocio.exception.CotizacionException("El paquete no existe o no está activo.");
        }

        boolean enUso = cotizacionRepository.existsByPaqueteIdAndEstadoIn(paqueteDTO.getId(), 
            java.util.List.of(com.mycompany.persistencia.enums.EstadoCotizacion.BORRADOR, 
                              com.mycompany.persistencia.enums.EstadoCotizacion.VIGENTE));
        
        if (enUso) {
            throw new com.example.negocio.exception.CotizacionException(
                "No se puede modificar el paquete porque tiene cotizaciones activas o vigentes asociadas. " +
                "Si desea cambiar el precio o los servicios, por favor cree un nuevo paquete para no afectar las cotizaciones existentes.");
        }

        Paquete p = opt.get();
        p.setNombre(paqueteDTO.getNombre());
        p.setCosto(paqueteDTO.getCostoBase());
        p.setDescripcion(paqueteDTO.getDescripcion());

        // Actualizar servicios (limpiar y recrear para simplificar, o actualizar)
        p.getServicios().clear();
        
        if (paqueteDTO.getServicios() != null) {
            for (PaqueteServicioDTO psDTO : paqueteDTO.getServicios()) {
                PaqueteServicio ps = new PaqueteServicio();
                ps.setPaquete(p);
                ps.setServicio(ServicioMapper.toEntity(psDTO.getServicio()));
                ps.setCantidad(psDTO.getCantidad());
                double precioSvc = psDTO.getServicio() != null ? psDTO.getServicio().getPrecio() : 0.0;
                ps.setSubtotal(precioSvc * psDTO.getCantidad());
                p.getServicios().add(ps);
            }
        }

        Paquete guardado = paqueteRepository.save(p);
        return PaqueteMapper.toDTO(guardado);
    }
}
