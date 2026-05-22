package com.example.negocio.reporte.usecase;

import com.mycompany.common.dtos.EventoDTO;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExportarReporteUseCase {

    public void exportarPDF(List<EventoDTO> eventos, InputStream logoStream, String rutaDestino, LocalDate inicio, LocalDate fin) throws Exception {
        if (eventos == null || eventos.isEmpty()) {
            throw new IllegalArgumentException("No hay datos para exportar.");
        }

        // Calcular KPIs
        int totalEventos = eventos.size();
        double totalIngresos = eventos.stream().mapToDouble(e -> e.getTotalCotizacion() + e.getTotalCargosExtras()).sum();
        double ticketPromedio = totalEventos > 0 ? totalIngresos / totalEventos : 0.0;
        double ingresosExtras = eventos.stream().mapToDouble(EventoDTO::getTotalCargosExtras).sum();

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
        
        Map<String, Long> paquetesCount = eventos.stream()
            .filter(e -> e.getPaqueteNombre() != null && !e.getPaqueteNombre().isEmpty())
            .collect(Collectors.groupingBy(EventoDTO::getPaqueteNombre, Collectors.counting()));
            
        String paqueteEstrella = paquetesCount.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("N/A");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String periodo;
        if (inicio == null && fin == null) {
            periodo = "Todos los eventos (máx. 20)";
        } else if (inicio != null && fin == null) {
            periodo = "Desde: " + inicio.format(formatter);
        } else if (inicio == null && fin != null) {
            periodo = "Hasta: " + fin.format(formatter);
        } else {
            periodo = "Del: " + inicio.format(formatter) + " Al: " + fin.format(formatter);
        }

        // Iniciar Documento OpenPDF
        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(rutaDestino));
        document.open();

        // Fuentes
        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA, 20, Color.WHITE);
        Font fontFechaHdr = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.WHITE);
        Font fontHeaderTable = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Font fontKPIKey = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
        Font fontKPIVal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font fontKPIGreen = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(39, 174, 96));
        Font fontKPIRed = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(231, 76, 60));
        Font fontKPIPurple = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(142, 68, 173));
        Font fontKPIBlue = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(41, 128, 185));

        // Cabecera Azul Fuerte
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2.5f, 1f, 0.5f});

        PdfPCell titleCell = new PdfPCell(new Phrase("Reporte Gerencial de Eventos", fontTitulo));
        titleCell.setBackgroundColor(new Color(26, 130, 184)); 
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPadding(20);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(titleCell);

        String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        PdfPCell dateCell = new PdfPCell(new Phrase(dateStr, fontFechaHdr));
        dateCell.setBackgroundColor(new Color(26, 130, 184));
        dateCell.setBorder(Rectangle.NO_BORDER);
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        dateCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        dateCell.setPadding(20);
        headerTable.addCell(dateCell);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBackgroundColor(new Color(26, 130, 184));
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setPadding(10);
        
        try {
            if (logoStream != null) {
                byte[] bytes = logoStream.readAllBytes();
                Image logo = Image.getInstance(bytes);
                logo.scaleToFit(50, 50);
                logo.setAlignment(Element.ALIGN_RIGHT);
                logoCell.addElement(logo);
            }
        } catch (Exception e) {
            // Si falla el logo, no ponemos nada
        }
        headerTable.addCell(logoCell);
        
        document.add(headerTable);
        document.add(new Paragraph(" "));

        // Tabla KPIs (Sin bordes visuales pesados, solo fondo sutil)
        PdfPTable kpiTable = new PdfPTable(6);
        kpiTable.setWidthPercentage(100);
        kpiTable.setSpacingAfter(20);
        kpiTable.setWidths(new float[]{1.2f, 1.8f, 1.2f, 1.8f, 1.5f, 1.5f});
        
        Color bgKPI = new Color(248, 249, 250);

        // Fila 1
        addKPICell(kpiTable, "Periodo:", fontKPIKey, bgKPI, Element.ALIGN_RIGHT);
        addKPICell(kpiTable, periodo, fontKPIVal, bgKPI, Element.ALIGN_LEFT);
        addKPICell(kpiTable, "Top Paquete:", fontKPIKey, bgKPI, Element.ALIGN_RIGHT);
        addKPICell(kpiTable, paqueteEstrella, fontKPIBlue, bgKPI, Element.ALIGN_LEFT);
        addKPICell(kpiTable, "Ingresos Totales:", fontKPIKey, bgKPI, Element.ALIGN_RIGHT);
        addKPICell(kpiTable, String.format("$%,.2f", totalIngresos), fontKPIGreen, bgKPI, Element.ALIGN_LEFT);

        // Fila 2
        addKPICell(kpiTable, "Total Eventos:", fontKPIKey, bgKPI, Element.ALIGN_RIGHT);
        addKPICell(kpiTable, String.valueOf(totalEventos), fontKPIVal, bgKPI, Element.ALIGN_LEFT);
        addKPICell(kpiTable, "Ocupación:", fontKPIKey, bgKPI, Element.ALIGN_RIGHT);
        addKPICell(kpiTable, String.format("%.0f%% Mat / %.0f%% Vesp", ocupacionMatutino, ocupacionVespertino), fontKPIVal, bgKPI, Element.ALIGN_LEFT);
        addKPICell(kpiTable, "Ticket Promedio:", fontKPIKey, bgKPI, Element.ALIGN_RIGHT);
        addKPICell(kpiTable, String.format("$%,.2f", ticketPromedio), fontKPIRed, bgKPI, Element.ALIGN_LEFT);

        // Fila 3
        addKPICell(kpiTable, "Tasa Cierre:", fontKPIKey, bgKPI, Element.ALIGN_RIGHT);
        addKPICell(kpiTable, String.format("%.1f%%", tasaCierre), fontKPIVal, bgKPI, Element.ALIGN_LEFT);
        addKPICell(kpiTable, "", fontKPIKey, bgKPI, Element.ALIGN_RIGHT);
        addKPICell(kpiTable, "", fontKPIVal, bgKPI, Element.ALIGN_LEFT);
        addKPICell(kpiTable, "Ingresos Extras:", fontKPIKey, bgKPI, Element.ALIGN_RIGHT);
        addKPICell(kpiTable, String.format("$%,.2f", ingresosExtras), fontKPIPurple, bgKPI, Element.ALIGN_LEFT);

        document.add(kpiTable);

        // Gráficos (JFreeChart)
        PdfPTable chartTable = new PdfPTable(2);
        chartTable.setWidthPercentage(100);
        chartTable.setWidths(new float[]{1f, 1f});
        chartTable.setSpacingAfter(20);

        // Paleta de colores modernos (Flat UI)
        Color[] modernColors = {
            new Color(41, 128, 185),   // Azul
            new Color(39, 174, 96),    // Verde
            new Color(142, 68, 173),   // Morado
            new Color(243, 156, 18),   // Naranja
            new Color(231, 76, 60),    // Rojo
            new Color(22, 160, 133)    // Turquesa
        };

        try {
            // Pie Chart (Paquetes)
            DefaultPieDataset pieDataset = new DefaultPieDataset();
            paquetesCount.forEach(pieDataset::setValue);
            JFreeChart pieChart = ChartFactory.createPieChart("Distribución de Paquetes", pieDataset, false, true, false);
            pieChart.setBackgroundPaint(java.awt.Color.WHITE);
            pieChart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));
            
            PiePlot piePlot = (PiePlot) pieChart.getPlot();
            piePlot.setBackgroundPaint(java.awt.Color.WHITE);
            piePlot.setOutlineVisible(false);
            piePlot.setShadowPaint(null); // Quitar sombra vieja
            piePlot.setSectionOutlinesVisible(false); // Quitar bordes de rebanadas
            piePlot.setLabelBackgroundPaint(new Color(255, 255, 255, 200));
            piePlot.setLabelOutlinePaint(null);
            piePlot.setLabelShadowPaint(null);
            piePlot.setLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
            
            // Asignar colores modernos al pastel
            int colorIndex = 0;
            for (Object key : pieDataset.getKeys()) {
                piePlot.setSectionPaint((Comparable) key, modernColors[colorIndex % modernColors.length]);
                colorIndex++;
            }

            // Bar Chart (Ingresos por Estado)
            DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
            Map<String, Double> ingresosPorEstado = eventos.stream()
                .collect(Collectors.groupingBy(e -> e.getEstadoEvento() != null ? e.getEstadoEvento().toString() : "N/A",
                         Collectors.summingDouble(e -> e.getTotalCotizacion() + e.getTotalCargosExtras())));
            ingresosPorEstado.forEach((k, v) -> barDataset.addValue(v, "Ingresos", k));
            
            JFreeChart barChart = ChartFactory.createBarChart("Ingresos por Estado", "", "Ingresos ($)", barDataset, PlotOrientation.VERTICAL, false, true, false);
            barChart.setBackgroundPaint(java.awt.Color.WHITE);
            barChart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));
            
            CategoryPlot barPlot = (CategoryPlot) barChart.getPlot();
            barPlot.setBackgroundPaint(java.awt.Color.WHITE);
            barPlot.setOutlineVisible(false);
            barPlot.setRangeGridlinePaint(new Color(230, 230, 230)); // Gridlines sutiles
            
            // Fuentes de ejes
            barPlot.getDomainAxis().setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));
            barPlot.getRangeAxis().setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));
            barPlot.getRangeAxis().setLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));

            // Quitar el efecto 3D viejo de las barras
            BarRenderer renderer = (BarRenderer) barPlot.getRenderer();
            renderer.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter()); // Flat style
            renderer.setShadowVisible(false); // Quitar sombra
            renderer.setSeriesPaint(0, new Color(41, 128, 185)); // Azul moderno
            renderer.setMaximumBarWidth(0.15); // Barras más delgadas y elegantes

            // Convert to Images
            BufferedImage pieImg = pieChart.createBufferedImage(400, 250);
            Image pdfPieImg = Image.getInstance(writer, pieImg, 1.0f);
            PdfPCell pieCell = new PdfPCell(pdfPieImg, true);
            pieCell.setBorder(Rectangle.NO_BORDER);
            pieCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            chartTable.addCell(pieCell);

            BufferedImage barImg = barChart.createBufferedImage(400, 250);
            Image pdfBarImg = Image.getInstance(writer, barImg, 1.0f);
            PdfPCell barCell = new PdfPCell(pdfBarImg, true);
            barCell.setBorder(Rectangle.NO_BORDER);
            barCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            chartTable.addCell(barCell);
            
            document.add(chartTable);
        } catch (Exception ex) {
            // Si hay error en charts, ignora y continúa
        }
        


        // Tabla de Eventos
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        // Aumentar ancho de Estado (columna 5) y reducir Fecha/Horario levemente para que no haga salto
        table.setWidths(new float[]{1.3f, 1.6f, 2.3f, 2.1f, 1.5f, 1.5f});

        String[] headers = {"Fecha", "Horario", "Cliente", "Paquete", "Estado", "Total"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontHeaderTable));
            cell.setBackgroundColor(new Color(44, 62, 80)); // #2C3E50
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(8);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }

        boolean altRow = false;
        for (EventoDTO e : eventos) {
            Color rowColor = altRow ? new Color(245, 247, 250) : Color.WHITE;
            altRow = !altRow;

            addTableCell(table, e.getFecha() != null ? e.getFecha().toString() : "", fontNormal, rowColor, Element.ALIGN_CENTER);
            
            String horario = "";
            if (e.getHoraInicio() != null && e.getHoraFin() != null) {
                horario = e.getHoraInicio().toString() + " - " + e.getHoraFin().toString();
            } else if (e.getTurno() != null) {
                horario = e.getTurno().toString();
            }
            addTableCell(table, horario, fontNormal, rowColor, Element.ALIGN_CENTER);
            
            addTableCell(table, e.getClienteNombre() != null ? e.getClienteNombre() : "", fontNormal, rowColor, Element.ALIGN_LEFT);
            addTableCell(table, e.getPaqueteNombre() != null ? e.getPaqueteNombre() : "", fontNormal, rowColor, Element.ALIGN_LEFT);
            addTableCell(table, e.getEstadoEvento() != null ? e.getEstadoEvento().toString() : "", fontNormal, rowColor, Element.ALIGN_CENTER);
            addTableCell(table, String.format("$%,.2f", e.getTotalCotizacion()), fontNormal, rowColor, Element.ALIGN_RIGHT);
        }

        document.add(table);
        document.close();
    }

    private void addKPICell(PdfPTable table, String text, Font font, Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setBorderColor(new Color(230, 230, 230));
        cell.setBorderWidthTop(0.5f);
        cell.setBorderWidthBottom(0.5f);
        cell.setBorderWidthLeft(0);
        cell.setBorderWidthRight(0);
        cell.setPadding(8);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setBorderColor(new Color(230, 230, 230));
        cell.setBorderWidthTop(0.5f);
        cell.setBorderWidthBottom(0.5f);
        cell.setBorderWidthLeft(0);
        cell.setBorderWidthRight(0);
        cell.setPadding(6);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }
}
