package com.example.negocio.logistica.usecase;

import com.mycompany.persistencia.enums.EstadoEvento;
import com.mycompany.persistencia.repository.EventoRepository;
import com.mycompany.persistencia.repository.LogisticaServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Corrección al arrancar: en los eventos confirmados o en curso, separa los registros de logística
 * de antes en lo del paquete y cada extra (ver {@link GenerarLogisticaEventoUseCase}). Antes iban
 * sumados en un registro por servicio y se perdían los datos de los extras agregados después de
 * confirmar (hora, ubicación, especificaciones).
 * <ul>
 *   <li>Solo se separan los servicios contratados en más de una parte (del paquete y extra, o varios
 *       extras): el registro existente conserva su estado, su responsable y lo que se le escribió, y
 *       queda con la cantidad de su parte; la que faltaba (normalmente el extra) se crea aparte, por
 *       contactar y con los datos de la cotización.</li>
 *   <li>A los demás registros solo se les pone su origen: no cambian su cantidad ni su texto, y no se
 *       quita ningún registro.</li>
 *   <li>Los eventos ya realizados no se tocan: su bitácora queda como historial.</li>
 *   <li>Solo trabaja con los registros sin origen: si se vuelve a ejecutar no hace nada.</li>
 * </ul>
 * Todo va en una transacción: si algo falla no se guarda nada y la aplicación sigue como antes.
 */
@Service
@RequiredArgsConstructor
public class SepararLogisticaExtrasUseCase {

    private final LogisticaServicioRepository logisticaRepository;
    private final EventoRepository eventoRepository;
    private final GenerarLogisticaEventoUseCase generarLogisticaEventoUseCase;

    /** @return cuántos eventos se revisaron */
    @Transactional
    public int separar() {
        List<Long> eventos = logisticaRepository.eventosConRegistrosSinOrigen(
            List.of(EstadoEvento.CONFIRMADO, EstadoEvento.EN_CURSO));
        for (Long id : eventos) {
            eventoRepository.findById(id).ifPresent(generarLogisticaEventoUseCase::separarRegistrosAnteriores);
        }
        return eventos.size();
    }
}
