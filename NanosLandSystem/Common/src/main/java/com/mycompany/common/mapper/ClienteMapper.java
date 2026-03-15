package com.mycompany.common.mapper;

import com.mycompany.common.dtos.ClienteDTO;
import com.mycompany.persistencia.dominio.Cliente;

import java.util.List;
import java.util.stream.Collectors;

public class ClienteMapper {

    public static ClienteDTO toDTO(Cliente cliente) {
        if (cliente == null) return null;
        ClienteDTO dto = new ClienteDTO();
        dto.setId(cliente.getId());
        dto.setNombre(cliente.getNombre());
        dto.setTelefono(cliente.getTelefono());
        dto.setCorreo(cliente.getCorreo());
        return dto;
    }

    public static Cliente toEntity(ClienteDTO dto) {
        if (dto == null) return null;
        Cliente cliente = new Cliente();
        cliente.setId(dto.getId());
        cliente.setNombre(dto.getNombre());
        cliente.setTelefono(dto.getTelefono());
        cliente.setCorreo(dto.getCorreo());
        return cliente;
    }

    public static List<ClienteDTO> toDTOList(List<Cliente> clientes) {
        if (clientes == null) return null;
        return clientes.stream()
                .map(ClienteMapper::toDTO)
                .collect(Collectors.toList());
    }

    public static List<Cliente> toEntityList(List<ClienteDTO> dtos) {
        if (dtos == null) return null;
        return dtos.stream()
                .map(ClienteMapper::toEntity)
                .collect(Collectors.toList());
    }
}
