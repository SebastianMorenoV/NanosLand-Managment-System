package com.example.negocio.paquete.usecase;

import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.mapper.PaqueteMapper;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.repository.PaqueteRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Da de alta un paquete con sus servicios incluidos y, si su precio depende de la comida
 * y el número de personas, su tabla de precios (en ese caso el precio fijo queda en 0).
 */
@Service
@RequiredArgsConstructor
public class RegistrarPaqueteUseCase {

    private final PaqueteRepository paqueteRepository;
    private final ValidadorPaquete validador;

    @Transactional
    public PaqueteDTO registrarPaquete(PaqueteDTO paqueteDTO) {
        List<ValidadorPaquete.Precio> precios = validador.precios(paqueteDTO == null ? null : paqueteDTO.getPrecios());
        boolean conTabla = !precios.isEmpty();
        validador.validarDatosGenerales(paqueteDTO, conTabla);
        List<ValidadorPaquete.Incluido> incluidos = validador.serviciosIncluidos(paqueteDTO.getServicios());

        Paquete p = PaqueteMapper.toEntity(paqueteDTO);
        p.setId(null);
        p.setNombre(paqueteDTO.getNombre().trim());
        p.setCosto(conTabla ? 0 : com.example.negocio.precio.Dinero.aCentavos(paqueteDTO.getCostoBase()));
        p.setActivo(true);
        p.setServicios(new ArrayList<>());
        p.setPrecios(new ArrayList<>());
        validador.aplicarServicios(p, incluidos);
        validador.aplicarPrecios(p, precios);

        Paquete guardado = paqueteRepository.save(p);
        return PaqueteMapper.toDTO(guardado);
    }
}
