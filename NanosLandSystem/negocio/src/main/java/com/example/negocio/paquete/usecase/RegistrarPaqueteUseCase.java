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

import java.util.ArrayList;

@Service
public class RegistrarPaqueteUseCase {

    @Autowired
    private PaqueteRepository paqueteRepository;

    public PaqueteDTO registrarPaquete(PaqueteDTO paqueteDTO) {
        if (paqueteDTO.getNombre() == null || paqueteDTO.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del paquete es obligatorio.");
        }
        if (paqueteDTO.getCostoBase() < 0) {
            throw new IllegalArgumentException("El costo base no puede ser negativo.");
        }

        Paquete p = PaqueteMapper.toEntity(paqueteDTO);
        p.setActivo(true);
        p.setServicios(new ArrayList<>());

        if (paqueteDTO.getServicios() != null) {
            for (PaqueteServicioDTO psDTO : paqueteDTO.getServicios()) {
                PaqueteServicio ps = new PaqueteServicio();
                ps.setPaquete(p);
                ps.setServicio(ServicioMapper.toEntity(psDTO.getServicio()));
                ps.setCantidad(psDTO.getCantidad());
                ps.setSubtotal(0); // Regla de negocio: costo base no suma subtotales
                p.getServicios().add(ps);
            }
        }

        Paquete guardado = paqueteRepository.save(p);
        return PaqueteMapper.toDTO(guardado);
    }
}
