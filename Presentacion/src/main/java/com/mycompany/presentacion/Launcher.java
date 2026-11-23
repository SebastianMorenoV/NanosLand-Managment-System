package com.mycompany.presentacion;


public class Launcher {
    public static void main(String[] args) {
        // Esta clase es un "caballo de Troya" para engañar a Java 11+
        // Simplemente llama al main de tu aplicación real.
        // Formato de números y fechas de México, sin depender de la configuración regional
        // de Windows (con coma decimal, los importes "1.234,56" se interpretarían mal).
        java.util.Locale.setDefault(new java.util.Locale("es", "MX"));
        com.mycompany.presentacion.config.RegistroErrores.instalar();
        App.main(args);
    }
}
