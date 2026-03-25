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
    public ClienteDTO registrarCliente(String nombre, String telefono, String correo) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new CotizacionException("El nombre del cliente es obligatorio.");
        }
        if (telefono == null || telefono.trim().isEmpty()) {
            throw new CotizacionException("El teléfono del cliente es obligatorio.");
        }

        String nombreTrim = nombre.trim();
        String telefonoTrim = telefono.trim();
        if (telefonoTrim.length() > 10) {
            throw new CotizacionException("El teléfono no puede exceder 10 caracteres.");
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(nombreTrim);
        cliente.setTelefono(telefonoTrim);
        cliente.setCorreo(correo != null && !correo.trim().isEmpty() ? correo.trim() : null);

        Cliente guardado = clienteRepository.save(cliente);
        return ClienteMapper.toDTO(guardado);
    }
}

