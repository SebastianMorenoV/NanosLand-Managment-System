package com.example.negocio.usuario.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.persistencia.dominio.Usuario;
import com.mycompany.persistencia.enums.RolUsuario;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.UsuarioRepository;
import com.mycompany.persistencia.repository.VentaRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EliminarUsuarioSistemaUseCaseTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock CotizacionRepository cotizacionRepository;
    @Mock VentaRepository ventaRepository;

    EliminarUsuarioSistemaUseCase casoDeUso;

    Usuario dueno = usuario(1, RolUsuario.DUEÑO, true);
    Usuario admin = usuario(2, RolUsuario.ADMINISTRADOR, true);

    @BeforeEach
    void setUp() {
        casoDeUso = new EliminarUsuarioSistemaUseCase(usuarioRepository, cotizacionRepository, ventaRepository);
        lenient().when(usuarioRepository.findById(1L)).thenReturn(Optional.of(dueno));
        lenient().when(usuarioRepository.findById(2L)).thenReturn(Optional.of(admin));
    }

    private static Usuario usuario(long id, RolUsuario rol, boolean activo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setCorreo("u" + id + "@nanosland.com");
        u.setRol(rol);
        u.setActivo(activo);
        return u;
    }

    @Test
    void desactivarEsBajaLogica() {
        casoDeUso.desactivarUsuario(2L);

        assertThat(admin.isActivo()).isFalse();
        verify(usuarioRepository).save(admin);
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void noSeDesactivaAlUnicoDuenoActivo() {
        when(usuarioRepository.countByRolAndActivoTrue(RolUsuario.DUEÑO)).thenReturn(1L);

        assertThatThrownBy(() -> casoDeUso.desactivarUsuario(1L))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("único dueño activo");
        assertThat(dueno.isActivo()).isTrue();
    }

    @Test
    void unDuenoSePuedeDesactivarSiHayOtroDuenoActivo() {
        when(usuarioRepository.countByRolAndActivoTrue(RolUsuario.DUEÑO)).thenReturn(2L);

        casoDeUso.desactivarUsuario(1L);

        assertThat(dueno.isActivo()).isFalse();
    }

    @Test
    void eliminarConservaCotizacionesYVentasYBorraLaCuenta() {
        casoDeUso.eliminarDefinitivamente(2L, 1L);

        InOrder orden = inOrder(cotizacionRepository, ventaRepository, usuarioRepository);
        orden.verify(cotizacionRepository).quitarUsuario(admin);
        orden.verify(ventaRepository).quitarUsuario(admin);
        orden.verify(ventaRepository).quitarUsuarioCancelacion(admin);
        orden.verify(usuarioRepository).delete(admin);
    }

    @Test
    void nadieSeEliminaASiMismo() {
        assertThatThrownBy(() -> casoDeUso.eliminarDefinitivamente(2L, 2L))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("propia cuenta");
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void noSeEliminaAlUnicoDuenoActivo() {
        when(usuarioRepository.countByRolAndActivoTrue(RolUsuario.DUEÑO)).thenReturn(1L);

        assertThatThrownBy(() -> casoDeUso.eliminarDefinitivamente(1L, 2L))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("único dueño activo");
        verify(cotizacionRepository, never()).quitarUsuario(any());
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void unDuenoInactivoSePuedeEliminar() {
        dueno.setActivo(false);

        casoDeUso.eliminarDefinitivamente(1L, 2L);

        verify(usuarioRepository).delete(dueno);
    }
}
