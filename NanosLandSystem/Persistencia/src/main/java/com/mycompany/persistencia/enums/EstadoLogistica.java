/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package com.mycompany.persistencia.enums;

/**
 *
 * @author skyro
 */
public enum EstadoLogistica {
    POR_CONTACTAR, // Aún no se habla con el proveedor/encargado
    ENCARGADO,     // Ya se solicitó pero no está 100% amarrado
    CONFIRMADO,    // Proveedor confirmó asistencia/entrega
    LISTO          // El servicio ya está montado o en el salón el día del evento
}
