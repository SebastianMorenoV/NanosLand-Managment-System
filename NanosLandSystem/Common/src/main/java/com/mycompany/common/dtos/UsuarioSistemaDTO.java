package com.mycompany.common.dtos;

import com.mycompany.persistencia.enums.RolUsuario;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la gestión de usuarios del sistema (CU-12).
 */
@Data
@NoArgsConstructor
public class UsuarioSistemaDTO {

    private Long id;
    private String correo;
    private String contrasena; // Solo se usa para crear/editar, nunca se devuelve en lectura
    private String telefono;
    private RolUsuario rol;
    private boolean activo;
}
