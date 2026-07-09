package com.example.negocio.sesion.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.persistencia.dominio.Usuario;
import com.mycompany.persistencia.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso para iniciar sesión en el sistema (CU-01).
 *
 * Responsabilidades:
 * - Validar que el correo exista en la BD y esté activo.
 * - Validar que la contraseña coincida.
 * - Devolver la entidad Usuario autenticada.
 */
@Service
@RequiredArgsConstructor
public class IniciarSesionUseCase {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Usuario iniciarSesion(String correo, String contrasena) {
        if (correo == null || correo.trim().isEmpty()) {
            throw new CotizacionException("El correo es obligatorio.");
        }
        if (contrasena == null || contrasena.trim().isEmpty()) {
            throw new CotizacionException("La contraseña es obligatoria.");
        }

        Usuario usuario = usuarioRepository.findByCorreoAndActivoTrue(correo.trim())
                .orElseThrow(() -> new CotizacionException("No se encontró un usuario activo con ese correo."));

        if (!usuario.getContrasena().equals(contrasena)) {
            throw new CotizacionException("Contraseña incorrecta.");
        }

        return usuario;
    }
}
