/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.common.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author skyro
 */
@Data
@NoArgsConstructor
public class ClienteDTO {
    
    private Long id;
    private String nombre;
    private String telefono;
    private String correo;
}
