package com.example.negocio.cotizacion.usecase;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Nombre sugerido al guardar un PDF (contrato, comprobante): identifica el documento con
 * sus folios y el cliente, p. ej. "Contrato COT-2026-0012 - Juan Pérez.pdf", para que no
 * haya que escribirlo a mano ni se confunda con otro.
 */
public final class NombreArchivoPdf {

    /** Largo máximo del nombre sin la extensión (las rutas de Windows tienen límite). */
    static final int LARGO_MAXIMO = 120;

    private NombreArchivoPdf() {
    }

    /** Une las partes no vacías con " - " y quita los caracteres que Windows no permite. */
    public static String de(String... partes) {
        String nombre = Arrays.stream(partes)
                .filter(p -> p != null && !p.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" - "))
                .replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (nombre.length() > LARGO_MAXIMO) {
            nombre = nombre.substring(0, LARGO_MAXIMO);
        }
        // Windows tampoco acepta nombres que terminan en punto o espacio.
        nombre = nombre.replaceAll("[. ]+$", "");
        return (nombre.isEmpty() ? "Documento" : nombre) + ".pdf";
    }
}
