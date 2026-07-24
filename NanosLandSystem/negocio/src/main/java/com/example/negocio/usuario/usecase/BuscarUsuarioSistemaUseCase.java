package com.example.negocio.usuario.usecase;

import com.mycompany.common.dtos.UsuarioSistemaDTO;
import com.mycompany.common.mapper.UsuarioSistemaMapper;
import com.mycompany.persistencia.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso para consultar usuarios del sistema (CU-12).
 */
@Service
@RequiredArgsConstructor
public class BuscarUsuarioSistemaUseCase {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<UsuarioSistemaDTO> obtenerTodos() {
        return UsuarioSistemaMapper.toDTOList(usuarioRepository.findAll());
    }

    @Transactional(readOnly = true)
    public UsuarioSistemaDTO buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .map(UsuarioSistemaMapper::toDTO)
                .orElse(null);
    }
}
