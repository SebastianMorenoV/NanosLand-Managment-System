package com.example.negocio.cli;

import com.example.negocio.reporte.usecase.GenerarReporteEventosUseCase;
import com.mycompany.common.dtos.EventoDTO;
import com.mycompany.persistencia.enums.EstadoCotizacion;
import com.mycompany.persistencia.enums.TurnoEvento;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.negocio", "com.mycompany.persistencia", "com.mycompany.common"})
@EntityScan(basePackages = "com.mycompany.persistencia.dominio")
@EnableJpaRepositories(basePackages = "com.mycompany.persistencia.repository")
public class ReporteEventosMenuMain {

    @Autowired private GenerarReporteEventosUseCase generarReporteEventosUseCase;

    private final Scanner sc = new Scanner(System.in);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) {
        org.springframework.context.ApplicationContext ctx = SpringApplication.run(ReporteEventosMenuMain.class, args);
        ctx.getBean(ReporteEventosMenuMain.class).iniciarMenu();
    }

    public void iniciarMenu() {
        int opcion;
        do {
            mostrarMenu();
            opcion = leerInt("  Elige una opción: ");
            System.out.println();
            if (opcion == 1) {
                generarReporte();
            } else if (opcion == 0) {
                System.out.println("  Saliendo de Reportes.\n");
            } else {
                System.out.println("  ⚠  Opción no válida.\n");
            }
        } while (opcion != 0);

        sc.close();
    }

    private void mostrarMenu() {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║    REPORTE DE EVENTOS (CU-07)            ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║  1  →  Generar Reporte de Eventos        ║");
        System.out.println("║  0  →  Salir                             ║");
        System.out.println("╚══════════════════════════════════════════╝");
    }

    private void generarReporte() {
        System.out.println("─── FILTROS DE BÚSQUEDA ─────────────────────────────────────────");

        LocalDate fechaInicio = leerFecha("  Fecha Inicio (YYYY-MM-DD): ");
        LocalDate fechaFin = leerFecha("  Fecha Fin    (YYYY-MM-DD): ");

        TurnoEvento turnoSel = leerTurno();
        EstadoCotizacion estadoSel = leerEstado();

        System.out.println("\n  Generando reporte...\n");

        try {
            List<EventoDTO> resultados = generarReporteEventosUseCase.generarReporte(fechaInicio, fechaFin, turnoSel, estadoSel);

            if (resultados.isEmpty()) {
                System.out.println("  ⚠ No se encontraron eventos para los criterios seleccionados (Flujo 2.2.1).\n");
                return;
            }

            imprimirTablaResultados(resultados);

            String exportar = leerTexto("\n  ¿Desea Exportar a PDF (simulado en .txt)? (s/n): ");
            if (exportar.equalsIgnoreCase("s")) {
                exportarReporteArchivo(resultados, fechaInicio, fechaFin, turnoSel, estadoSel);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("\n  ✗ Error: " + e.getMessage());
        }
        System.out.println();
    }

    private void imprimirTablaResultados(List<EventoDTO> resultados) {
        System.out.println("─── RESULTADOS DEL PERIODO ──────────────────────────────────────");
        System.out.printf("  %-12s %-10s %-20s %-20s %-12s %-12s%n",
                "Fecha", "Folio", "Cliente", "Paquete", "Estado", "Total");
        System.out.println("  " + "─".repeat(95));

        double totalPeriodo = 0;

        for (EventoDTO e : resultados) {
            String folio = e.getFolioCotizacion() != null ? e.getFolioCotizacion() : "-";
            String cliente = e.getClienteNombre() != null ? e.getClienteNombre() : "Desconocido";
            String paquete = e.getPaqueteNombre() != null ? e.getPaqueteNombre() : "Personalizado";
            String estado = e.getEstadoCotizacion() != null ? e.getEstadoCotizacion().name() : "-";
            double total = e.getTotalCotizacion();

            totalPeriodo += total;

            System.out.printf("  %-12s %-10s %-20s %-20s %-12s $%-11.2f%n",
                    e.getFecha() != null ? e.getFecha().toString() : "-",
                    truncar(folio, 9),
                    truncar(cliente, 19),
                    truncar(paquete, 19),
                    estado,
                    total);
        }
        System.out.println("  " + "─".repeat(95));
        System.out.printf("  %-78s $%-11.2f%n", "TOTAL COBRADO EN EL PERIODO:", totalPeriodo);
    }

    private void exportarReporteArchivo(List<EventoDTO> resultados, LocalDate inicio, LocalDate fin, TurnoEvento turno, EstadoCotizacion estado) {
        String desktopPath = Paths.get(System.getProperty("user.home"), "Desktop", "Reporte_Eventos.txt").toString();
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(desktopPath))) {
            writer.println("=================================================");
            writer.println("              NANOS LAND - REPORTE               ");
            writer.println("=================================================");
            writer.println("Fecha de generación: " + LocalDate.now());
            writer.println("Criterios de filtrado:");
            writer.println("  - Desde  : " + (inicio == null ? "Inicio" : inicio));
            writer.println("  - Hasta  : " + (fin == null ? "Fin" : fin));
            writer.println("  - Turno  : " + (turno == null ? "Todos" : turno.name()));
            writer.println("  - Estado : " + (estado == null ? "Todos" : estado.name()));
            writer.println("-------------------------------------------------");
            
            writer.printf("%-12s %-10s %-25s %-12s %-10s%n", "Fecha", "Folio", "Cliente", "Estado", "Total");
            writer.println("-------------------------------------------------------------------------");
            
            double granTotal = 0;
            for (EventoDTO e : resultados) {
                String folio = e.getFolioCotizacion() != null ? e.getFolioCotizacion() : "-";
                String cliente = e.getClienteNombre() != null ? e.getClienteNombre() : "Desconocido";
                String st = e.getEstadoCotizacion() != null ? e.getEstadoCotizacion().name() : "-";
                double total = e.getTotalCotizacion();
                granTotal += total;

                writer.printf("%-12s %-10s %-25s %-12s $%.2f%n",
                        e.getFecha(), truncar(folio, 9), truncar(cliente, 24), st, total);
            }
            writer.println("-------------------------------------------------------------------------");
            writer.printf("RESUMEN: %d eventos listados. TOTAL ACUMULADO: $%.2f%n", resultados.size(), granTotal);
            writer.println("=================================================");

            System.out.println("  ✓ Reporte exportado exitosamente en: " + desktopPath);
        } catch (IOException e) {
            System.out.println("  ✗ Error al guardar el archivo: " + e.getMessage());
        }
    }

    private LocalDate leerFecha(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (input.isBlank()) {
                return null;
            }
            try { return LocalDate.parse(input, formatter); } 
            catch (DateTimeParseException e) { System.out.println("  ✗ Formato de fecha inválido. Usa YYYY-MM-DD."); }
        }
    }

    private TurnoEvento leerTurno() {
        System.out.println("  Seleccione el Turno:");
        System.out.println("    [0] Todos (Ambos)");
        System.out.println("    [1] Matutino");
        System.out.println("    [2] Vespertino");
        int op = leerInt("  Opción: ");
        if (op == 1) return TurnoEvento.MATUTINO;
        if (op == 2) return TurnoEvento.VESPERTINO;
        return null;
    }

    private EstadoCotizacion leerEstado() {
        System.out.println("  Seleccione el Estado:");
        System.out.println("    [0] Todos");
        System.out.println("    [1] Vigente");
        System.out.println("    [2] Confirmada");
        System.out.println("    [3] Cancelada");
        System.out.println("    [4] Borrador");
        int op = leerInt("  Opción: ");
        if (op == 1 || op == 2) return EstadoCotizacion.VIGENTE;
        if (op == 3) return EstadoCotizacion.CANCELADA;
        if (op == 4) return EstadoCotizacion.BORRADOR;
        return null; 
    }

    private int leerInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try { return Integer.parseInt(sc.nextLine().trim()); } 
            catch (NumberFormatException e) { System.out.println("  ⚠ Ingresa un número válido."); }
        }
    }

    private String leerTexto(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private String truncar(String s, int max) {
        if (s == null) return "-";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
