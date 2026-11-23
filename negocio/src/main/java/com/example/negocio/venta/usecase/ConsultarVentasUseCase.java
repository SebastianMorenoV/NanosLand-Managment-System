package com.example.negocio.venta.usecase;

import com.mycompany.common.dtos.VentaDTO;
import com.mycompany.common.mapper.VentaMapper;
import com.mycompany.persistencia.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultarVentasUseCase {

    private final VentaRepository ventaRepository;

    /** Ventas del día, incluidas las canceladas; las más recientes primero. */
    @Transactional(readOnly = true)
    public List<VentaDTO> obtenerVentasDelDia(LocalDate dia) {
        return VentaMapper.toDTOList(
            ventaRepository.findConDetallesEntre(dia.atStartOfDay(), dia.plusDays(1).atStartOfDay()));
    }
}
