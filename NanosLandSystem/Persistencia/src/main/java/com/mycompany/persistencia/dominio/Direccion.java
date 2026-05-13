package com.mycompany.persistencia.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dirección física de un cliente.
 *
 * Se usa como @Embeddable: sus columnas viven directamente en la tabla
 * "clientes" (sin JOIN ni tabla separada), lo que es correcto cuando
 * cada cliente tiene una y solo una dirección.
 *
 * Columnas generadas en BD:
 *   calle, colonia, ciudad, codigo_postal
 */
@Embeddable
@Data
@NoArgsConstructor
public class Direccion {

    /** Calle y número exterior/interior. Ej: "Av. Reforma 123 Int. 4" */
    private String calle;

    /** Colonia o fraccionamiento. Ej: "Col. Centro" */
    private String colonia;

    /** Ciudad o municipio. Ej: "Cajeme" */
    private String ciudad;

    /** Código postal de 5 dígitos. */
    @Column(name = "codigo_postal", length = 5)
    private String codigoPostal;
}
