/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.negocio.cotizacion.usecase;

import com.example.negocio.exception.CotizacionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Pago;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.PagoRepository;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 *
 * @author skyro
 */
@Service
@RequiredArgsConstructor
public class GenerarComprobanteUseCase {

    private final CotizacionRepository cotizacionRepository;
    private final PagoRepository pagoRepository;

    @Transactional(readOnly = true)
    public byte[] generarComprobante(Long cotizacionId) {
        if (cotizacionId == null) {
            throw new CotizacionException("Debe indicar la cotización.");
        }

        Cotizacion cotizacion = cotizacionRepository.findById(cotizacionId)
                .orElseThrow(() -> new CotizacionException("No existe la cotización indicada."));

        // Aseguramos carga de relaciones (cliente/paquete/servicios) dentro de la transacción.
        String folio = cotizacion.getFolio();
        var cliente = cotizacion.getCliente();
        var paquete = cotizacion.getPaquete();
        var detalles = cotizacion.getServiciosExtra();

        List<Pago> pagos = pagoRepository.findByCotizacionId(cotizacionId);
        double anticipo = pagos == null ? 0.0 : pagos.stream().mapToDouble(Pago::getCantidad).sum();
        double total = cotizacion.getTotal();
        double saldo = total - anticipo;
        if (saldo < 0) {
            saldo = 0;
        }

        NumberFormat money = NumberFormat.getCurrencyInstance(new java.util.Locale("es", "MX"));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Rectangle pageSize = new Rectangle(226, 600);
        Document document = new Document(pageSize, 10, 10, 15, 15);
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Tipografías más pequeñas para que quepan en el ticket
            Font titleFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 8);
            Font boldFont = new Font(Font.HELVETICA, 8, Font.BOLD);

            Paragraph header = new Paragraph("NANOS LAND", titleFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Paragraph subHeader = new Paragraph("Blvd. de los Niños #123\nTel: (644) 123-4567\n ", normalFont);
            subHeader.setAlignment(Element.ALIGN_CENTER);
            document.add(subHeader);
            document.add(new Paragraph(" "));

            PdfPTable datos = new PdfPTable(2);
            datos.setWidthPercentage(100);
            datos.setWidths(new int[]{1, 2});

            var evento = cotizacion.getEvento();
            addRow(datos, "FOLIO:", folio != null ? folio : "-", boldFont, normalFont);
            addRow(datos, "FECHA:", evento != null && evento.getFecha() != null ? evento.getFecha().format(fmt) : "-", boldFont, normalFont);
            addRow(datos, "ESTADO:", cotizacion.getEstado() != null ? cotizacion.getEstado().name() : "-", boldFont, normalFont);
            addRow(datos, "CLIENTE:", cliente != null ? cliente.getNombre() : "-", boldFont, normalFont);
            addRow(datos, "TURNO:", evento != null && evento.getTurno() != null ? evento.getTurno().name() : "-", boldFont, normalFont);
            addRow(datos, "FESTEJADO:", evento != null && evento.getNombreFestejado() != null ? evento.getNombreFestejado() : "-", boldFont, normalFont);
            addRow(datos, "PAQUETE:", paquete != null ? paquete.getNombre() : "-", boldFont, normalFont);

            document.add(datos);
            document.add(new Paragraph(" "));

            Font lineTitleFont = new Font(Font.HELVETICA, 11, Font.BOLD);
            document.add(new Paragraph("SERVICIOS ADICIONALES", lineTitleFont));
            if (detalles != null && !detalles.isEmpty()) {
                for (var d : detalles) {
                    String nombre = d.getServicio() != null ? d.getServicio().getNombre() : "Servicio";
                    double precio = d.getPrecioUnitario();
                    int cantidad = d.getCantidad();
                    document.add(new Paragraph(nombre + "   x" + cantidad + "   -   " + money.format(precio), normalFont));
                }
            } else {
                document.add(new Paragraph("Sin servicios adicionales.", normalFont));
            }

            document.add(new Paragraph(" "));

            PdfPTable totales = new PdfPTable(2);
            totales.setWidthPercentage(100);
            totales.setWidths(new int[]{1, 2});

            addRow(totales, "TOTAL:", money.format(total), boldFont, normalFont);
            addRow(totales, "ANTICIPO:", money.format(anticipo), boldFont, normalFont);
            addRow(totales, "SALDO PENDIENTE:", money.format(saldo), boldFont, normalFont);

            document.add(totales);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            try {
                document.close();
            } catch (Exception ignored) {
            }
            throw new CotizacionException("Error al generar el comprobante: " + e.getMessage());
        }
    }

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
        PdfPCell c2 = new PdfPCell(new Phrase(value, valueFont));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(c1);
        table.addCell(c2);
    }
}
