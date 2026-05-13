package com.mycompany.common.mapper;

import com.mycompany.common.dtos.ClienteDTO;
import com.mycompany.persistencia.dominio.Cliente;
import com.mycompany.persistencia.dominio.Direccion;

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

        // Mapear dirección (puede ser null en registros antiguos)
        Direccion d = cliente.getDireccion();
        if (d != null) {
            dto.setCalle(d.getCalle());
            dto.setColonia(d.getColonia());
            dto.setCiudad(d.getCiudad());
            dto.setCodigoPostal(d.getCodigoPostal());
        }
        return dto;
    }

    public static Cliente toEntity(ClienteDTO dto) {
        if (dto == null) return null;
        Cliente cliente = new Cliente();
        cliente.setId(dto.getId());
        cliente.setNombre(dto.getNombre());
        cliente.setTelefono(dto.getTelefono());
        cliente.setCorreo(dto.getCorreo());

        // Construir Direccion solo si hay al menos un campo con valor
        if (dto.getCalle() != null || dto.getColonia() != null
                || dto.getCiudad() != null || dto.getCodigoPostal() != null) {
            Direccion d = new Direccion();
            d.setCalle(dto.getCalle());
            d.setColonia(dto.getColonia());
            d.setCiudad(dto.getCiudad());
            d.setCodigoPostal(dto.getCodigoPostal());
            cliente.setDireccion(d);
        }
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

