package com.mycompany.persistencia.enums;

/**
 * Estado lógico de la ejecución de un evento (independiente del estado de la cotización financiera).
 */
public enum EstadoEvento {
    TENTATIVO,    // Creado junto con la cotización en borrador
    CONFIRMADO,   // El anticipo mínimo fue pagado, bloquea la fecha
    EN_CURSO,     // El día del evento ha llegado y está sucediendo
    FINALIZADO,   // El evento terminó satisfactoriamente
    CANCELADO     // El evento fue cancelado (ej. lluvia, problemas de logística)
}
