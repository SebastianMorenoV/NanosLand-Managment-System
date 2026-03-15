/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.negocio.cliente.usecase;

import com.mycompany.common.dtos.ClienteDTO;
import com.mycompany.common.mapper.ClienteMapper;
import com.mycompany.persistencia.dominio.Cliente;
import com.mycompany.persistencia.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *
 * @author skyro
 */
@Service
@RequiredArgsConstructor
public class BuscarClienteUseCase {

    private final ClienteRepository clienteRepository;

    public Cliente porId(Long id) {
        return clienteRepository.findById(id).orElse(null);
    }

    public List<Cliente> porNombre(String nombre) {
        return clienteRepository.findByNombreContainingIgnoreCase(nombre);
    }

    public List<ClienteDTO> obtenerTodos() {
        return ClienteMapper.toDTOList(clienteRepository.findAll());
    }
}
