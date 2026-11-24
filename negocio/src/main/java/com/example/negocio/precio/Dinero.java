package com.example.negocio.precio;

/** Redondeo de importes en pesos (el proyecto guarda el dinero como double). */
public final class Dinero {

    private Dinero() {
    }

    /** Redondea a centavos. */
    public static double aCentavos(double importe) {
        return Math.round(importe * 100) / 100.0;
    }
}
