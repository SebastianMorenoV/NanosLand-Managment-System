package com.mycompany.common.mapper;

import com.mycompany.common.dtos.UsuarioSistemaDTO;
import com.mycompany.persistencia.dominio.Usuario;

import java.util.List;
import java.util.stream.Collectors;

public class UsuarioSistemaMapper {

    public static UsuarioSistemaDTO toDTO(Usuario usuario) {
        if (usuario == null) return null;
        UsuarioSistemaDTO dto = new UsuarioSistemaDTO();
        dto.setId(usuario.getId());
        dto.setCorreo(usuario.getCorreo());
        // No se copia la contraseña al DTO por seguridad
        dto.setTelefono(usuario.getTelefono());
        dto.setRol(usuario.getRol());
        dto.setActivo(usuario.isActivo());
        return dto;
    }

    public static Usuario toEntity(UsuarioSistemaDTO dto) {
        if (dto == null) return null;
        Usuario usuario = new Usuario();
        usuario.setId(dto.getId());
        usuario.setCorreo(dto.getCorreo());
        usuario.setContrasena(dto.getContrasena());
        usuario.setTelefono(dto.getTelefono());
        usuario.setRol(dto.getRol());
        usuario.setActivo(dto.isActivo());
        return usuario;
    }

    public static List<UsuarioSistemaDTO> toDTOList(List<Usuario> usuarios) {
        if (usuarios == null) return null;
        return usuarios.stream()
                .map(UsuarioSistemaMapper::toDTO)
                .collect(Collectors.toList());
    }
}
