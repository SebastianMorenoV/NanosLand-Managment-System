package com.example.negocio.precio;

/**
 * Precio de un paquete para una cotización.
 *
 * @param precio  importe en pesos, redondeado a centavos
 * @param pactado {@code true} si es el precio que ya tenía guardado la cotización
 *                (la selección no cambió); {@code false} si sale del catálogo actual
 */
public record PrecioPaquete(double precio, boolean pactado) {
}
