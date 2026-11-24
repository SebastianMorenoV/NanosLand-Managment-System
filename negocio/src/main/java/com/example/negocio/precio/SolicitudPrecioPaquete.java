package com.example.negocio.precio;

import java.time.LocalDate;

/**
 * Lo que se necesita para cotizar un paquete.
 *
 * @param paqueteId   paquete elegido
 * @param comidaId    comida elegida; solo en paquetes con tabla de precios
 * @param personas    rango de personas elegido; solo en paquetes con tabla de precios
 * @param fechaEvento fecha del evento; decide qué descuento aplica (sin fecha no hay descuento)
 */
public record SolicitudPrecioPaquete(Long paqueteId, Long comidaId, Integer personas, LocalDate fechaEvento) {
}
