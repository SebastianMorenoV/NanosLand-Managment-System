package com.example.negocio.reporte.usecase;

import com.mycompany.common.dtos.EventoDTO;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExportarReporteUseCase {

    /**
     * Fix #5 — Cache del reporte compilado.
     * JasperCompileManager.compileReport() es costoso en CPU (parsea XML + compila a bytecode).
     * Al ser este bean un singleton de Spring, el cache es seguro y se inicializa
     * una sola vez en la primera exportación. Las siguientes solo hacen fillReport().
     */
    private JasperReport jasperReportCache;

    public void exportarPDF(List<EventoDTO> eventos, InputStream reporteStream, String rutaDestino, LocalDate inicio, LocalDate fin) throws Exception {
        if (eventos == null || eventos.isEmpty()) {
            throw new IllegalArgumentException("No hay datos para exportar.");
        }

        if (reporteStream == null) {
            throw new RuntimeException("El flujo de la plantilla es nulo.");
        }

        // Fix #5: compilar solo si no está cacheado aún
        if (jasperReportCache == null) {
            jasperReportCache = JasperCompileManager.compileReport(reporteStream);
        }
        JasperReport jasperReport = jasperReportCache;

        // Calcular KPIs Financieros
        int totalEventos = eventos.size();
        double totalIngresos = eventos.stream().mapToDouble(e -> e.getTotalCotizacion() + e.getTotalCargosExtras()).sum();
        double ticketPromedio = totalEventos > 0 ? totalIngresos / totalEventos : 0.0;
        double ingresosExtras = eventos.stream().mapToDouble(EventoDTO::getTotalCargosExtras).sum();

        // Calcular KPIs Logísticos y de Ventas
        long cerrados = eventos.stream()
            .filter(e -> e.getEstadoEvento() == com.mycompany.persistencia.enums.EstadoEvento.CONFIRMADO || 
                         e.getEstadoEvento() == com.mycompany.persistencia.enums.EstadoEvento.FINALIZADO)
            .count();
        double tasaCierre = totalEventos > 0 ? (cerrados * 100.0 / totalEventos) : 0.0;

        long matutinos = eventos.stream()
            .filter(e -> e.getTurno() == com.mycompany.persistencia.enums.TurnoEvento.MATUTINO)
            .count();
        double ocupacionMatutino = totalEventos > 0 ? (matutinos * 100.0 / totalEventos) : 0.0;
        double ocupacionVespertino = totalEventos > 0 ? 100.0 - ocupacionMatutino : 0.0;
        
        String paqueteEstrella = eventos.stream()
            .filter(e -> e.getPaqueteNombre() != null && !e.getPaqueteNombre().isEmpty())
            .collect(java.util.stream.Collectors.groupingBy(e -> e.getPaqueteNombre(), java.util.stream.Collectors.counting()))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("N/A");

        // Formatear periodo
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String periodo;
        if (inicio == null && fin == null) {
            periodo = "Todos los eventos (máx. 20 más recientes)";
        } else if (inicio != null && fin == null) {
            periodo = "Desde: " + inicio.format(formatter);
        } else if (inicio == null && fin != null) {
            periodo = "Hasta: " + fin.format(formatter);
        } else {
            periodo = "Del: " + inicio.format(formatter) + " Al: " + fin.format(formatter);
        }

        // Crear el DataSource a partir de la lista de DTOs
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(eventos);

        // Parámetros adicionales
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TituloReporte", "Reporte Gerencial de Eventos");
        parametros.put("Periodo", periodo);
        parametros.put("TotalEventos", String.valueOf(totalEventos));
        parametros.put("TotalIngresos", String.format("$%,.2f", totalIngresos));
        parametros.put("TicketPromedio", String.format("$%,.2f", ticketPromedio));
        parametros.put("TasaCierre", String.format("%.1f%%", tasaCierre));
        parametros.put("PaqueteEstrella", paqueteEstrella);
        parametros.put("OcupacionTurnos", String.format("%.0f%% Mat / %.0f%% Vesp", ocupacionMatutino, ocupacionVespertino));
        parametros.put("IngresosExtras", String.format("$%,.2f", ingresosExtras));

        // Llenar el reporte
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, dataSource);

        // Exportar a PDF
        JasperExportManager.exportReportToPdfFile(jasperPrint, rutaDestino);
    }
}
