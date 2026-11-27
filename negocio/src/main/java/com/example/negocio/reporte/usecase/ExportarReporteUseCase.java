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
        try {
            exportarPDFInterno(eventos, logoStream, rutaDestino, inicio, fin);
        } finally {
            cerrarSalidasPendientes();
        }
    }

    private void exportarPDFInterno(List<EventoDTO> eventos, InputStream logoStream, String rutaDestino, LocalDate inicio, LocalDate fin) throws Exception {
        if (eventos == null || eventos.isEmpty()) {
            throw new IllegalArgumentException("No hay datos para exportar.");
        }

        // Calcular KPIs
        int totalEventos = eventos.size();
        // Los importes solo cuentan ventas reales: eventos confirmados, en curso o
        // finalizados. Un borrador sin anticipo o un evento cancelado no es ingreso.
        List<EventoDTO> vendidos = eventos.stream().filter(ExportarReporteUseCase::esVenta).collect(Collectors.toList());
        double totalIngresos = vendidos.stream().mapToDouble(e -> e.getTotalCotizacion() + e.getTotalCargosExtras()).sum();
        double ticketPromedio = !vendidos.isEmpty() ? totalIngresos / vendidos.size() : 0.0;
        double ingresosExtras = vendidos.stream().mapToDouble(EventoDTO::getTotalCargosExtras).sum();

        long cerrados = vendidos.size();
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
            periodo = "Todos los eventos";
        } else if (inicio != null && fin == null) {
            periodo = "Desde: " + inicio.format(formatter);
        } else if (inicio == null && fin != null) {
            periodo = "Hasta: " + fin.format(formatter);
        } else {
            periodo = "Del: " + inicio.format(formatter) + " Al: " + fin.format(formatter);
        }

        // Iniciar Documento OpenPDF
        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, abrirDestino(rutaDestino));
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
                Image logo = logoReducido(bytes);
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
            Map<String, Double> ingresosPorEstado = eventos.stream().filter(ExportarReporteUseCase::esVenta)
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
            // Mismo importe que suman los indicadores: cotización más cargos extra.
            addTableCell(table, String.format("$%,.2f", e.getTotalCotizacion() + e.getTotalCargosExtras()), fontNormal, rowColor, Element.ALIGN_RIGHT);
        }

        document.add(table);
        document.close();
    }

    /** Un evento cuenta como venta si está confirmado, en curso o finalizado. */
    private static boolean esVenta(EventoDTO e) {
        return e.getEstadoEvento() == com.mycompany.persistencia.enums.EstadoEvento.CONFIRMADO
            || e.getEstadoEvento() == com.mycompany.persistencia.enums.EstadoEvento.EN_CURSO
            || e.getEstadoEvento() == com.mycompany.persistencia.enums.EstadoEvento.FINALIZADO;
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

    public void exportarAdeudosPDF(List<com.mycompany.common.dtos.AdeudoDTO> adeudos, InputStream logoStream, String rutaDestino) throws Exception {
        try {
            exportarAdeudosPDFInterno(adeudos, logoStream, rutaDestino);
        } finally {
            cerrarSalidasPendientes();
        }
    }

    private void exportarAdeudosPDFInterno(List<com.mycompany.common.dtos.AdeudoDTO> adeudos, InputStream logoStream, String rutaDestino) throws Exception {
        if (adeudos == null || adeudos.isEmpty()) {
            throw new IllegalArgumentException("No hay datos de adeudos para exportar.");
        }

        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, abrirDestino(rutaDestino));
        document.open();

        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA, 20, Color.WHITE);
        Font fontFechaHdr = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.WHITE);
        Font fontHeaderTable = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);

        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2.5f, 1f, 0.5f});

        PdfPCell titleCell = new PdfPCell(new Phrase("Reporte de Adeudos por Cobrar", fontTitulo));
        titleCell.setBackgroundColor(new Color(231, 76, 60)); // Rojo oscuro para adeudos
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPadding(20);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(titleCell);

        String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        PdfPCell dateCell = new PdfPCell(new Phrase(dateStr, fontFechaHdr));
        dateCell.setBackgroundColor(new Color(231, 76, 60));
        dateCell.setBorder(Rectangle.NO_BORDER);
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        dateCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        dateCell.setPadding(20);
        headerTable.addCell(dateCell);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBackgroundColor(new Color(231, 76, 60));
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setPadding(10);
        
        try {
            if (logoStream != null) {
                byte[] bytes = logoStream.readAllBytes();
                Image logo = logoReducido(bytes);
                logo.scaleToFit(50, 50);
                logo.setAlignment(Element.ALIGN_RIGHT);
                logoCell.addElement(logo);
            }
        } catch (Exception e) {}
        headerTable.addCell(logoCell);
        
        document.add(headerTable);
        document.add(new Paragraph(" "));

        double sumaAdeudos = adeudos.stream().mapToDouble(com.mycompany.common.dtos.AdeudoDTO::getSaldoPendiente).sum();
        
        Font fontKPIKey = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY);
        Font fontKPIVal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(231, 76, 60));
        PdfPTable kpiTable = new PdfPTable(2);
        kpiTable.setWidthPercentage(50);
        kpiTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        kpiTable.setSpacingAfter(20);
        kpiTable.setWidths(new float[]{1.5f, 2f});
        
        addKPICell(kpiTable, "Total por Cobrar:", fontKPIKey, new Color(248, 249, 250), Element.ALIGN_RIGHT);
        addKPICell(kpiTable, String.format("$%,.2f", sumaAdeudos), fontKPIVal, new Color(248, 249, 250), Element.ALIGN_LEFT);
        addKPICell(kpiTable, "Eventos con Deuda:", fontKPIKey, new Color(248, 249, 250), Element.ALIGN_RIGHT);
        addKPICell(kpiTable, String.valueOf(adeudos.size()), fontKPIVal, new Color(248, 249, 250), Element.ALIGN_LEFT);
        document.add(kpiTable);

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 2f, 1.5f, 1.5f, 1.2f, 1.2f, 1.5f});

        String[] headers = {"Folio", "Cliente", "Teléfono", "Fecha", "G. Total", "Pagado", "Deuda"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontHeaderTable));
            cell.setBackgroundColor(new Color(192, 57, 43));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(8);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }

        boolean altRow = false;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (com.mycompany.common.dtos.AdeudoDTO a : adeudos) {
            Color rowColor = altRow ? new Color(245, 247, 250) : Color.WHITE;
            altRow = !altRow;

            addTableCell(table, a.getFolioCotizacion(), fontNormal, rowColor, Element.ALIGN_CENTER);
            addTableCell(table, a.getClienteNombre(), fontNormal, rowColor, Element.ALIGN_LEFT);
            addTableCell(table, a.getClienteTelefono() != null ? a.getClienteTelefono() : "N/A", fontNormal, rowColor, Element.ALIGN_CENTER);
            addTableCell(table, a.getFechaEvento() != null ? a.getFechaEvento().format(dtf) : "", fontNormal, rowColor, Element.ALIGN_CENTER);
            addTableCell(table, String.format("$%,.2f", a.getGranTotal()), fontNormal, rowColor, Element.ALIGN_RIGHT);
            addTableCell(table, String.format("$%,.2f", a.getTotalPagado()), fontNormal, rowColor, Element.ALIGN_RIGHT);
            
            Font fontRed = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(231, 76, 60));
            addTableCell(table, String.format("$%,.2f", a.getSaldoPendiente()), fontRed, rowColor, Element.ALIGN_RIGHT);
        }

        document.add(table);
        document.close();
    }
    public void exportarIngresosPDF(List<com.mycompany.common.dtos.IngresoDTO> ingresos, List<com.mycompany.common.dtos.VentaProductoDTO> ventasPorProducto, InputStream logoStream, String rutaDestino, LocalDate inicio, LocalDate fin) throws Exception {
        try {
            exportarIngresosPDFInterno(ingresos, ventasPorProducto, logoStream, rutaDestino, inicio, fin);
        } finally {
            cerrarSalidasPendientes();
        }
    }

    private void exportarIngresosPDFInterno(List<com.mycompany.common.dtos.IngresoDTO> ingresos, List<com.mycompany.common.dtos.VentaProductoDTO> ventasPorProducto, InputStream logoStream, String rutaDestino, LocalDate inicio, LocalDate fin) throws Exception {
        if (ingresos == null || ingresos.isEmpty()) {
            throw new IllegalArgumentException("No hay datos de ingresos para exportar.");
        }

        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, abrirDestino(rutaDestino));
        document.open();

        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA, 20, Color.WHITE);
        Font fontFechaHdr = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.WHITE);
        Font fontHeaderTable = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);

        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2.5f, 1f, 0.5f});

        PdfPCell titleCell = new PdfPCell(new Phrase("Reporte de Ingresos (Flujo de Caja)", fontTitulo));
        titleCell.setBackgroundColor(new Color(39, 174, 96)); // Verde para ingresos
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPadding(20);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(titleCell);

        String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        PdfPCell dateCell = new PdfPCell(new Phrase(dateStr, fontFechaHdr));
        dateCell.setBackgroundColor(new Color(39, 174, 96));
        dateCell.setBorder(Rectangle.NO_BORDER);
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        dateCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        dateCell.setPadding(20);
        headerTable.addCell(dateCell);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBackgroundColor(new Color(39, 174, 96));
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setPadding(10);
        try {
            if (logoStream != null) {
                byte[] bytes = logoStream.readAllBytes();
                Image logo = logoReducido(bytes);
                logo.scaleToFit(50, 50);
                logo.setAlignment(Element.ALIGN_RIGHT);
                logoCell.addElement(logo);
            }
        } catch (Exception e) {}
        headerTable.addCell(logoCell);
        document.add(headerTable);
        document.add(new Paragraph(" "));

        double totalIngresos = ingresos.stream().mapToDouble(com.mycompany.common.dtos.IngresoDTO::getCantidad).sum();
        
        Font fontKPIKey = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY);
        Font fontKPIVal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(39, 174, 96));
        PdfPTable kpiTable = new PdfPTable(2);
        kpiTable.setWidthPercentage(50);
        kpiTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        kpiTable.setSpacingAfter(20);
        kpiTable.setWidths(new float[]{1.5f, 2f});
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String periodo = (inicio != null ? inicio.format(dtf) : "Inicio") + " al " + (fin != null ? fin.format(dtf) : "Fin");
        
        addKPICell(kpiTable, "Periodo:", fontKPIKey, new Color(248, 249, 250), Element.ALIGN_RIGHT);
        addKPICell(kpiTable, periodo, fontKPIVal, new Color(248, 249, 250), Element.ALIGN_LEFT);
        double ingresosVentas = ingresos.stream().filter(com.mycompany.common.dtos.IngresoDTO::isVentaExtra)
            .mapToDouble(com.mycompany.common.dtos.IngresoDTO::getCantidad).sum();
        addKPICell(kpiTable, "Eventos:", fontKPIKey, new Color(248, 249, 250), Element.ALIGN_RIGHT);
        addKPICell(kpiTable, String.format("$%,.2f", totalIngresos - ingresosVentas), fontKPIVal, new Color(248, 249, 250), Element.ALIGN_LEFT);
        addKPICell(kpiTable, "Ventas extra:", fontKPIKey, new Color(248, 249, 250), Element.ALIGN_RIGHT);
        addKPICell(kpiTable, String.format("$%,.2f", ingresosVentas), fontKPIVal, new Color(248, 249, 250), Element.ALIGN_LEFT);
        addKPICell(kpiTable, "Ingreso Total:", fontKPIKey, new Color(248, 249, 250), Element.ALIGN_RIGHT);
        addKPICell(kpiTable, String.format("$%,.2f", totalIngresos), fontKPIVal, new Color(248, 249, 250), Element.ALIGN_LEFT);
        document.add(kpiTable);

        if (ventasPorProducto != null && !ventasPorProducto.isEmpty()) {
            Paragraph tituloVentas = new Paragraph("Ventas extra por producto",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY));
            tituloVentas.setSpacingAfter(6);
            document.add(tituloVentas);

            PdfPTable tablaVentas = new PdfPTable(3);
            tablaVentas.setWidthPercentage(60);
            tablaVentas.setHorizontalAlignment(Element.ALIGN_LEFT);
            tablaVentas.setWidths(new float[]{3f, 1f, 1.4f});
            tablaVentas.setSpacingAfter(16);
            for (String h : new String[]{"Producto", "Vendidos", "Total"}) {
                PdfPCell cell = new PdfPCell(new Phrase(h, fontHeaderTable));
                cell.setBackgroundColor(new Color(22, 160, 133));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(6);
                cell.setBorder(Rectangle.NO_BORDER);
                tablaVentas.addCell(cell);
            }
            boolean altVenta = false;
            for (com.mycompany.common.dtos.VentaProductoDTO v : ventasPorProducto) {
                Color fila = altVenta ? new Color(245, 247, 250) : Color.WHITE;
                altVenta = !altVenta;
                addTableCell(tablaVentas, v.getNombre(), fontNormal, fila, Element.ALIGN_LEFT);
                addTableCell(tablaVentas, String.valueOf(v.getCantidad()), fontNormal, fila, Element.ALIGN_CENTER);
                addTableCell(tablaVentas, String.format("$%,.2f", v.getTotal()), fontNormal, fila, Element.ALIGN_RIGHT);
            }
            document.add(tablaVentas);
        }

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.65f, 1.55f, 1.6f, 2.4f, 1.4f, 1.2f});

        String[] headers = {"Fecha", "Folio", "Origen", "Cliente / Productos", "Método", "Monto"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontHeaderTable));
            cell.setBackgroundColor(new Color(22, 160, 133));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(8);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }

        boolean altRow = false;
        DateTimeFormatter dtfTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (com.mycompany.common.dtos.IngresoDTO i : ingresos) {
            Color rowColor = altRow ? new Color(245, 247, 250) : Color.WHITE;
            altRow = !altRow;

            addTableCell(table, i.getFechaHora() != null ? i.getFechaHora().format(dtfTime) : "", fontNormal, rowColor, Element.ALIGN_CENTER);
            addTableCell(table, i.getFolioPago(), fontNormal, rowColor, Element.ALIGN_CENTER);
            // Pagos de eventos: su cotización. Ventas extra: se identifican como tales.
            addTableCell(table, i.isVentaExtra() ? i.getOrigen() : i.getFolioCotizacion(), fontNormal, rowColor, Element.ALIGN_CENTER);
            addTableCell(table, i.getClienteNombre(), fontNormal, rowColor, Element.ALIGN_LEFT);
            addTableCell(table, nombreMetodoPago(i.getTipo()), fontNormal, rowColor, Element.ALIGN_CENTER);
            addTableCell(table, String.format("$%,.2f", i.getCantidad()), fontNormal, rowColor, Element.ALIGN_RIGHT);
        }

        document.add(table);
        document.close();
    }

    public void exportarOportunidadesPDF(List<com.mycompany.common.dtos.OportunidadDTO> oportunidades, InputStream logoStream, String rutaDestino, String tituloMes) throws Exception {
        try {
            exportarOportunidadesPDFInterno(oportunidades, logoStream, rutaDestino, tituloMes);
        } finally {
            cerrarSalidasPendientes();
        }
    }

    private void exportarOportunidadesPDFInterno(List<com.mycompany.common.dtos.OportunidadDTO> oportunidades, InputStream logoStream, String rutaDestino, String tituloMes) throws Exception {
        if (oportunidades == null || oportunidades.isEmpty()) {
            throw new IllegalArgumentException("No hay datos para exportar.");
        }

        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, abrirDestino(rutaDestino));
        document.open();

        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA, 20, Color.WHITE);
        Font fontFechaHdr = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.WHITE);
        Font fontHeaderTable = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);

        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2.5f, 1f, 0.5f});

        PdfPCell titleCell = new PdfPCell(new Phrase("Reporte de Oportunidades de Venta", fontTitulo));
        titleCell.setBackgroundColor(new Color(243, 156, 18)); // Naranja
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPadding(20);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(titleCell);

        String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        PdfPCell dateCell = new PdfPCell(new Phrase(dateStr, fontFechaHdr));
        dateCell.setBackgroundColor(new Color(243, 156, 18));
        dateCell.setBorder(Rectangle.NO_BORDER);
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        dateCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        dateCell.setPadding(20);
        headerTable.addCell(dateCell);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBackgroundColor(new Color(243, 156, 18));
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setPadding(10);
        try {
            if (logoStream != null) {
                byte[] bytes = logoStream.readAllBytes();
                Image logo = logoReducido(bytes);
                logo.scaleToFit(50, 50);
                logo.setAlignment(Element.ALIGN_RIGHT);
                logoCell.addElement(logo);
            }
        } catch (Exception e) {}
        headerTable.addCell(logoCell);
        document.add(headerTable);
        document.add(new Paragraph(" "));

        Font fontKPIKey = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY);
        Font fontKPIVal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(211, 84, 0));
        PdfPTable kpiTable = new PdfPTable(2);
        kpiTable.setWidthPercentage(50);
        kpiTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        kpiTable.setSpacingAfter(20);
        kpiTable.setWidths(new float[]{1.5f, 2f});
        
        addKPICell(kpiTable, "Filtro:", fontKPIKey, new Color(248, 249, 250), Element.ALIGN_RIGHT);
        addKPICell(kpiTable, tituloMes, fontKPIVal, new Color(248, 249, 250), Element.ALIGN_LEFT);
        addKPICell(kpiTable, "Leads (Contactos):", fontKPIKey, new Color(248, 249, 250), Element.ALIGN_RIGHT);
        addKPICell(kpiTable, String.valueOf(oportunidades.size()), fontKPIVal, new Color(248, 249, 250), Element.ALIGN_LEFT);
        document.add(kpiTable);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 1.5f, 1.5f, 2f, 1.2f});

        String[] headers = {"Cliente", "Teléfono", "Fecha Pasada", "Paquete Anterior", "Gasto Anterior"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontHeaderTable));
            cell.setBackgroundColor(new Color(211, 84, 0));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(8);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }

        boolean altRow = false;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (com.mycompany.common.dtos.OportunidadDTO o : oportunidades) {
            Color rowColor = altRow ? new Color(245, 247, 250) : Color.WHITE;
            altRow = !altRow;

            addTableCell(table, o.getClienteNombre(), fontNormal, rowColor, Element.ALIGN_LEFT);
            addTableCell(table, o.getClienteTelefono(), fontNormal, rowColor, Element.ALIGN_CENTER);
            addTableCell(table, o.getFechaEventoPasado() != null ? o.getFechaEventoPasado().format(dtf) : "", fontNormal, rowColor, Element.ALIGN_CENTER);
            addTableCell(table, o.getNombrePaquete(), fontNormal, rowColor, Element.ALIGN_LEFT);
            addTableCell(table, String.format("$%,.2f", o.getMontoGastado()), fontNormal, rowColor, Element.ALIGN_RIGHT);
        }

        document.add(table);
        document.close();
    }
    // ─────────────────────────────────────────────────────────────────────────
    // Reporte semanal de logística
    // ─────────────────────────────────────────────────────────────────────────

    public void exportarLogisticaSemanalPDF(com.mycompany.common.dtos.ReporteLogisticaSemanalDTO reporte, InputStream logoStream, String rutaDestino) throws Exception {
        try {
            exportarLogisticaSemanalPDFInterno(reporte, logoStream, rutaDestino);
        } finally {
            cerrarSalidasPendientes();
        }
    }

    private void exportarLogisticaSemanalPDFInterno(com.mycompany.common.dtos.ReporteLogisticaSemanalDTO reporte, InputStream logoStream, String rutaDestino) throws Exception {
        if (reporte == null || reporte.getDias() == null || reporte.getDias().isEmpty()) {
            throw new IllegalArgumentException("No hay datos de logística para exportar.");
        }

        Color azul = new Color(26, 130, 184);
        Color oscuro = new Color(44, 62, 80);
        Color grisDia = new Color(236, 240, 241);
        Color grisTexto = new Color(127, 140, 141);

        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, abrirDestino(rutaDestino));
        document.open();

        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA, 20, Color.WHITE);
        Font fontFechaHdr = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.WHITE);
        Font fontRango = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, oscuro);
        Font fontDia = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
        Font fontDiaDer = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.WHITE);
        Font fontDiaVacio = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, grisTexto);
        Font fontDiaVacioDer = FontFactory.getFont(FontFactory.HELVETICA, 10, grisTexto);
        Font fontTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, azul);
        Font fontHorario = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, azul);
        Font fontDescripcion = FontFactory.getFont(FontFactory.HELVETICA, 10, oscuro);
        Font fontImportante = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(192, 57, 43));
        Font fontServicio = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, oscuro);
        Font fontDetalle = FontFactory.getFont(FontFactory.HELVETICA, 10, grisTexto);
        Font fontCasilla = FontFactory.getFont(FontFactory.ZAPFDINGBATS, 13, grisTexto);
        Font fontCasillaLista = FontFactory.getFont(FontFactory.ZAPFDINGBATS, 13, new Color(39, 174, 96));

        // Cabecera (la misma de los demás reportes)
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2.5f, 1f, 0.5f});

        PdfPCell titleCell = new PdfPCell(new Phrase("Logística de la semana", fontTitulo));
        titleCell.setBackgroundColor(azul);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPadding(20);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        headerTable.addCell(titleCell);

        String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        PdfPCell dateCell = new PdfPCell(new Phrase(dateStr, fontFechaHdr));
        dateCell.setBackgroundColor(azul);
        dateCell.setBorder(Rectangle.NO_BORDER);
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        dateCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        dateCell.setPadding(20);
        headerTable.addCell(dateCell);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBackgroundColor(azul);
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setPadding(10);
        try {
            if (logoStream != null) {
                byte[] bytes = logoStream.readAllBytes();
                Image logo = logoReducido(bytes);
                logo.scaleToFit(50, 50);
                logo.setAlignment(Element.ALIGN_RIGHT);
                logoCell.addElement(logo);
            }
        } catch (Exception e) {}
        headerTable.addCell(logoCell);
        document.add(headerTable);

        Paragraph rango = new Paragraph(reporte.getRango(), fontRango);
        rango.setSpacingBefore(12);
        rango.setSpacingAfter(4);
        document.add(rango);

        for (com.mycompany.common.dtos.ReporteLogisticaSemanalDTO.Dia dia : reporte.getDias()) {
            int n = dia.getEventos().size();
            if (n == 0) {
                PdfPTable vacio = tablaLogistica();
                agregarBarraDia(vacio, dia.getNombre(), "Sin eventos", grisDia, fontDiaVacio, fontDiaVacioDer);
                document.add(vacio);
                continue;
            }

            boolean primero = true;
            for (com.mycompany.common.dtos.ReporteLogisticaSemanalDTO.EventoLogistica ev : dia.getEventos()) {
                // Cada evento va en una sola tabla para que no se parta entre dos páginas;
                // el primero lleva la barra del día, para que no quede sola al final de una hoja.
                PdfPTable tabla = tablaLogistica();
                if (primero) {
                    agregarBarraDia(tabla, dia.getNombre(), n + (n == 1 ? " evento" : " eventos"), oscuro, fontDia, fontDiaDer);
                    if (n > 1) {
                        PdfPCell total = new PdfPCell(new Phrase("Total del día: " + dia.getResumenTexto(), fontTotal));
                        total.setColspan(4);
                        total.setBackgroundColor(new Color(235, 245, 251));
                        total.setBorder(Rectangle.NO_BORDER);
                        total.setPadding(7);
                        tabla.addCell(total);
                    }
                    primero = false;
                }

                PdfPCell cabecera = new PdfPCell();
                cabecera.setColspan(4);
                cabecera.setBorder(Rectangle.NO_BORDER);
                cabecera.setPaddingTop(6);
                cabecera.setPaddingBottom(4);
                cabecera.addElement(new Paragraph(ev.getHorario(), fontHorario));
                cabecera.addElement(new Paragraph(ev.getDescripcion(), fontDescripcion));
                if (ev.getNotas() != null && !ev.getNotas().isBlank()) {
                    cabecera.addElement(new Paragraph("Importante: " + ev.getNotas().trim(), fontImportante));
                }
                tabla.addCell(cabecera);

                if (ev.getServicios().isEmpty()) {
                    PdfPCell sin = new PdfPCell(new Phrase("No tiene servicios registrados.", fontDetalle));
                    sin.setColspan(4);
                    sin.setBorder(Rectangle.NO_BORDER);
                    tabla.addCell(sin);
                }
                for (com.mycompany.common.dtos.ReporteLogisticaSemanalDTO.Servicio sv : ev.getServicios()) {
                    // En ZapfDingbats, "o" es una casilla vacía y "4" una palomita.
                    tabla.addCell(celdaLogistica(sv.isListo() ? "4" : "o", sv.isListo() ? fontCasillaLista : fontCasilla, Element.ALIGN_CENTER));
                    tabla.addCell(celdaLogistica(String.valueOf(sv.getCantidad()), fontServicio, Element.ALIGN_CENTER));
                    tabla.addCell(celdaLogistica(sv.getEtiqueta(), fontServicio, Element.ALIGN_LEFT));
                    tabla.addCell(celdaLogistica(sv.getDetalle(), fontDetalle, Element.ALIGN_LEFT));
                }
                document.add(tabla);
            }
        }

        document.close();
    }

    private static PdfPTable tablaLogistica() throws Exception {
        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{0.4f, 0.4f, 2.8f, 3.6f});
        tabla.setSpacingBefore(8);
        tabla.setKeepTogether(true);
        return tabla;
    }

    private static void agregarBarraDia(PdfPTable tabla, String dia, String cuenta, Color fondo, Font fuenteDia, Font fuenteCuenta) {
        PdfPCell nombre = new PdfPCell(new Phrase(dia, fuenteDia));
        nombre.setColspan(3);
        nombre.setBackgroundColor(fondo);
        nombre.setBorder(Rectangle.NO_BORDER);
        nombre.setPadding(7);
        tabla.addCell(nombre);

        PdfPCell derecha = new PdfPCell(new Phrase(cuenta, fuenteCuenta));
        derecha.setBackgroundColor(fondo);
        derecha.setBorder(Rectangle.NO_BORDER);
        derecha.setPadding(7);
        derecha.setHorizontalAlignment(Element.ALIGN_RIGHT);
        derecha.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabla.addCell(derecha);
    }

    private static PdfPCell celdaLogistica(String texto, Font fuente, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColor(new Color(236, 240, 241));
        celda.setPaddingTop(4);
        celda.setPaddingBottom(5);
        celda.setHorizontalAlignment(alineacion);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return celda;
    }

    /** "TRANSFERENCIA" -> "Transferencia": más legible y cabe en la columna. */
    private static String nombreMetodoPago(String metodo) {
        if (metodo == null || metodo.equals("N/A") || metodo.length() < 2) {
            return metodo;
        }
        return metodo.charAt(0) + metodo.substring(1).toLowerCase();
    }

    /**
     * Logo para la cabecera de los reportes. El archivo original mide 2000 px y pesa más
     * de 1 MB; incrustarlo tal cual hacía que cada PDF pesara más de 2 MB para mostrarlo
     * a 50 puntos. Se reduce antes de incrustarlo; si no se puede leer, se usa el original.
     */
    private static Image logoReducido(byte[] bytes) throws Exception {
        try {
            BufferedImage original = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            if (original != null && original.getWidth() > 300) {
                int ancho = 300;
                int alto = Math.max(1, original.getHeight() * ancho / original.getWidth());
                BufferedImage reducido = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = reducido.createGraphics();
                g.drawImage(original.getScaledInstance(ancho, alto, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
                g.dispose();
                return Image.getInstance(reducido, null);
            }
        } catch (Exception e) {
            // Se continúa con la imagen original.
        }
        return Image.getInstance(bytes);
    }

    /**
     * Abre el archivo destino del PDF. El flujo se registra para cerrarlo aunque la
     * generación falle a medias: de lo contrario el archivo queda truncado y bloqueado
     * por Windows hasta cerrar la aplicación.
     */
    private static FileOutputStream abrirDestino(String rutaDestino) throws java.io.IOException {
        FileOutputStream salida = new FileOutputStream(rutaDestino);
        SALIDAS_ABIERTAS.get().add(salida);
        return salida;
    }

    private static final ThreadLocal<java.util.List<FileOutputStream>> SALIDAS_ABIERTAS =
        ThreadLocal.withInitial(java.util.ArrayList::new);

    /** Cierra los archivos que una exportación haya dejado abiertos. */
    private static void cerrarSalidasPendientes() {
        for (FileOutputStream salida : SALIDAS_ABIERTAS.get()) {
            try {
                salida.close();
            } catch (java.io.IOException e) {
                // ya estaba cerrado o no se pudo cerrar: no hay nada más que hacer
            }
        }
        SALIDAS_ABIERTAS.get().clear();
    }
}
