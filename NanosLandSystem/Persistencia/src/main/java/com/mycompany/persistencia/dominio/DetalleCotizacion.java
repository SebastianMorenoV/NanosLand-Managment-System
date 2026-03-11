/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.persistencia.dominio;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Entity
@Table(name = "detalles_cotizacion")
@Data
@NoArgsConstructor
public class DetalleCotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cotizacion_id")
    private Cotizacion cotizacion;

    private double precioUnitario;
    private int cantidad;
    private double subtotal;

    @Column(name = "hora_sugerida")
    private LocalTime horaSugerida;

    @Column(name = "especificaciones_cliente", length = 500)
    private String especificacionesCliente;

    @Column(name = "desglose_opciones", length = 500)
    private String desgloseOpciones;

    @Column(name = "ubicacion_montaje")
    private String ubicacionMontaje;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public void setServicio(Servicio servicio) {
        this.servicio = servicio;
    }

    public Cotizacion getCotizacion() {
        return cotizacion;
    }

    public void setCotizacion(Cotizacion cotizacion) {
        this.cotizacion = cotizacion;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public LocalTime getHoraSugerida() {
        return horaSugerida;
    }

    public void setHoraSugerida(LocalTime horaSugerida) {
        this.horaSugerida = horaSugerida;
    }

    public String getEspecificacionesCliente() {
        return especificacionesCliente;
    }

    public void setEspecificacionesCliente(String especificacionesCliente) {
        this.especificacionesCliente = especificacionesCliente;
    }

    public String getDesgloseOpciones() {
        return desgloseOpciones;
    }

    public void setDesgloseOpciones(String desgloseOpciones) {
        this.desgloseOpciones = desgloseOpciones;
    }

    public String getUbicacionMontaje() {
        return ubicacionMontaje;
    }

    public void setUbicacionMontaje(String ubicacionMontaje) {
        this.ubicacionMontaje = ubicacionMontaje;
    }
    
    
}