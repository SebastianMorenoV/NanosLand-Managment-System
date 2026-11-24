/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.common.dtos;


import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PaqueteDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    /** Precio de un paquete de precio fijo; 0 en los paquetes con tabla de precios. */
    private double costoBase;
    private boolean activo = true;
    /** Servicios incluidos. Al actualizar, {@code null} significa "sin cambios". */
    private List<PaqueteServicioDTO> servicios;
    /** Tabla de precios por comida y personas; vacía en paquetes de precio fijo. Al actualizar, {@code null} significa "sin cambios". */
    private List<PaquetePrecioDTO> precios;

    /** Indica si el precio depende de la comida y el número de personas. */
    public boolean isTablaPrecios() {
        return precios != null && !precios.isEmpty();
    }

    /** Precio más bajo del paquete: el de la tabla si tiene, o el precio fijo. */
    public double getPrecioDesde() {
        if (!isTablaPrecios()) return costoBase;
        return precios.stream().mapToDouble(PaquetePrecioDTO::getPrecio).min().orElse(0);
    }
}
