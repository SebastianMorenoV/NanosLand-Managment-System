package com.mycompany.common.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class IngresoDTO {
    public static final String ORIGEN_EVENTO = "Evento";
    public static final String ORIGEN_VENTA_EXTRA = "Venta extra";

    private String folioPago;
    private LocalDateTime fechaHora;
    private String tipo;
    private double cantidad;
    /** Nombre del cliente en pagos de eventos; productos vendidos en ventas extra. */
    private String clienteNombre;
    private String folioCotizacion;
    private String origen = ORIGEN_EVENTO;

    public boolean isVentaExtra() {
        return ORIGEN_VENTA_EXTRA.equals(origen);
    }
}
