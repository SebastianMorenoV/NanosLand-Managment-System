/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.persistencia.repository;

import com.mycompany.persistencia.dominio.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Cliente.
 *
 * @author skyro
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // ── Flujo Básico 2.1 ────────────────────────────────────────────────────
    /** Búsqueda por nombre (case-insensitive). Usada internamente. */
    List<Cliente> findByNombreContainingIgnoreCase(String nombre);

    // ── Flujo Alternativo 2.2.1 — Búsqueda reactiva ─────────────────────────
    /**
     * Busca clientes cuyo nombre, teléfono O correo contengan el texto dado.
     * La búsqueda ignora diferencias de mayúsculas/minúsculas en los tres campos,
     * permitiendo filtrar la tabla desde la barra de búsqueda en tiempo real.
     */
    List<Cliente> findByNombreContainingIgnoreCaseOrTelefonoContainingIgnoreCaseOrCorreoContainingIgnoreCase(
            String nombre, String telefono, String correo);

}
