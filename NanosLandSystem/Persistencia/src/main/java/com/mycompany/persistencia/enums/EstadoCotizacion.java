/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package com.mycompany.persistencia.enums;

/**
 *
 * @author Sebastian Moreno
 */
public enum EstadoCotizacion {
   /**
    * Estado inicial. No bloquea fechas ni turnos. Puede editarse libremente.
    */
   BORRADOR,
   
   /**
    * Cotización confirmada (se pagó el anticipo mínimo). Bloquea la fecha/turno en el calendario 
    * y genera un Evento formal. Sus datos pasan a ser de solo lectura.
    */
   VIGENTE,
   
   /**
    * Cotización que estaba vigente pero se canceló. Deja el registro histórico y los abonos realizados,
    * pero libera la fecha en el calendario.
    */
   CANCELADA,
   
   /**
    * Borrado lógico de la base de datos. Se oculta del sistema pero el registro se mantiene por integridad.
    */
   ELIMINADA
}
