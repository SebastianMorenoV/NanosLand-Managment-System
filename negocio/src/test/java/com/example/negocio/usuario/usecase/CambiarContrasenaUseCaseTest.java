package com.example.negocio.usuario.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.negocio.exception.CotizacionException;
import com.example.negocio.sesion.usecase.Contrasenas;
import com.mycompany.persistencia.dominio.Usuario;
import com.mycompany.persistencia.enums.RolUsuario;
import com.mycompany.persistencia.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CambiarContrasenaUseCaseTest {

    @Mock UsuarioRepository usuarioRepository;

    Contrasenas contrasenas;
    CambiarContrasenaUseCase cambiar;

    Usuario dueno;
    Usuario admin;
    Usuario otroAdmin;

    @BeforeEach
    void setUp() {
        contrasenas = new Contrasenas(usuarioRepository);
        cambiar = new CambiarContrasenaUseCase(usuarioRepository, contrasenas);
        dueno = usuario(1, RolUsuario.DUEÑO, "dueno123");
        admin = usuario(2, RolUsuario.ADMINISTRADOR, "admin123");
        otroAdmin = usuario(3, RolUsuario.ADMINISTRADOR, "otro123");
    }

    private Usuario usuario(long id, RolUsuario rol, String contrasena) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setCorreo("u" + id + "@nanosland.com");
        u.setRol(rol);
        u.setActivo(true);
        u.setContrasena(contrasenas.cifrar(contrasena));
        lenient().when(usuarioRepository.findById(id)).thenReturn(Optional.of(u));
        return u;
    }

    @Test
    void elAdministradorCambiaLaSuyaConLaActual() {
        cambiar.cambiarContrasena(2L, 2L, "admin123", "nueva456");

        assertThat(contrasenas.coincide("nueva456", admin.getContrasena())).isTrue();
        assertThat(Contrasenas.esHash(admin.getContrasena())).isTrue();
        verify(usuarioRepository).save(admin);
    }

    @Test
    void sinLaActualCorrectaNoSeCambiaLaPropia() {
        assertThatThrownBy(() -> cambiar.cambiarContrasena(2L, 2L, "equivocada", "nueva456"))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("actual no es correcta");
        assertThatThrownBy(() -> cambiar.cambiarContrasena(2L, 2L, "", "nueva456"))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("contraseña actual");
        assertThat(contrasenas.coincide("admin123", admin.getContrasena())).isTrue();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void elDuenoTambienNecesitaLaActualParaLaSuya() {
        assertThatThrownBy(() -> cambiar.cambiarContrasena(1L, 1L, null, "nueva456"))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("contraseña actual");

        cambiar.cambiarContrasena(1L, 1L, "dueno123", "nueva456");
        assertThat(contrasenas.coincide("nueva456", dueno.getContrasena())).isTrue();
    }

    @Test
    void elDuenoCambiaLaDeOtroSinSaberLaActual() {
        cambiar.cambiarContrasena(1L, 2L, null, "temporal1");

        assertThat(contrasenas.coincide("temporal1", admin.getContrasena())).isTrue();
        verify(usuarioRepository).save(admin);
    }

    @Test
    void unAdministradorNoCambiaLaDeOtro() {
        assertThatThrownBy(() -> cambiar.cambiarContrasena(2L, 3L, "otro123", "nueva456"))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("Solo el dueño");
        assertThatThrownBy(() -> cambiar.cambiarContrasena(2L, 1L, null, "nueva456"))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("Solo el dueño");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void laNuevaNoPuedeIrVacia() {
        assertThatThrownBy(() -> cambiar.cambiarContrasena(1L, 2L, null, "   "))
                .isInstanceOf(CotizacionException.class).hasMessageContaining("contraseña nueva");
        verify(usuarioRepository, never()).save(any());
    }
}
