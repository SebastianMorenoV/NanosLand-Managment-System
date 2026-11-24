package com.example.negocio.precio;

import com.mycompany.persistencia.dominio.Cotizacion;

/**
 * Precio de un paquete para una cotización.
 *
 * @param precio         importe final en pesos (ya con el descuento), redondeado a centavos
 * @param pactado        {@code true} si es el precio que ya tenía guardado la cotización
 *                       (la selección no cambió); {@code false} si sale del catálogo actual
 * @param precioLista    precio de catálogo del paquete antes del descuento
 * @param descuento      nombre del descuento aplicado, o {@code null}
 * @param montoDescuento pesos descontados: {@code precioLista - precio} (0 sin descuento)
 */
public record PrecioPaquete(double precio, boolean pactado, double precioLista, String descuento, double montoDescuento) {

    public static PrecioPaquete sinDescuento(double precio, boolean pactado) {
        return new PrecioPaquete(precio, pactado, precio, null, 0);
    }

    public boolean tieneDescuento() {
        return montoDescuento > 0;
    }

    /** Guarda en la cotización el precio pactado del paquete con su descuento. */
    public void guardarEn(Cotizacion cotizacion) {
        cotizacion.setPrecioPaquete(precio);
        cotizacion.setPrecioLista(precioLista);
        cotizacion.setDescuentoNombre(tieneDescuento() ? descuento : null);
        cotizacion.setDescuentoMonto(tieneDescuento() ? montoDescuento : null);
    }
}
