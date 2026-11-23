package com.example.negocio.sesion.usecase;

import com.mycompany.persistencia.dominio.Usuario;
import com.mycompany.persistencia.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Manejo de contraseñas de los usuarios del sistema.
 *
 * Las contraseñas se guardan como hash BCrypt (con sal incluida), nunca en
 * texto plano: quien vea la tabla de usuarios no puede leerlas ni recuperarlas.
 */
@Service
@RequiredArgsConstructor
public class Contrasenas {

    /** Formato de un hash BCrypt: $2a$/$2b$/$2y$ + costo + 53 caracteres. */
    private static final Pattern HASH_BCRYPT = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UsuarioRepository usuarioRepository;

    /** Devuelve el hash con el que se guarda una contraseña. */
    public String cifrar(String contrasena) {
        return encoder.encode(contrasena);
    }

    /** Indica si el valor guardado ya es un hash (y no una contraseña en texto plano). */
    public static boolean esHash(String guardada) {
        return guardada != null && HASH_BCRYPT.matcher(guardada).matches();
    }

    /**
     * Compara la contraseña tecleada con la guardada. Acepta también valores
     * antiguos en texto plano (usuarios creados antes del cifrado o cargados
     * con un script), que se convierten con {@link #migrarTextoPlano()}.
     */
    public boolean coincide(String tecleada, String guardada) {
        if (tecleada == null || guardada == null) {
            return false;
        }
        return esHash(guardada) ? encoder.matches(tecleada, guardada) : guardada.equals(tecleada);
    }

    /**
     * Convierte a hash las contraseñas que estén guardadas en texto plano.
     * Se ejecuta al abrir la aplicación y al iniciar sesión.
     *
     * @return cantidad de usuarios convertidos.
     */
    @Transactional
    public int migrarTextoPlano() {
        int convertidos = 0;
        for (Usuario usuario : usuarioRepository.findAll()) {
            String guardada = usuario.getContrasena();
            if (guardada != null && !guardada.isEmpty() && !esHash(guardada)) {
                usuario.setContrasena(cifrar(guardada));
                convertidos++;
            }
        }
        return convertidos;
    }
}
