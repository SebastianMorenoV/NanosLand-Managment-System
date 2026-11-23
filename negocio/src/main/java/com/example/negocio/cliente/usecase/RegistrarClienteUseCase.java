package com.example.negocio.cliente.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.ClienteDTO;
import com.mycompany.common.mapper.ClienteMapper;
import com.mycompany.persistencia.dominio.Cliente;
import com.mycompany.persistencia.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrarClienteUseCase {

    private final ClienteRepository clienteRepository;

    @Transactional
    public ClienteDTO registrarCliente(ClienteDTO dto) {
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new CotizacionException("El nombre del cliente es obligatorio.");
        }
        if (dto.getTelefono() == null || dto.getTelefono().trim().isEmpty()) {
            throw new CotizacionException("El teléfono del cliente es obligatorio.");
        }

        String nombreTrim = dto.getNombre().trim();
        String telefonoTrim = dto.getTelefono().trim();
        if (telefonoTrim.length() > 10) {
            throw new CotizacionException("El teléfono no puede exceder 10 caracteres.");
        }

        dto.setNombre(nombreTrim);
        dto.setTelefono(telefonoTrim);
        if (dto.getCorreo() != null && !dto.getCorreo().trim().isEmpty()) {
            dto.setCorreo(dto.getCorreo().trim());
        } else {
            dto.setCorreo(null);
        }

        Cliente cliente = ClienteMapper.toEntity(dto);
        Cliente guardado = clienteRepository.save(cliente);
        return ClienteMapper.toDTO(guardado);
    }
}

