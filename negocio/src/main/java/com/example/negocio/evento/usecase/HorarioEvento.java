package com.example.negocio.evento.usecase;

import com.example.negocio.exception.CotizacionException;

import java.time.LocalTime;

/**
 * Horario real de un evento: se captura al cotizar o al reprogramar, junto con el turno.
 * El turno solo aparta el bloque del día; las horas son las que el cliente pactó y las
 * que salen en el contrato.
 */
public final class HorarioEvento {

    private HorarioEvento() {
    }

    /**
     * Valida que el evento tenga hora de inicio y de fin, y que termine después de empezar.
     * Las 00:00 como hora de fin cuentan como medianoche (fin del día).
     *
     * @throws CotizacionException si falta alguna hora o el horario no es válido.
     */
    public static void validar(LocalTime inicio, LocalTime fin) {
        if (inicio == null || fin == null) {
            throw new CotizacionException("Debe seleccionar la hora de inicio y la hora de fin del evento.");
        }
        if (!fin.equals(LocalTime.MIDNIGHT) && !fin.isAfter(inicio)) {
            throw new CotizacionException("La hora de fin del evento debe ser posterior a la hora de inicio.");
        }
    }
}
