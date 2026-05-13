package com.example.negocio.cli;

import com.example.negocio.cliente.usecase.ActualizarClienteUseCase;
import com.example.negocio.cliente.usecase.BuscarClienteUseCase;
import com.example.negocio.cliente.usecase.EliminarClienteUseCase;
import com.example.negocio.cliente.usecase.RegistrarClienteUseCase;
import com.example.negocio.exception.CotizacionException;
import com.mycompany.common.dtos.ClienteDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;
import java.util.Scanner;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.negocio", "com.mycompany.persistencia", "com.mycompany.common"})
@EntityScan(basePackages = "com.mycompany.persistencia.dominio")
@EnableJpaRepositories(basePackages = "com.mycompany.persistencia.repository")
public class ClienteMenuMain {

    @Autowired private BuscarClienteUseCase buscarClienteUseCase;
    @Autowired private RegistrarClienteUseCase registrarClienteUseCase;
    @Autowired private ActualizarClienteUseCase actualizarClienteUseCase;
    @Autowired private EliminarClienteUseCase eliminarClienteUseCase;

    private final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        org.springframework.context.ApplicationContext ctx = SpringApplication.run(ClienteMenuMain.class, args);
        ctx.getBean(ClienteMenuMain.class).iniciarMenu();
    }

    public void iniciarMenu() {
        int opcion;
        do {
            mostrarMenu();
            opcion = leerInt("  Elige una opción: ");
            System.out.println();
            switch (opcion) {
                case 1 -> listarClientes();
                case 2 -> buscarCliente();
                case 3 -> registrarCliente();
                case 4 -> editarCliente();
                case 5 -> eliminarCliente();
                case 0 -> System.out.println("  Hasta luego.\n");
                default -> System.out.println("  ⚠  Opción no válida. Intenta de nuevo.\n");
            }
        } while (opcion != 0);
        sc.close();
    }

    private void mostrarMenu() {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║    GESTIÓN DE CLIENTES — NanosLand       ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║  1  →  Ver todos los clientes            ║");
        System.out.println("║  2  →  Buscar cliente                    ║");
        System.out.println("║  3  →  Registrar nuevo cliente           ║");
        System.out.println("║  4  →  Editar cliente                    ║");
        System.out.println("║  5  →  Eliminar cliente                  ║");
        System.out.println("║  0  →  Salir                             ║");
        System.out.println("╚══════════════════════════════════════════╝");
    }

    private void listarClientes() {
        List<ClienteDTO> lista = buscarClienteUseCase.obtenerTodos();
        System.out.println("─── CLIENTES REGISTRADOS (" + lista.size() + ") ──────────────────────────────");
        if (lista.isEmpty()) {
            System.out.println("  (no hay clientes registrados)");
        } else {
            encabezadoTabla();
            lista.forEach(this::imprimirFila);
        }
        System.out.println();
    }

    private void buscarCliente() {
        System.out.println("─── BUSCAR CLIENTE ──────────────────────────────────────────────");
        String texto = leerTexto("  Texto a buscar (nombre / teléfono / correo): ");
        List<ClienteDTO> resultado = buscarClienteUseCase.buscarPorTexto(texto);
        System.out.println("  " + resultado.size() + " resultado(s) para \"" + texto + "\":");
        if (resultado.isEmpty()) {
            System.out.println("  (sin coincidencias)");
        } else {
            encabezadoTabla();
            resultado.forEach(this::imprimirFila);
        }
        System.out.println();
    }

    private void registrarCliente() {
        System.out.println("─── REGISTRAR NUEVO CLIENTE ─────────────────────────────────────");
        ClienteDTO dto = new ClienteDTO();
        
        dto.setNombre(leerTexto("  Nombre completo    : "));
        dto.setTelefono(leerTexto("  Teléfono (10 díg.) : "));
        dto.setCorreo(leerTexto("  Correo (opcional)  : "));
        
        System.out.println("  ── Dirección (opcional) ──");
        dto.setCalle(leerTexto("  Calle              : "));
        dto.setColonia(leerTexto("  Colonia            : "));
        dto.setCiudad(leerTexto("  Ciudad             : "));
        dto.setCodigoPostal(leerTexto("  Código postal      : "));

        try {
            ClienteDTO guardado = registrarClienteUseCase.registrarCliente(dto);
            System.out.println("\n  ✓ Cliente registrado con ID=" + guardado.getId());
        } catch (CotizacionException ex) {
            System.out.println("  ✗ Error de validación: " + ex.getMessage());
        } catch (Exception ex) {
            System.out.println("  ✗ Error al guardar: " + ex.getMessage());
        }
        System.out.println();
    }

    private void editarCliente() {
        System.out.println("─── EDITAR CLIENTE ──────────────────────────────────────────────");
        listarClientes();

        Long id = leerLong("  ID del cliente a editar (0 para cancelar): ");
        if (id == 0) return;

        ClienteDTO c = buscarClienteUseCase.buscarPorIdDTO(id);
        if (c == null) {
            System.out.println("  ✗ No se encontró ningún cliente con ID=" + id + "\n");
            return;
        }

        System.out.println("  (Deja vacío para conservar el valor actual)");

        String nuevoNombre = leerTextoConDefault("  Nombre    [" + c.getNombre() + "]: ");
        if (!nuevoNombre.isBlank()) c.setNombre(nuevoNombre);

        String nuevoTel = leerTextoConDefault("  Teléfono  [" + nvl(c.getTelefono()) + "]: ");
        if (!nuevoTel.isBlank()) c.setTelefono(nuevoTel);

        String nuevoCorreo = leerTextoConDefault("  Correo    [" + nvl(c.getCorreo()) + "]: ");
        if (!nuevoCorreo.isBlank()) c.setCorreo(nuevoCorreo);

        System.out.println("  ── Dirección ─────────────────────────────────────────");
        String nuevaCalle   = leerTextoConDefault("  Calle     [" + nvl(c.getCalle()) + "]: ");
        String nuevaColonia = leerTextoConDefault("  Colonia   [" + nvl(c.getColonia()) + "]: ");
        String nuevaCiudad  = leerTextoConDefault("  Ciudad    [" + nvl(c.getCiudad()) + "]: ");
        String nuevoCP      = leerTextoConDefault("  C. Postal [" + nvl(c.getCodigoPostal()) + "]: ");

        if (!nuevaCalle.isBlank())   c.setCalle(nuevaCalle);
        if (!nuevaColonia.isBlank()) c.setColonia(nuevaColonia);
        if (!nuevaCiudad.isBlank())  c.setCiudad(nuevaCiudad);
        if (!nuevoCP.isBlank())      c.setCodigoPostal(nuevoCP);

        try {
            actualizarClienteUseCase.actualizarCliente(c);
            System.out.println("\n  ✓ Cliente ID=" + id + " actualizado correctamente.");
        } catch (CotizacionException ex) {
            System.out.println("  ✗ Error de validación: " + ex.getMessage());
        } catch (Exception ex) {
            System.out.println("  ✗ Error al actualizar: " + ex.getMessage());
        }
        System.out.println();
    }

    private void eliminarCliente() {
        System.out.println("─── ELIMINAR CLIENTE ────────────────────────────────────────────");
        listarClientes();

        Long id = leerLong("  ID del cliente a eliminar (0 para cancelar): ");
        if (id == 0) return;

        ClienteDTO c = buscarClienteUseCase.buscarPorIdDTO(id);
        if (c == null) {
            System.out.println("  ✗ No se encontró ningún cliente con ID=" + id + "\n");
            return;
        }

        String conf = leerTexto("  ¿Eliminar a \"" + c.getNombre() + "\"? (s/n): ");
        if (!conf.equalsIgnoreCase("s")) {
            System.out.println("  Eliminación cancelada.\n");
            return;
        }

        try {
            eliminarClienteUseCase.eliminarCliente(id);
            System.out.println("  ✓ Cliente eliminado correctamente.");
        } catch (CotizacionException ex) {
            System.out.println("  ✗ No se puede eliminar: " + ex.getMessage());
        } catch (Exception ex) {
            System.out.println("  ✗ Error al eliminar: " + ex.getMessage());
        }
        System.out.println();
    }

    // Helpers
    private String leerTexto(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private String leerTextoConDefault(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private int leerInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try { return Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("  ⚠  Ingresa un número entero."); }
        }
    }

    private long leerLong(String prompt) {
        while (true) {
            System.out.print(prompt);
            try { return Long.parseLong(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("  ⚠  Ingresa un número válido."); }
        }
    }

    private void encabezadoTabla() {
        System.out.printf("  %-5s %-28s %-12s %-26s %-20s%n", "ID", "Nombre", "Teléfono", "Correo", "Ciudad");
        System.out.println("  " + "─".repeat(95));
    }

    private void imprimirFila(ClienteDTO c) {
        System.out.printf("  %-5d %-28s %-12s %-26s %-20s%n",
                c.getId(), truncar(c.getNombre(), 27), nvl(c.getTelefono()),
                truncar(nvl(c.getCorreo()), 25), truncar(nvl(c.getCiudad()), 19));
    }

    private String nvl(String s) { return s != null ? s : "-"; }
    private String truncar(String s, int max) { return s != null && s.length() > max ? s.substring(0, max - 1) + "…" : (s != null ? s : "-"); }
}
