package com.example.negocio.venta.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.VentaDTO;
import com.mycompany.common.mapper.VentaMapper;
import com.mycompany.persistencia.dominio.Usuario;
import com.mycompany.persistencia.dominio.Venta;
import com.mycompany.persistencia.enums.EstadoVenta;
import com.mycompany.persistencia.enums.RolUsuario;
import com.mycompany.persistencia.repository.UsuarioRepository;
import com.mycompany.persistencia.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cancela una venta capturada por error. No se borra: queda con quién y cuándo
 * la canceló, y deja de contar en los ingresos.
 */
@Service
@RequiredArgsConstructor
public class CancelarVentaUseCase {

    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;

    /** El administrador solo cancela ventas del día; el dueño, de cualquier fecha. */
    @Transactional
    public VentaDTO cancelarVenta(Long ventaId, Long usuarioId) {
        if (ventaId == null) {
            throw new CotizacionException("Debe indicar la venta.");
        }
        Usuario usuario = usuarioId == null ? null : usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null || !usuario.isActivo()) {
            throw new CotizacionException("Inicia sesión para cancelar la venta.");
        }
        Venta venta = ventaRepository.findById(ventaId)
            .orElseThrow(() -> new CotizacionException("La venta no existe."));
        if (venta.getEstado() == EstadoVenta.CANCELADA) {
            throw new CotizacionException("Esta venta ya estaba cancelada.");
        }
        if (usuario.getRol() != RolUsuario.DUEÑO
                && !venta.getFechaHora().toLocalDate().equals(LocalDate.now())) {
            throw new CotizacionException("Solo el dueño puede cancelar ventas de días anteriores.");
        }

        venta.setEstado(EstadoVenta.CANCELADA);
        venta.setFechaCancelacion(LocalDateTime.now());
        venta.setUsuarioCancelacion(usuario);
        return VentaMapper.toDTO(venta);
    }
}
