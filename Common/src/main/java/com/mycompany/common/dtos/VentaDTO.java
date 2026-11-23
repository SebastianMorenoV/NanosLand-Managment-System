package com.mycompany.common.dtos;

import com.mycompany.persistencia.enums.EstadoVenta;
import com.mycompany.persistencia.enums.MetodoPago;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class VentaDTO {
    private Long id;
    private String folio;
    private LocalDateTime fechaHora;
    private MetodoPago metodoPago;
    private double total;
    private EstadoVenta estado;
    private String usuario;
    private List<DetalleVentaDTO> detalles = new ArrayList<>();

    public boolean isCancelada() {
        return estado == EstadoVenta.CANCELADA;
    }

    /** Por ejemplo: "2 Calcetines, 1 Gorra". */
    public String getResumen() {
        return detalles.stream()
            .map(d -> d.getCantidad() + " " + d.getNombreProducto())
            .collect(Collectors.joining(", "));
    }
}
