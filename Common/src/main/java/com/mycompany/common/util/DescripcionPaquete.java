package com.mycompany.common.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Texto para mostrar el paquete contratado con su comida y personas,
 * p. ej. "Paquete Full · Barbacoa · 100 personas" o solo "Renta de local".
 */
public final class DescripcionPaquete {

    private DescripcionPaquete() {
    }

    public static String de(String paquete, String comida, Integer personas) {
        List<String> partes = new ArrayList<>(3);
        if (paquete != null && !paquete.isBlank()) partes.add(paquete);
        if (comida != null && !comida.isBlank()) partes.add(comida);
        if (personas != null && personas > 0) partes.add(personas + " personas");
        return String.join(" · ", partes);
    }
}
