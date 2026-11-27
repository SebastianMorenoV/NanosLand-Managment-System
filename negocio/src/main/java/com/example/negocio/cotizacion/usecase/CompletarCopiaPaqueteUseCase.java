package com.example.negocio.cotizacion.usecase;

import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.repository.CotizacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Corrección al arrancar: guarda la copia del paquete en las cotizaciones que son de antes de
 * esa copia: borradores, confirmadas con evento próximo o en curso, y las de eventos ya realizados.
 * <ul>
 *   <li>Solo escribe la copia (campos y tabla nuevos) donde está vacía: no cambia ningún dato que ya
 *       exista, y si se vuelve a ejecutar no hace nada.</li>
 *   <li>No cambia nada para la cotización: la copia es el paquete del catálogo, que es justo lo que
 *       la cotización usa hoy en su contrato y su logística.</li>
 *   <li>Es fiel a lo vendido. En las abiertas, el sistema no dejaba cambiar los servicios incluidos
 *       de su paquete. En las de eventos ya realizados, al publicar esta versión (noviembre de 2026)
 *       los dueños confirmaron que los paquetes no habían cambiado desde esos eventos.</li>
 *   <li>No se tocan las canceladas o eliminadas (no se vendieron), ni las de un paquete dado de baja,
 *       que ya no conserva sus servicios: siguen usando el paquete del catálogo, como antes.</li>
 * </ul>
 * Todo va en una transacción: si algo falla no se guarda nada y la aplicación sigue como antes.
 */
@Service
@RequiredArgsConstructor
public class CompletarCopiaPaqueteUseCase {

    private final CotizacionRepository cotizacionRepository;

    /** @return cuántas cotizaciones recibieron su copia */
    @Transactional
    public int completar() {
        int completadas = 0;
        for (Cotizacion cotizacion : cotizacionRepository.findByPaqueteIsNotNullAndPaqueteNombreIsNull()) {
            if (seCompleta(cotizacion)) {
                PaqueteCotizado.guardarCopia(cotizacion);
                completadas++;
            }
        }
        return completadas;
    }

    private static boolean seCompleta(Cotizacion cotizacion) {
        if (cotizacion.tieneCopiaDelPaquete() || cotizacion.getPaquete() == null || !cotizacion.getPaquete().isActivo()) {
            return false;
        }
        if (cotizacion.getEstado() != EstadoCotizacion.BORRADOR && cotizacion.getEstado() != EstadoCotizacion.VIGENTE) {
            return false;
        }
        EstadoEvento evento = cotizacion.getEvento() != null ? cotizacion.getEvento().getEstado() : null;
        return evento != EstadoEvento.CANCELADO && evento != EstadoEvento.CANCELADO_TARDIO;
    }
}
