package com.example.negocio.usuario.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.UsuarioSistemaDTO;
import com.mycompany.common.mapper.UsuarioSistemaMapper;
import com.mycompany.persistencia.dominio.Usuario;
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

        existente.setCorreo(dto.getCorreo().trim());
        existente.setTelefono(dto.getTelefono() != null ? dto.getTelefono().trim() : null);
        existente.setRol(dto.getRol());

        // Solo actualizar contraseña si se proporcionó una nueva
        if (dto.getContrasena() != null && !dto.getContrasena().trim().isEmpty()) {
            existente.setContrasena(dto.getContrasena());
        }

        Usuario guardado = usuarioRepository.save(existente);
        return UsuarioSistemaMapper.toDTO(guardado);
    }
}
