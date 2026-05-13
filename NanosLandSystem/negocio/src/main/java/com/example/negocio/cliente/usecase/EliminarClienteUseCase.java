package com.example.negocio.cliente.usecase;

import com.example.negocio.exception.CotizacionException;
import com.mycompany.persistencia.dominio.Cliente;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.repository.ClienteRepository;
import com.mycompany.persistencia.repository.CotizacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Eliminar un cliente del sistema.
 *
 * Responsabilidades (Flujo Alternativo 2.2.4):
 * - Verificar que el cliente exista.
 * - Proteger la integridad histórica: bloquear la eliminación si el cliente
 *   tiene cotizaciones en estado VIGENTE (evento activo/confirmado).
 *   Se permiten cotizaciones en BORRADOR, CANCELADA o ELIMINADA.
 * - Eliminar físicamente el registro si pasa todas las validaciones.
 */
@Service
@RequiredArgsConstructor
public class EliminarClienteUseCase {

    private final ClienteRepository clienteRepository;
    private final CotizacionRepository cotizacionRepository;

    /**
     * Elimina al cliente identificado por su ID, previa validación de integridad.
     *
     * @param id ID del cliente a eliminar. No puede ser nulo.
     * @throws CotizacionException si el cliente no existe o tiene eventos activos (VIGENTE).
     */
    @Transactional
    public void eliminarCliente(Long id) {

        // ── 1. Validar que se proporcionó un ID ──────────────────────────────
        if (id == null) {
            throw new CotizacionException("El ID del cliente es obligatorio para eliminar.");
        }

        // ── 2. Verificar existencia del cliente ──────────────────────────────
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new CotizacionException(
                        "No se encontró el cliente con ID " + id + ". No se puede eliminar."));

        // ── 3. Proteger cotizaciones históricas (Flujo 2.2.4, paso 4) ────────
        //    Se bloquea la eliminación únicamente si hay cotizaciones VIGENTES
        //    (evento confirmado, con fecha bloqueada). Los borradores, cancelados
        //    y eliminados no impiden la operación.
        boolean tieneEventosVigentes = cotizacionRepository
                .findByClienteId(id)
                .stream()
                .anyMatch(c -> c.getEstado() == EstadoCotizacion.VIGENTE);

        if (tieneEventosVigentes) {
            throw new CotizacionException(
                    "No se puede eliminar al cliente \"" + cliente.getNombre() + "\" porque "
                    + "tiene eventos vigentes activos. Cancele o finalice esos eventos antes "
                    + "de eliminar al cliente."
            );
        }

        // ── 4. Eliminar físicamente el registro ──────────────────────────────
        clienteRepository.deleteById(id);
    }
}
