package com.example.negocio.cli;

import com.example.negocio.catalogo.usecase.ConsultarServiciosUseCase;
import com.example.negocio.paquete.usecase.ActualizarPaqueteUseCase;
import com.example.negocio.paquete.usecase.ConsultarPaquetesUseCase;
import com.example.negocio.paquete.usecase.EliminarPaqueteUseCase;
import com.example.negocio.paquete.usecase.RegistrarPaqueteUseCase;
import com.mycompany.common.dtos.PaqueteDTO;
import com.mycompany.common.dtos.PaqueteServicioDTO;
import com.mycompany.common.dtos.ServicioDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.negocio", "com.mycompany.persistencia", "com.mycompany.common"})
@EntityScan(basePackages = "com.mycompany.persistencia.dominio")
@EnableJpaRepositories(basePackages = "com.mycompany.persistencia.repository")
public class PaqueteMenuMain {

    @Autowired private ConsultarPaquetesUseCase consultarPaquetesUseCase;
    @Autowired private RegistrarPaqueteUseCase registrarPaqueteUseCase;
    @Autowired private ActualizarPaqueteUseCase actualizarPaqueteUseCase;
    @Autowired private EliminarPaqueteUseCase eliminarPaqueteUseCase;
    @Autowired private ConsultarServiciosUseCase consultarServiciosUseCase;

    private final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        org.springframework.context.ApplicationContext ctx = SpringApplication.run(PaqueteMenuMain.class, args);
        ctx.getBean(PaqueteMenuMain.class).iniciarMenu();
    }

    public void iniciarMenu() {
        int opcion;
        do {
            mostrarMenu();
            opcion = leerInt("  Elige una opción: ");
            System.out.println();
            switch (opcion) {
                case 1 -> listarPaquetesActivos();
                case 2 -> registrarPaquete();
                case 3 -> editarPaquete();
                case 4 -> eliminarPaqueteLogico();
                case 0 -> System.out.println("  Saliendo del administrador de paquetes.\n");
                default -> System.out.println("  ⚠  Opción no válida.\n");
            }
        } while (opcion != 0);
        sc.close();
    }

    private void mostrarMenu() {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║    GESTIÓN DE PAQUETES (CU-06)           ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║  1  →  Ver Paquetes Activos              ║");
        System.out.println("║  2  →  Registrar Nuevo Paquete           ║");
        System.out.println("║  3  →  Editar Paquete                    ║");
        System.out.println("║  4  →  Eliminar Paquete (Lógico)         ║");
        System.out.println("║  0  →  Salir                             ║");
        System.out.println("╚══════════════════════════════════════════╝");
    }

    private void listarPaquetesActivos() {
        List<PaqueteDTO> lista = consultarPaquetesUseCase.obtenerPaquetesActivos();
        System.out.println("─── PAQUETES ACTIVOS (" + lista.size() + ") ────────────────────────────────");
        if (lista.isEmpty()) {
            System.out.println("  (no hay paquetes activos)");
        } else {
            System.out.printf("  %-5s %-25s %-10s %-30s%n", "ID", "Nombre", "Costo Base", "Descripción");
            System.out.println("  " + "─".repeat(75));
            for (PaqueteDTO p : lista) {
                System.out.printf("  %-5d %-25s $%-9.2f %-30s%n",
                        p.getId(), truncar(p.getNombre(), 24), p.getCostoBase(), truncar(p.getDescripcion(), 29));
                if (p.getServicios() != null && !p.getServicios().isEmpty()) {
                    System.out.println("      Incluye: ");
                    p.getServicios().forEach(ps -> 
                        System.out.println("       - " + ps.getServicio().getNombre() + " (Cantidad: " + ps.getCantidad() + ")")
                    );
                }
            }
        }
        System.out.println();
    }

    private void registrarPaquete() {
        System.out.println("─── REGISTRAR NUEVO PAQUETE ─────────────────────────────────");

        PaqueteDTO dto = new PaqueteDTO();
        dto.setNombre(leerTexto("  Nombre del Paquete : "));
        dto.setCostoBase(leerDouble("  Costo Base (MXN)   : "));
        if (dto.getCostoBase() < 0) return; 
        dto.setDescripcion(leerTexto("  Descripción        : "));
        dto.setServicios(new ArrayList<>());

        agregarServiciosAlPaquete(dto);

        try {
            PaqueteDTO guardado = registrarPaqueteUseCase.registrarPaquete(dto);
            System.out.println("\n  ✓ Paquete agregado exitosamente! (ID=" + guardado.getId() + ")");
        } catch (Exception ex) {
            System.out.println("  ✗ Error al guardar: " + ex.getMessage());
        }
        System.out.println();
    }

    private void editarPaquete() {
        System.out.println("─── EDITAR PAQUETE ──────────────────────────────────────────");
        listarPaquetesActivos();

        Long id = leerLong("  ID del paquete a editar (0 para cancelar): ");
        if (id == 0) return;

        Optional<PaqueteDTO> opt = consultarPaquetesUseCase.obtenerPaquetePorId(id);
        if (opt.isEmpty()) {
            System.out.println("  ✗ No se encontró paquete activo con ID=" + id + "\n");
            return;
        }

        PaqueteDTO p = opt.get();
        System.out.println("  (Deja vacío o ingresa -1 para conservar el valor actual)");

        String nuevoNombre = leerTextoConDefault("  Nombre     [" + p.getNombre() + "]: ");
        if (!nuevoNombre.isBlank()) p.setNombre(nuevoNombre);

        double nuevoCosto = leerDoubleConDefault("  Costo Base [$" + p.getCostoBase() + "]: ");
        if (nuevoCosto >= 0) p.setCostoBase(nuevoCosto);

        String nuevaDesc = leerTextoConDefault("  Descripción[" + p.getDescripcion() + "]: ");
        if (!nuevaDesc.isBlank()) p.setDescripcion(nuevaDesc);

        String modificarServicios = leerTexto("  ¿Desea modificar los servicios incluidos? (s/n): ");
        if (modificarServicios.equalsIgnoreCase("s")) {
            p.getServicios().clear();
            System.out.println("  (Se han limpiado los servicios anteriores. Agregue los nuevos.)");
            agregarServiciosAlPaquete(p);
        }

        try {
            actualizarPaqueteUseCase.actualizarPaquete(p);
            System.out.println("\n  ✓ Paquete actualizado exitosamente!");
        } catch (Exception ex) {
            System.out.println("  ✗ Error al actualizar: " + ex.getMessage());
        }
        System.out.println();
    }

    private void eliminarPaqueteLogico() {
        System.out.println("─── ELIMINAR PAQUETE (LÓGICO) ───────────────────────────────");
        listarPaquetesActivos();

        Long id = leerLong("  ID del paquete a eliminar (0 para cancelar): ");
        if (id == 0) return;

        Optional<PaqueteDTO> opt = consultarPaquetesUseCase.obtenerPaquetePorId(id);
        if (opt.isEmpty()) {
            System.out.println("  ✗ No se encontró paquete activo con ID=" + id + "\n");
            return;
        }

        System.out.println("  ⚠  Advertencia: El paquete ya no aparecerá en nuevas cotizaciones.");
        String conf = leerTexto("  ¿Confirmar eliminación de \"" + opt.get().getNombre() + "\"? (s/n): ");
        
        if (conf.equalsIgnoreCase("s")) {
            try {
                eliminarPaqueteUseCase.eliminarPaqueteLogico(id);
                System.out.println("  ✓ Paquete eliminado lógicamente.");
            } catch (Exception ex) {
                System.out.println("  ✗ Error al eliminar: " + ex.getMessage());
            }
        } else {
            System.out.println("  Eliminación cancelada.");
        }
        System.out.println();
    }

    private void agregarServiciosAlPaquete(PaqueteDTO p) {
        List<ServicioDTO> catalogoServicios = consultarServiciosUseCase.obtenerTodos();
        if (catalogoServicios.isEmpty()) {
            System.out.println("  (No hay servicios en el catálogo para agregar al paquete)");
            return;
        }

        System.out.println("  -- Seleccionar Servicios --");
        for (ServicioDTO s : catalogoServicios) {
            System.out.printf("    [%d] %s (Referencia: $%.2f)%n", s.getId(), s.getNombre(), s.getPrecio());
        }

        while (true) {
            Long sId = leerLong("  ID del servicio a agregar (0 para terminar): ");
            if (sId == 0) break;

            Optional<ServicioDTO> servOpt = consultarServiciosUseCase.obtenerPorId(sId);
            if (servOpt.isPresent()) {
                int cant = leerInt("  Cantidad para este servicio: ");
                if (cant > 0) {
                    PaqueteServicioDTO psDTO = new PaqueteServicioDTO();
                    psDTO.setServicio(servOpt.get());
                    psDTO.setCantidad(cant);
                    p.getServicios().add(psDTO);
                    System.out.println("  + Agregado.");
                }
            } else {
                System.out.println("  ✗ ID de servicio no válido.");
            }
        }
    }

    private String leerTexto(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private String leerTextoConDefault(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private double leerDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String val = sc.nextLine().trim();
            if (val.isBlank()) {
                System.out.println("  ✗ El costo base es obligatorio.");
                return -1;
            }
            try { return Double.parseDouble(val); } 
            catch (NumberFormatException e) { System.out.println("  ✗ El costo debe ser un número válido."); }
        }
    }

    private double leerDoubleConDefault(String prompt) {
        while (true) {
            System.out.print(prompt);
            String val = sc.nextLine().trim();
            if (val.isBlank()) return -1;
            try { return Double.parseDouble(val); } 
            catch (NumberFormatException e) { System.out.println("  ✗ El costo debe ser un número válido."); }
        }
    }

    private int leerInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try { return Integer.parseInt(sc.nextLine().trim()); } 
            catch (NumberFormatException e) { System.out.println("  ⚠ Ingresa un número válido."); }
        }
    }

    private long leerLong(String prompt) {
        while (true) {
            System.out.print(prompt);
            try { return Long.parseLong(sc.nextLine().trim()); } 
            catch (NumberFormatException e) { System.out.println("  ⚠ Ingresa un número válido."); }
        }
    }

    private String truncar(String s, int max) {
        if (s == null) return "-";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
