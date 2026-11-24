package com.example.negocio;

import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.DetalleCotizacion;
import com.mycompany.persistencia.dominio.Paquete;
import com.mycompany.persistencia.dominio.PaquetePrecio;
import com.mycompany.persistencia.dominio.PaqueteServicio;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.enums.CategoriaServicio;
import java.util.ArrayList;

/** Entidades armadas a mano para las pruebas (sin base de datos). */
public final class Fixtures {

    private Fixtures() {
    }

    public static Servicio servicio(long id, String nombre, double precio, CategoriaServicio categoria) {
        Servicio s = new Servicio();
        s.setId(id);
        s.setNombre(nombre);
        s.setPrecio(precio);
        s.setCategoria(categoria);
        s.setActivo(true);
        return s;
    }

    public static Servicio comida(long id, String nombre) {
        return servicio(id, nombre, 0, CategoriaServicio.COMIDA);
    }

    public static Paquete paqueteFijo(long id, String nombre, double costo) {
        Paquete p = new Paquete();
        p.setId(id);
        p.setNombre(nombre);
        p.setCosto(costo);
        p.setActivo(true);
        p.setServicios(new ArrayList<>());
        return p;
    }

    public static Paquete paqueteConTabla(long id, String nombre) {
        return paqueteFijo(id, nombre, 0);
    }

    public static PaquetePrecio precio(Paquete paquete, Servicio comida, int personas, double precio) {
        PaquetePrecio pp = new PaquetePrecio();
        pp.setPaquete(paquete);
        pp.setComida(comida);
        pp.setPersonas(personas);
        pp.setPrecio(precio);
        paquete.getPrecios().add(pp);
        return pp;
    }

    public static PaqueteServicio incluido(Paquete paquete, Servicio servicio, int cantidad) {
        PaqueteServicio ps = new PaqueteServicio();
        ps.setPaquete(paquete);
        ps.setServicio(servicio);
        ps.setCantidad(cantidad);
        ps.setSubtotal(servicio.getPrecio() * cantidad);
        paquete.getServicios().add(ps);
        return ps;
    }

    public static Cotizacion cotizacion(long id, Paquete paquete, Servicio comida, Integer personas, Double precioPaquete) {
        Cotizacion c = new Cotizacion();
        c.setId(id);
        c.setFolio("COT-" + id);
        c.setPaquete(paquete);
        c.setComida(comida);
        c.setPersonas(personas);
        c.setPrecioPaquete(precioPaquete);
        c.setServiciosExtra(new ArrayList<>());
        return c;
    }

    public static DetalleCotizacion extra(Cotizacion cotizacion, Servicio servicio, int cantidad, double precioUnitario) {
        DetalleCotizacion d = new DetalleCotizacion();
        d.setCotizacion(cotizacion);
        d.setServicio(servicio);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(precioUnitario);
        d.setSubtotal(precioUnitario * cantidad);
        cotizacion.getServiciosExtra().add(d);
        return d;
    }
}
