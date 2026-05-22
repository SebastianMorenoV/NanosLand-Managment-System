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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso para consultar clientes del sistema.
 *
 * Responsabilidades:
 * - Obtener el listado completo de clientes (Flujo Básico 2.1).
 * - Buscar un cliente específico por su ID (Flujo Alternativo 2.2.3 — precarga de formulario).
 * - Filtrar clientes por texto libre en nombre, teléfono o correo (Flujo Alternativo 2.2.1).
 *
 * @author skyro
 */
@Service
@RequiredArgsConstructor
public class BuscarClienteUseCase {

    private final ClienteRepository clienteRepository;

    // ── Flujo Básico 2.1 ─────────────────────────────────────────────────────

    /**
     * Devuelve la entidad Cliente por su ID.
     * Usada internamente por otros use cases que necesitan la entidad directa.
     */
    public Cliente porId(Long id) {
        return clienteRepository.findById(id).orElse(null);
    }

    /**
     * Devuelve un ClienteDTO listo para mostrar/precargar en la UI por su ID.
     * Usado en el Flujo Alternativo 2.2.3 para precargar el formulario de edición.
     */
    public ClienteDTO buscarPorIdDTO(Long id) {
        return clienteRepository.findById(id)
                .map(ClienteMapper::toDTO)
                .orElse(null);
    }

    /**
     * Devuelve el listado completo de clientes ACTIVOS registrados en el sistema.
     * Fix #12: usa findByActivoTrue() para excluir clientes con soft-delete.
     * Fix #11: @Transactional(readOnly=true) para evitar dirty-checking.
     */
    @Transactional(readOnly = true)
    public List<ClienteDTO> obtenerTodos() {
        return ClienteMapper.toDTOList(clienteRepository.findByActivoTrue());
    }

    // ── Flujo Alternativo 2.2.1 — Búsqueda y Filtrado ────────────────────────

    /**
     * Filtra clientes cuyo nombre, teléfono O correo contengan el texto indicado.
     * La búsqueda es case-insensitive. Si el texto está vacío o nulo,
     * devuelve el listado completo (equivale a sin filtro).
     *
     * @param texto Término de búsqueda libre (nombre, teléfono o correo).
     * @return Lista de ClienteDTO que coinciden con el filtro.
     */
    public List<ClienteDTO> buscarPorTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return obtenerTodos();
        }
        String t = texto.trim();
        return ClienteMapper.toDTOList(
                clienteRepository
                        .findByNombreContainingIgnoreCaseOrTelefonoContainingIgnoreCaseOrCorreoContainingIgnoreCase(
                                t, t, t)
        );
    }

    /**
     * Búsqueda interna por nombre (devuelve entidades).
     * Mantenida por compatibilidad con otros use cases existentes.
     */
    public List<Cliente> porNombre(String nombre) {
        return clienteRepository.findByNombreContainingIgnoreCase(nombre);
    }
}

