package com.mycompany.persistencia.enums;

/**
 * Para qué se usa un servicio del catálogo.
 */
public enum CategoriaServicio {

    /** Se vende por separado: aparece en la lista de extras de la cotización y puede ir dentro de un paquete. */
    EXTRA("Extra"),

    /** Solo va dentro de los paquetes (p. ej. la payasita del paquete); no se ofrece como extra. */
    INCLUIDO("Incluido en paquete"),

    /**
     * Opción de comida de los paquetes con tabla de precios. Su precio lo define la tabla
     * del paquete según el número de personas, así que el precio propio del servicio es 0.
     */
    COMIDA("Comida");

    private final String etiqueta;

    CategoriaServicio(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Nombre para mostrar en pantalla. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
