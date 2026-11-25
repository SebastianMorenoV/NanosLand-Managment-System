package com.example.negocio.usuario.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.UsuarioSistemaDTO;
import com.mycompany.common.mapper.UsuarioSistemaMapper;
import com.mycompany.persistencia.dominio.Usuario;
import com.mycompany.persistencia.enums.RolUsuario;
import com.mycompany.persistencia.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Caso de uso para actualizar usuarios del sistema (CU-12).
 */
@Service
@RequiredArgsConstructor
public class ActualizarUsuarioSistemaUseCase {

    private final UsuarioRepository usuarioRepository;

    @Transactional
    public UsuarioSistemaDTO actualizarUsuario(UsuarioSistemaDTO dto) {
        if (dto.getId() == null) {
            throw new CotizacionException("El ID del usuario es obligatorio para actualizar.");
        }

        Usuario existente = usuarioRepository.findById(dto.getId())
                .orElseThrow(() -> new CotizacionException("No se encontró el usuario con ID: " + dto.getId()));

        if (dto.getCorreo() == null || dto.getCorreo().trim().isEmpty()) {
            throw new CotizacionException("El correo es obligatorio.");
        }

        // Verificar que el correo no esté en uso por OTRO usuario
        Optional<Usuario> otro = usuarioRepository.findByCorreo(dto.getCorreo().trim());
        if (otro.isPresent() && !otro.get().getId().equals(dto.getId())) {
            throw new CotizacionException("Ya existe otro usuario con ese correo electrónico.");
        }

        // El sistema no puede quedarse sin un dueño activo: solo el dueño ve la
        // gestión de usuarios, así que nadie podría volver a asignar el rol.
        if (existente.getRol() == RolUsuario.DUEÑO && existente.isActivo()
                && dto.getRol() != RolUsuario.DUEÑO
                && usuarioRepository.countByRolAndActivoTrue(RolUsuario.DUEÑO) <= 1) {
            throw new CotizacionException(
                "No se puede cambiar el rol del único dueño activo. Registre antes a otro dueño.");
        }

        existente.setCorreo(dto.getCorreo().trim());
        existente.setTelefono(dto.getTelefono() != null ? dto.getTelefono().trim() : null);
        existente.setRol(dto.getRol());
        // La contraseña no se cambia aquí: solo con CambiarContrasenaUseCase, que pide la
        // actual cuando es la propia.

        Usuario guardado = usuarioRepository.save(existente);
        return UsuarioSistemaMapper.toDTO(guardado);
    }
}
