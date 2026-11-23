package com.mycompany.common.dtos;

import java.time.LocalTime;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author skyro
 */
@Data
@NoArgsConstructor
public class DetalleCotizacionDTO {
    private Long id;
    private Long servicioId;
    private String nombreServicio;
    private double precioUnitario;
    private int cantidad;
    private double subtotal;
    private LocalTime horaSugerida;
    private String especificacionesCliente;
    private String desgloseOpciones;
    private String ubicacionMontaje;
}
