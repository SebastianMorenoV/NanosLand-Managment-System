package com.example.negocio.usuario.usecase;

import com.example.negocio.exception.CotizacionException;
import com.example.negocio.sesion.usecase.Contrasenas;
import com.mycompany.persistencia.dominio.Usuario;
import com.mycompany.persistencia.enums.RolUsuario;
import com.mycompany.persistencia.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cambio de contraseña de un usuario del sistema.
 * <ul>
 *   <li>Para cambiar la propia, cualquier usuario (dueño o administrador) debe escribir la actual.</li>
 *   <li>El dueño puede cambiar la de otro usuario sin conocer la actual (por ejemplo, si la olvidó).</li>
 *   <li>Un administrador solo puede cambiar la suya.</li>
 * </ul>
 * Es la única forma de cambiar una contraseña ya creada.
 */
@Service
@RequiredArgsConstructor
public class CambiarContrasenaUseCase {

    private final UsuarioRepository usuarioRepository;
    private final Contrasenas contrasenas;

    /**
     * @param solicitanteId usuario con la sesión abierta, que pide el cambio.
     * @param usuarioId     usuario al que se le cambia la contraseña.
     * @param actual        contraseña actual; solo se pide (y se revisa) si es la propia.
     * @param nueva         contraseña nueva.
     */
    @Transactional
    public void cambiarContrasena(Long solicitanteId, Long usuarioId, String actual, String nueva) {
        if (solicitanteId == null || usuarioId == null) {
            throw new CotizacionException("Debe indicar el usuario.");
        }
        if (nueva == null || nueva.trim().isEmpty()) {
            throw new CotizacionException("Escriba la contraseña nueva.");
        }

        // El rol se lee de la base, no de la sesión, por si cambió después de iniciarla.
        Usuario solicitante = usuarioRepository.findById(solicitanteId)
                .orElseThrow(() -> new CotizacionException("No se encontró el usuario con la sesión abierta."));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new CotizacionException("No se encontró el usuario indicado."));

        if (solicitante.getId().equals(usuario.getId())) {
            if (actual == null || actual.isEmpty()) {
                throw new CotizacionException("Escriba su contraseña actual.");
            }
            if (!contrasenas.coincide(actual, usuario.getContrasena())) {
                throw new CotizacionException("La contraseña actual no es correcta.");
            }
        } else if (solicitante.getRol() != RolUsuario.DUEÑO || !solicitante.isActivo()) {
            throw new CotizacionException("Solo el dueño puede cambiar la contraseña de otro usuario.");
        }

        usuario.setContrasena(contrasenas.cifrar(nueva));
        usuarioRepository.save(usuario);
    }
}
