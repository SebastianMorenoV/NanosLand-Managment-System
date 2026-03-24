package com.example.negocio.cotizacion.usecase;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.repository.CotizacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // IMPORTANTE

import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class GenerarComprobanteUseCase {

    private final CotizacionRepository cotizacionRepository;

    @Transactional(readOnly = true) // Esto arregla el error de "no session"
    public byte[] generarTicketPDF(Long cotizacionId) {
        Cotizacion c = cotizacionRepository.findById(cotizacionId)
                .orElseThrow(() -> new RuntimeException("Cotización no encontrada"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Estilos
            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font negritaFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

            // Contenido
            Paragraph titulo = new Paragraph("NANOS LAND - COMPROBANTE DE ANTICIPO\n\n", tituloFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            document.add(new Paragraph("Folio: " + c.getFolio(), negritaFont));
            document.add(new Paragraph("Cliente: " + (c.getCliente() != null ? c.getCliente().getNombre() : "N/A"), normalFont));
            document.add(new Paragraph("Festejado: " + (c.getNombreFestejado() != null ? c.getNombreFestejado() : "N/A"), normalFont));
            document.add(new Paragraph("Fecha Evento: " + c.getFecha(), normalFont));
            document.add(new Paragraph("-----------------------------------------------------------"));

            String nombrePaquete = (c.getPaquete() != null) ? c.getPaquete().getNombre() : "Personalizado";
            document.add(new Paragraph("Servicio: " + nombrePaquete, normalFont));
            document.add(new Paragraph("\nTOTAL A PAGAR: $" + String.format("%.2f", c.getTotal()), negritaFont));

            document.add(new Paragraph("\n\n¡Gracias por tu reserva!", normalFont));

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF", e);
        }
        return baos.toByteArray();
    }
}