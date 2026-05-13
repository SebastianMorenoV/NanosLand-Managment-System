package com.example.negocio.cliente.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.ClienteDTO;
import com.mycompany.common.mapper.ClienteMapper;
import com.mycompany.persistencia.dominio.Cliente;
import com.mycompany.persistencia.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Editar la información de un cliente existente.
 *
 * Responsabilidades (Flujo Alternativo 2.2.3):
 * - Verificar que el cliente exista en la base de datos.
 * - Aplicar las mismas validaciones de campos que el flujo de registro
 *   (nombre y teléfono obligatorios, teléfono ≤ 10 caracteres).
 * - Actualizar y persistir los cambios.
 * - Devolver el DTO enriquecido con los datos actualizados.
 */
@Service
@RequiredArgsConstructor
public class ActualizarClienteUseCase {

    private final ClienteRepository clienteRepository;

    /**
     * Actualiza el nombre, teléfono y correo de un cliente identificado por su ID.
     *
     * @param id       ID del cliente a actualizar. No puede ser nulo.
     * @param nombre   Nuevo nombre completo. Campo obligatorio.
     * @param telefono Nuevo teléfono. Obligatorio, máximo 10 caracteres.
     * @param correo   Nuevo correo electrónico. Opcional; si está vacío se almacena como null.
     * @return ClienteDTO con los datos ya actualizados.
     * @throws CotizacionException si alguna validación falla o el cliente no existe.
     */
    @Transactional
    public ClienteDTO actualizarCliente(ClienteDTO dto) {

        // ── 1. Validar que se proporcionó un ID ──────────────────────────────
        if (dto.getId() == null) {
            throw new CotizacionException("El ID del cliente es obligatorio para actualizar.");
        }

        // ── 2. Verificar existencia del cliente ──────────────────────────────
        Cliente clienteBD = clienteRepository.findById(dto.getId())
                .orElseThrow(() -> new CotizacionException(
                        "No se encontró el cliente con ID " + dto.getId() + ". No se puede editar."));

        // ── 3. Validaciones de campos obligatorios ───────────────────────────
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new CotizacionException("El nombre del cliente es obligatorio.");
        }
        if (dto.getTelefono() == null || dto.getTelefono().trim().isEmpty()) {
            throw new CotizacionException("El teléfono del cliente es obligatorio.");
        }

        // ── 4. Validación de longitud del teléfono (Flujo Alternativo 2.2.2) ─
        String nombreTrim   = dto.getNombre().trim();
        String telefonoTrim = dto.getTelefono().trim();
        if (telefonoTrim.length() > 10) {
            throw new CotizacionException("El teléfono no puede exceder 10 caracteres.");
        }

        // ── 5. Actualizar los campos de la entidad ───────────────────────────
        // Mapeamos el DTO a Entity temporal para extraer los campos ya procesados
        dto.setNombre(nombreTrim);
        dto.setTelefono(telefonoTrim);
        if (dto.getCorreo() != null && !dto.getCorreo().trim().isEmpty()) {
            dto.setCorreo(dto.getCorreo().trim());
        } else {
            dto.setCorreo(null);
        }
        
        Cliente entityMapeada = ClienteMapper.toEntity(dto);
        
        clienteBD.setNombre(entityMapeada.getNombre());
        clienteBD.setTelefono(entityMapeada.getTelefono());
        clienteBD.setCorreo(entityMapeada.getCorreo());
        clienteBD.setDireccion(entityMapeada.getDireccion());

        // ── 6. Persistir y devolver el DTO ───────────────────────────────────
        Cliente actualizado = clienteRepository.save(clienteBD);
        return ClienteMapper.toDTO(actualizado);
    }
}
