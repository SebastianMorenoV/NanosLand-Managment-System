/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.negocio.cotizacion.usecase;

import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.dtos.ServicioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 
 * @author skyro
 */
@Service
@RequiredArgsConstructor
public class CalcularTotalCotizacionUseCase {

    /**
     * Calcula el monto total de la cotización sumando el paquete base y los extras.
     * Cumple con los flujos 2.2.3 y 2.2.4 al procesar la lista actualizada de servicios.
     * * @param paquete El paquete base seleccionado (contiene el costo inicial).
     * @param serviciosExtras Lista de servicios adicionales agregados por el usuario.
     * @return El total calculado en formato Double.
     */
    public Double ejecutar(PaqueteDTO paquete, List<ServicioDTO> serviciosExtras) {
        double total = 0.0;
        
        // Sumamos el costo base del paquete si existe
        if (paquete != null) {
            total += paquete.getCostoBase();
        }

        // Sumamos cada servicio extra (Flujo 2.2.3 / 2.2.4)
        if (serviciosExtras != null && !serviciosExtras.isEmpty()) {
            for (ServicioDTO servicio : serviciosExtras) {
                // Se asume que el DTO ya trae el precio unitario vigente
                total += servicio.getPrecio();
            }
        }

        return total;
    }
}