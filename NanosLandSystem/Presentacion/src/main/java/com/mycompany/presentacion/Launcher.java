package com.mycompany.presentacion;

public class Launcher {
    public static void main(String[] args) {
        // Esta clase es un "caballo de Troya" para engañar a Java 11+
        // Simplemente llama al main de tu aplicación real.
        App.main(args);
    }
}
