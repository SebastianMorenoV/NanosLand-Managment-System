package com.mycompany.presentacion.context;

import com.mycompany.persistencia.dominio.Usuario;
import com.mycompany.persistencia.enums.RolUsuario;
import lombok.Data;
import org.springframework.stereotype.Service;

/**
 * Contexto de sesión global del usuario autenticado.
 * Sigue el mismo patrón que CotizacionContext.
 */
@Service
@Data
public class SesionContext {

    private Usuario usuarioAutenticado;

    public boolean esDueno() {
        return usuarioAutenticado != null && usuarioAutenticado.getRol() == RolUsuario.DUEÑO;
    }

    public boolean estaAutenticado() {
        return usuarioAutenticado != null;
    }

    public boolean esAdministrador() {
        return usuarioAutenticado != null && usuarioAutenticado.getRol() == RolUsuario.ADMINISTRADOR;
    }

    public void cerrarSesion() {
        this.usuarioAutenticado = null;
    }
}
