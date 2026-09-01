package com.mycompany.presentacion;

import java.time.ZoneId;
import java.util.TimeZone;

public class Launcher {
    public static void main(String[] args) {
        // Establecer la zona horaria del JVM a UTC para evitar errores de cambio de horario (DST)
        // en conversiones de bases de datos como "HOUR_OF_DAY: 0 -> 1".
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.systemDefault()));

        // Esta clase es un "caballo de Troya" para engañar a Java 11+
        // Simplemente llama al main de tu aplicación real.
        App.main(args);
    }
}
