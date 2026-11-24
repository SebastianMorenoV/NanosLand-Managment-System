package com.mycompany.persistencia.enums;

/**
 * Cómo se calcula un descuento sobre el precio de un paquete.
 */
public enum TipoDescuento {

    /** Un porcentaje del precio del paquete (p. ej. 15 = 15 % menos). */
    PORCENTAJE("Porcentaje"),

    /** Una cantidad fija en pesos; nunca deja el paquete por debajo de $0. */
    MONTO_FIJO("Monto fijo");

    private final String etiqueta;

    TipoDescuento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Nombre para mostrar en pantalla. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
