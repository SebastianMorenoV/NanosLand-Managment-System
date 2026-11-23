package com.mycompany.persistencia.enums;

/**
 * Estado de una venta extra. Las ventas nunca se borran: una venta capturada
 * por error se cancela, queda como rastro y deja de contar en los ingresos.
 */
public enum EstadoVenta {
    REGISTRADA,
    CANCELADA
}
