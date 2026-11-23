package com.example.negocio.usuario.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.persistencia.dominio.Usuario;
import com.mycompany.persistencia.enums.RolUsuario;
import com.mycompany.persistencia.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso para eliminar (soft-delete) usuarios del sistema (CU-12).
 */
@Service
@RequiredArgsConstructor
public class EliminarUsuarioSistemaUseCase {

    private final UsuarioRepository usuarioRepository;

    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new CotizacionException("No se encontró el usuario con ID: " + id));

        if (usuario.getRol() == RolUsuario.DUEÑO && usuario.isActivo()
                && usuarioRepository.countByRolAndActivoTrue(RolUsuario.DUEÑO) <= 1) {
            throw new CotizacionException(
                "No se puede desactivar al único dueño activo. Registre antes a otro dueño.");
        }

        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }
}
