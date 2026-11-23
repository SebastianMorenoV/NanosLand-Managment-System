package com.example.negocio.usuario.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.UsuarioSistemaDTO;
import com.mycompany.common.mapper.UsuarioSistemaMapper;
import com.mycompany.persistencia.dominio.Usuario;
import com.mycompany.persistencia.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso para registrar nuevos usuarios del sistema (CU-12).
 */
@Service
@RequiredArgsConstructor
public class RegistrarUsuarioSistemaUseCase {

    private final UsuarioRepository usuarioRepository;

    @Transactional
    public UsuarioSistemaDTO registrarUsuario(UsuarioSistemaDTO dto) {
        if (dto.getCorreo() == null || dto.getCorreo().trim().isEmpty()) {
            throw new CotizacionException("El correo es obligatorio.");
        }
        if (dto.getContrasena() == null || dto.getContrasena().trim().isEmpty()) {
            throw new CotizacionException("La contraseña es obligatoria.");
        }
        if (dto.getRol() == null) {
            throw new CotizacionException("Debe seleccionar un rol para el usuario.");
        }

        // Verificar correo único
        if (usuarioRepository.findByCorreo(dto.getCorreo().trim()).isPresent()) {
            throw new CotizacionException("Ya existe un usuario con ese correo electrónico.");
        }

        Usuario usuario = new Usuario();
        usuario.setCorreo(dto.getCorreo().trim());
        usuario.setContrasena(dto.getContrasena());
        usuario.setTelefono(dto.getTelefono() != null ? dto.getTelefono().trim() : null);
        usuario.setRol(dto.getRol());
        usuario.setActivo(true);

        Usuario guardado = usuarioRepository.save(usuario);
        return UsuarioSistemaMapper.toDTO(guardado);
    }
}
