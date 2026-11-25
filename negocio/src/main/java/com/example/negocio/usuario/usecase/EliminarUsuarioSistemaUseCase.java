package com.example.negocio.usuario.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.persistencia.dominio.Usuario;
import com.mycompany.persistencia.enums.RolUsuario;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.UsuarioRepository;
import com.mycompany.persistencia.repository.VentaRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baja de usuarios del sistema (CU-12): desactivarlos (no pueden iniciar sesión, pero
 * se pueden volver a activar) o eliminarlos definitivamente.
 *
 * Regla de negocio en ambos casos: el sistema no puede quedarse sin un dueño activo,
 * porque solo el dueño administra los usuarios y nadie podría volver a dar el rol.
 */
@Service
@RequiredArgsConstructor
public class EliminarUsuarioSistemaUseCase {

    private final UsuarioRepository usuarioRepository;
    private final CotizacionRepository cotizacionRepository;
    private final VentaRepository ventaRepository;

    /** Baja lógica: el usuario ya no puede iniciar sesión; se puede reactivar. */
    @Transactional
    public void desactivarUsuario(Long id) {
        Usuario usuario = obtener(id);
        validarQueQuedeUnDueno(usuario, "desactivar");
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    /**
     * Borra la cuenta de la base de datos. Las cotizaciones y ventas que registró se
     * conservan, sin el dato de quién las hizo (ese dato no se muestra en ninguna pantalla).
     *
     * @param solicitanteId usuario con la sesión abierta: no puede eliminarse a sí mismo
     */
    @Transactional
    public void eliminarDefinitivamente(Long id, Long solicitanteId) {
        Usuario usuario = obtener(id);
        if (Objects.equals(id, solicitanteId)) {
            throw new CotizacionException("No puede eliminar su propia cuenta mientras tiene la sesión abierta.");
        }
        validarQueQuedeUnDueno(usuario, "eliminar");

        cotizacionRepository.quitarUsuario(usuario);
        ventaRepository.quitarUsuario(usuario);
        ventaRepository.quitarUsuarioCancelacion(usuario);
        usuarioRepository.delete(usuario);
    }

    private Usuario obtener(Long id) {
        if (id == null) {
            throw new CotizacionException("Debe indicar el usuario.");
        }
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new CotizacionException("No se encontró el usuario con ID: " + id));
    }

    private void validarQueQuedeUnDueno(Usuario usuario, String accion) {
        if (usuario.getRol() == RolUsuario.DUEÑO && usuario.isActivo()
                && usuarioRepository.countByRolAndActivoTrue(RolUsuario.DUEÑO) <= 1) {
            throw new CotizacionException(
                "No se puede " + accion + " al único dueño activo. Registre antes a otro dueño.");
        }
    }
}
