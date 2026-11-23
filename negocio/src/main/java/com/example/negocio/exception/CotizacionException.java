package com.example.negocio.exception;

/**
 * Excepción de negocio para operaciones de cotización.
 * Se lanza cuando se viola una regla de negocio al crear, modificar o eliminar una cotización.
 */
public class CotizacionException extends RuntimeException {

    public CotizacionException(String message) {
        super(message);
    }

    public CotizacionException(String message, Throwable cause) {
        super(message, cause);
    }
}
