package com.mycompany.persistencia;
//
//import com.mycompany.persistencia.DAO.CargoExtraDAO;
//import com.mycompany.persistencia.DAO.ClienteDAO;
//import com.mycompany.persistencia.DAO.ConexionJPA;
//import com.mycompany.persistencia.DAO.CotizacionDAO;
//import com.mycompany.persistencia.DAO.DetalleCotizacionDAO;
//import com.mycompany.persistencia.DAO.EventoDAO;
//import com.mycompany.persistencia.DAO.LogisticaServicioDAO;
//import com.mycompany.persistencia.DAO.PagoDAO;
//import com.mycompany.persistencia.DAO.PaqueteDAO;
//import com.mycompany.persistencia.DAO.PaqueteServicioDAO;
//import com.mycompany.persistencia.DAO.ServicioDAO;
//import com.mycompany.persistencia.DAO.UsuarioDAO;
//import com.mycompany.persistencia.dominio.*;
//import com.mycompany.persistencia.dominio.enums.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class Persistencia {

    public static void main(String[] args) {
//        System.out.println("Iniciando Sistema NanosLand con DAOs...");
//
//        UsuarioDAO usuarioDAO = new UsuarioDAO();
//        ClienteDAO clienteDAO = new ClienteDAO();
//        ServicioDAO servicioDAO = new ServicioDAO();
//        PaqueteDAO paqueteDAO = new PaqueteDAO();
//        PaqueteServicioDAO paqueteServicioDAO = new PaqueteServicioDAO();
//        CotizacionDAO cotizacionDAO = new CotizacionDAO();
//        DetalleCotizacionDAO detalleCotizacionDAO = new DetalleCotizacionDAO(); 
//        EventoDAO eventoDAO = new EventoDAO();
//        PagoDAO pagoDAO = new PagoDAO();
//        LogisticaServicioDAO logisticaDAO = new LogisticaServicioDAO();
//        CargoExtraDAO cargoExtraDAO = new CargoExtraDAO();
//
//        Scanner scanner = new Scanner(System.in);
//        int opcion;
//
//        do {
//            System.out.println("\n=== MENU DE BASE DE DATOS (DAOs) ===");
//            System.out.println("1. Insertar datos de prueba (Poblar BD)");
//            System.out.println("2. Ver Usuarios");
//            System.out.println("3. Ver Clientes");
//            System.out.println("4. Ver Servicios");
//            System.out.println("5. Ver Paquetes");
//            System.out.println("6. Ver Paquetes-Servicios (Intermedia)");
//            System.out.println("7. Ver Cotizaciones");
//            System.out.println("8. Ver Detalles de Cotizaciones");
//            System.out.println("9. Ver Eventos");
//            System.out.println("10. Ver Pagos");
//            System.out.println("11. Ver Logistica de Servicios");
//            System.out.println("12. Ver Cargos Extras");
//            System.out.println("0. Salir");
//            System.out.print("Elige una opcion: ");
//
//            while (!scanner.hasNextInt()) {
//                System.out.print("Ingresa un numero válido: ");
//                scanner.next();
//            }
//            opcion = scanner.nextInt();
//
//            switch (opcion) {
//                case 1 -> insertarDatosDePrueba(usuarioDAO, clienteDAO, servicioDAO, paqueteDAO, paqueteServicioDAO, cotizacionDAO, detalleCotizacionDAO, eventoDAO, pagoDAO, logisticaDAO, cargoExtraDAO);
//                case 2 -> imprimirLista("Usuarios", usuarioDAO.buscarTodos());
//                case 3 -> imprimirLista("Clientes", clienteDAO.buscarTodos());
//                case 4 -> imprimirLista("Servicios", servicioDAO.buscarTodos());
//                case 5 -> imprimirLista("Paquetes", paqueteDAO.buscarTodos());
//                case 6 -> imprimirLista("Paquetes-Servicios", paqueteServicioDAO.buscarTodos());
//                case 7 -> imprimirLista("Cotizaciones", cotizacionDAO.buscarTodos());
//                case 8 -> imprimirLista("Detalles de Cotizaciones", detalleCotizacionDAO.buscarTodos());
//                case 9 -> imprimirLista("Eventos", eventoDAO.buscarTodos());
//                case 10 -> imprimirLista("Pagos", pagoDAO.buscarTodos());
//                case 11 -> imprimirLista("Logistica", logisticaDAO.buscarTodos());
//                case 12 -> imprimirLista("Cargos Extras", cargoExtraDAO.buscarTodos());
//                case 0 -> System.out.println("Cerrando conexion y saliendo...");
//                default -> System.out.println("Opcion no válida.");
//            }
//        } while (opcion != 0);
//
//        scanner.close();
//        ConexionJPA.cerrar();
    }

    // =================================================================================
    // MÉTODOS AUXILIARES
    // =================================================================================

//    private static <T> void imprimirLista(String nombreTabla, List<T> lista) {
//        System.out.println("\n--- TABLA: " + nombreTabla.toUpperCase() + " ---");
//        if (lista.isEmpty()) {
//            System.out.println(" No hay registros en la base de datos.");
//        } else {
//            for (T entidad : lista) {
//                System.out.println(" -> " + entidad.toString());
//            }
//        }
//    }
//
//    private static void insertarDatosDePrueba(
//            UsuarioDAO uDAO, ClienteDAO cDAO, ServicioDAO sDAO, PaqueteDAO pDAO, 
//            PaqueteServicioDAO psDAO, CotizacionDAO cotDAO, DetalleCotizacionDAO detCotDAO, EventoDAO eDAO, 
//            PagoDAO pagoDAO, LogisticaServicioDAO logDAO, CargoExtraDAO extraDAO) {
//
//        if (!uDAO.buscarTodos().isEmpty()) {
//            System.out.println("\n[AVISO] Los datos de prueba ya fueron insertados anteriormente.");
//            System.out.println("Si deseas volver a insertarlos, primero borra los datos de tu base de datos MySQL.");
//            return;
//        }
//
//        System.out.println("\nInsertando datos en cascada...");
//
//        try {
//            // 1. Usuario
//            Usuario admin = new Usuario();
//            admin.setCorreo("admin@nanosland.com");
//            admin.setContrasena("1234");
//            admin.setTelefono("6440001122");
//            admin.setRol(RolUsuario.ADMINISTRADOR);
//            uDAO.guardar(admin);
//
//            // 2. Cliente
//            Cliente cliente = new Cliente();
//            cliente.setNombre("Carlos Slim");
//            cliente.setTelefono("5551234567");
//            cliente.setCorreo("carlos@slim.com");
//            cDAO.guardar(cliente);
//
//            // 3. Servicios
//            Servicio salon = new Servicio();
//            salon.setNombre("Renta de Salón");
//            salon.setDescripcion("Salón por 4 horas");
//            salon.setPrecio(2500.0);
//            sDAO.guardar(salon);
//
//            Servicio toro = new Servicio();
//            toro.setNombre("Toro Mecánico");
//            toro.setDescripcion("Incluye operador");
//            toro.setPrecio(1200.0);
//            sDAO.guardar(toro);
//
//            // 4. Paquete
//            Paquete paquete = new Paquete();
//            paquete.setNombre("Paquete Extremo");
//            paquete.setDescripcion("Salón + Toro Mecánico");
//            paquete.setCostoBase(3500.0);
//            pDAO.guardar(paquete);
//
//            // 5. Paquete-Servicio
//            PaqueteServicio ps = new PaqueteServicio();
//            ps.setPaquete(paquete);
//            ps.setServicio(toro);
//            ps.setCantidad(1);
//            ps.setSubtotal(1200.0);
//            psDAO.guardar(ps);
//
//            // 6. Cotización
//            Cotizacion cot = new Cotizacion();
//            cot.setFolio("COT-001");
//            cot.setCliente(cliente);
//            cot.setUsuario(admin);
//            cot.setPaquete(paquete);
//            cot.setFecha(new Date());
//            cot.setNombreFestejado("Carlitos Jr.");
//            cot.setTematica("Vaqueros");
//            cot.setTotal(4700.0); // 3500 del paquete + 1200 del servicio extra
//            cot.setEstado(EstadoCotizacion.VIGENTE);
//            cotDAO.guardar(cot);
//
//            // 7. Detalle Cotización (ESTE FALTABA) - Simulamos que rentaron un servicio extra fuera del paquete
//            DetalleCotizacion detalle = new DetalleCotizacion();
//            detalle.setCotizacion(cot);
//            detalle.setServicio(toro);
//            detalle.setCantidad(1);
//            detalle.setPrecioUnitario(1200.0);
//            detalle.setSubtotal(1200.0);
//            detalle.setHoraSugerida(LocalTime.of(16, 0));
//            detalle.setEspecificacionesCliente("El toro debe ir a velocidad lenta para los niños.");
//            detalle.setDesgloseOpciones("Toro infantil");
//            detalle.setUbicacionMontaje("En el centro del patio");
//            detCotDAO.guardar(detalle);
//
//            // 8. Evento
//            Evento evento = new Evento();
//            evento.setCotizacion(cot);
//            evento.setTurno(TurnoEvento.VESPERTINO);
//            evento.setFechaHoraInicio(LocalDateTime.now().plusDays(7));
//            evento.setNotas("Llevar sombreros para los niños.");
//            eDAO.guardar(evento);
//
//            // 9. Pago
//            Pago pago = new Pago();
//            pago.setCotizacion(cot);
//            pago.setCantidad(1000.0); // Anticipo
//            pago.setTipo(MetodoPago.TRANSFERENCIA);
//            pago.setFolioPago("TX-999888");
//            pago.setFechaHora(LocalDateTime.now());
//            pagoDAO.guardar(pago);
//
//            // 10. Logística
//            LogisticaServicio log = new LogisticaServicio();
//            log.setEvento(evento);
//            log.setServicio(toro);
//            log.setHoraRequerida(LocalTime.of(15, 30));
//            log.setUbicacionMontaje("Centro del jardín");
//            log.setEstado(EstadoLogistica.CONFIRMADO);
//            logDAO.guardar(log);
//
//            // 11. Cargo Extra
//            CargoExtra extra = new CargoExtra();
//            extra.setEvento(evento);
//            extra.setDescripcion("Plato roto por el toro");
//            extra.setCantidad(1);
//            extra.setPrecioUnitario(50.0);
//            extra.setSubtotal(50.0);
//            extra.setFechaHoraCargo(LocalDateTime.now().plusDays(7));
//            extraDAO.guardar(extra);
//
//            System.out.println("[ÉXITO] ¡Las 11 entidades fueron insertadas correctamente!");
//
//        } catch (Exception e) {
//            System.err.println("Ocurrió un error al insertar: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
}