package com.mycompany.persistencia.enums;

/** De dónde viene un registro de logística de un evento. */
public enum OrigenLogistica {
    /** Lo que incluye el paquete (y la comida elegida). */
    PAQUETE,
    /** Un servicio extra de la cotización, con sus propios datos (hora, ubicación, especificaciones). */
    EXTRA
}
