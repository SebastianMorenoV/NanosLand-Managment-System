package com.mycompany.common.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class EstadoCuentaDTO {
    // Info del Evento
    private Long eventoId;
    private String clienteNombre;
    private String folioCotizacion;
    private String fechaEventoFormateada;
    private String estadoEvento;

    // Desglose Financiero
    private double precioPaqueteBase;
    private String nombrePaqueteBase;
    
    private List<ServicioExtraDTO> serviciosExtrasOriginales;
    private double totalServiciosExtrasOriginales;
    
    private List<CargoExtraDTO> cargosExtras;
    private double totalCargosExtras;
    
    private List<PagoDTO> pagosRealizados;
    
    // Totales Consolidados
    private double granTotal; // Paquete + Servicios Extras + Cargos Extras
    private double totalPagado; // Suma de todos los pagos
    private double saldoPendiente; // granTotal - totalPagado
}
