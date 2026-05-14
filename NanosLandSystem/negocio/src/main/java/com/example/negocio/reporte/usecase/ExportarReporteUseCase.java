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

    public void exportarPDF(List<EventoDTO> eventos, InputStream reporteStream, String rutaDestino, LocalDate inicio, LocalDate fin) throws Exception {
        if (eventos == null || eventos.isEmpty()) {
            throw new IllegalArgumentException("No hay datos para exportar.");
        }

        if (reporteStream == null) {
            throw new RuntimeException("El flujo de la plantilla es nulo.");
        }

        JasperReport jasperReport = JasperCompileManager.compileReport(reporteStream);

        // Calcular KPIs
        int totalEventos = eventos.size();
        double totalIngresos = eventos.stream().mapToDouble(EventoDTO::getTotalCotizacion).sum();
        double ticketPromedio = totalEventos > 0 ? totalIngresos / totalEventos : 0.0;

        // Formatear periodo
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String strInicio = (inicio != null) ? inicio.format(formatter) : "Inicio";
        String strFin = (fin != null) ? fin.format(formatter) : "Fin";
        String periodo = "Del: " + strInicio + " Al: " + strFin;

        // Crear el DataSource a partir de la lista de DTOs
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(eventos);

        // Parámetros adicionales
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("TituloReporte", "Reporte Gerencial de Eventos");
        parametros.put("Periodo", periodo);
        parametros.put("TotalEventos", String.valueOf(totalEventos));
        parametros.put("TotalIngresos", String.format("$%,.2f", totalIngresos));
        parametros.put("TicketPromedio", String.format("$%,.2f", ticketPromedio));

        // Llenar el reporte
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, dataSource);

        // Exportar a PDF
        JasperExportManager.exportReportToPdfFile(jasperPrint, rutaDestino);
    }
}
