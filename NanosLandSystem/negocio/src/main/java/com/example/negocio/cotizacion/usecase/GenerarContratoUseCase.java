package com.example.negocio.cotizacion.usecase;

import com.example.negocio.exception.CotizacionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Chunk;
import com.lowagie.text.ListItem;
import com.lowagie.text.List;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Pago;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.PagoRepository;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GenerarContratoUseCase {

    private final CotizacionRepository cotizacionRepository;
    private final PagoRepository pagoRepository;

    @Transactional(readOnly = true)
    public byte[] generarContrato(Long cotizacionId) {
        if (cotizacionId == null) {
            throw new CotizacionException("Debe indicar la cotización.");
        }

        Cotizacion cotizacion = cotizacionRepository.findById(cotizacionId)
                .orElseThrow(() -> new CotizacionException("No existe la cotización indicada."));

        String folio = cotizacion.getFolio();
        var cliente = cotizacion.getCliente();
        var paquete = cotizacion.getPaquete();
        var evento = cotizacion.getEvento();
        var detalles = cotizacion.getServiciosExtra();

        java.util.List<Pago> pagos = pagoRepository.findByCotizacionId(cotizacionId);
        double anticipo = pagos == null ? 0.0 : pagos.stream().mapToDouble(Pago::getCantidad).sum();
        double total = cotizacion.getTotal();
        double saldo = total - anticipo;
        if (saldo < 0) saldo = 0;

        NumberFormat money = NumberFormat.getCurrencyInstance(new java.util.Locale("es", "MX"));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE dd 'de' MMMM 'del' yyyy", new java.util.Locale("es", "MX"));
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.LEGAL, 40, 40, 30, 30);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font subtitleFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 10);
            Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font smallFont = new Font(Font.HELVETICA, 9);
            Font smallBoldFont = new Font(Font.HELVETICA, 9, Font.BOLD);

            // HEADER
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new int[]{1, 3});
            
            PdfPCell logoCell = new PdfPCell(new Phrase("NANOS\nLAND", titleFont));
            logoCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerTable.addCell(logoCell);

            PdfPCell textCell = new PdfPCell();
            textCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            textCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph p1 = new Paragraph("CONTRATO DE ARRENDAMIENTO DE EVENTO\n", subtitleFont);
            p1.setAlignment(Element.ALIGN_CENTER);
            Paragraph p2 = new Paragraph("SALÓN DE FIESTAS INFANTILES NANOS LAND\nUBICADO EN CALLE ALLENDE ESQUINA CON TEBARI.\nCD. OBREGON, SONORA", smallFont);
            p2.setAlignment(Element.ALIGN_CENTER);
            textCell.addElement(p1);
            textCell.addElement(p2);
            headerTable.addCell(textCell);
            
            document.add(headerTable);

            // DATE & FOLIO
            Paragraph dateFolio = new Paragraph();
            dateFolio.add(new Chunk("Folio: ", boldFont));
            dateFolio.add(new Chunk(folio != null ? folio : "______", normalFont));
            dateFolio.add(new Chunk("                  A " + LocalDate.now().getDayOfMonth() + " DE " + 
                                     LocalDate.now().getMonth().name() + " DEL " + LocalDate.now().getYear(), normalFont));
            dateFolio.setAlignment(Element.ALIGN_RIGHT);
            document.add(dateFolio);
            document.add(new Paragraph(" "));

            // DATOS DEL CLIENTE
            document.add(new Paragraph("DATOS DEL CLIENTE", subtitleFont));
            PdfPTable tableCliente = new PdfPTable(2);
            tableCliente.setWidthPercentage(100);
            addRow(tableCliente, "NOMBRE:", cliente != null ? cliente.getNombre() : "", boldFont, normalFont);
            addRow(tableCliente, "FESTEJADO (A):", evento != null && evento.getNombreFestejado() != null ? evento.getNombreFestejado() : "", boldFont, normalFont);
            addRow(tableCliente, "TELÉFONO:", cliente != null && cliente.getTelefono() != null ? cliente.getTelefono() : "", boldFont, normalFont);
            
            String direccion = "________________________________________________________";
            if (cliente != null && cliente.getDireccion() != null) {
                String c = cliente.getDireccion().getCalle() != null ? cliente.getDireccion().getCalle() : "";
                String col = cliente.getDireccion().getColonia() != null ? ", Col. " + cliente.getDireccion().getColonia() : "";
                String ciu = cliente.getDireccion().getCiudad() != null ? ", " + cliente.getDireccion().getCiudad() : "";
                String cp = cliente.getDireccion().getCodigoPostal() != null ? " CP " + cliente.getDireccion().getCodigoPostal() : "";
                if (!c.isEmpty() || !col.isEmpty()) {
                    direccion = c + col + ciu + cp;
                }
            }
            addRow(tableCliente, "DIRECCIÓN:", direccion, boldFont, normalFont);
            document.add(tableCliente);
            document.add(new Paragraph(" "));

            // DATOS DEL EVENTO
            document.add(new Paragraph("DATOS DEL EVENTO", subtitleFont));
            PdfPTable tableEvento = new PdfPTable(2);
            tableEvento.setWidthPercentage(100);
            
            String fechaStr = (evento != null && evento.getFecha() != null) ? evento.getFecha().format(fmt).toUpperCase() : "";
            addRow(tableEvento, "FECHA:", fechaStr, boldFont, normalFont);
            
            String hrInicio = (evento != null && evento.getHoraInicio() != null) ? evento.getHoraInicio().format(timeFmt) : "________";
            String hrSalida = (evento != null && evento.getHoraFin() != null) ? evento.getHoraFin().format(timeFmt) : "________";
            
            PdfPCell cellHoras = new PdfPCell();
            cellHoras.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            cellHoras.addElement(new Phrase("HR DE INICIO: " + hrInicio + "      HR DE SALIDA: " + hrSalida, normalFont));
            
            PdfPCell lblHoras = new PdfPCell(new Phrase("HORARIO:", boldFont));
            lblHoras.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            tableEvento.addCell(lblHoras);
            tableEvento.addCell(cellHoras);

            addRow(tableEvento, "TIPO DE PAQUETE:", paquete != null ? paquete.getNombre() : "", boldFont, normalFont);
            addRow(tableEvento, "COSTO TOTAL:", money.format(total), boldFont, normalFont);
            addRow(tableEvento, "ANTICIPO:", money.format(anticipo), boldFont, normalFont);
            addRow(tableEvento, "SALDO RESTANTE:", money.format(saldo), boldFont, normalFont);
            document.add(tableEvento);
            document.add(new Paragraph(" "));

            // SU RENTA O PAQUETE INCLUYE
            document.add(new Paragraph("SU RENTA O PAQUETE INCLUYE:", subtitleFont));
            if (paquete != null && paquete.getDescripcion() != null) {
                document.add(new Paragraph(paquete.getDescripcion(), normalFont));
            }
            if (detalles != null && !detalles.isEmpty()) {
                document.add(new Paragraph("Servicios Extra Contratados:", smallBoldFont));
                for (var d : detalles) {
                    String n = d.getServicio() != null ? d.getServicio().getNombre() : "Servicio";
                    document.add(new Paragraph("- " + n + " (x" + d.getCantidad() + ")", normalFont));
                }
            }
            document.add(new Paragraph("Nota: ____________________________________________________________________", normalFont));
            document.add(new Paragraph(" "));

            // CLAUSULAS Y CONDICIONES
            document.add(new Paragraph("CLAUSULAS Y CONDICIONES", subtitleFont));
            List list = new List(List.UNORDERED, 10);
            list.setListSymbol("\u2022");
            
            String[] clausulas = {
                "Cualquier desperfecto ocasionado al inmueble o mobiliario, será pagado por el contratante.",
                "El anticipo será de $3,000.00, no hay devolución o traspaso por cancelación. Podrá reagendar su fecha dando aviso con al menos 21 días de anticipación, para moverla a un lapso no mayor de 120 días. Si desea una fecha posterior se cobrará un ajuste de $800.00 con precios sujetos a cambios. Si desea reagendar sin cumplir los 21 días tendrá que pagar $3000.00 para poder hacerlo. Se deberá liquidar 7 días antes de la fecha de su evento, en efectivo (o se cobrará IVA). De no realizarse la liquidación se dará por cancelado el evento.",
                "La capacidad máxima es de 120 personas incluyendo niños. Se colocarán máximo 10 mesas con 10 sillas. Se realiza conteo de entrada y salida de personas, una vez llegado al número máximo permitido, se cerrará el acceso. Para poder ingresar así sea 1 persona más, se pagará una multa de $2000.00 al momento. El evento dará por concluido si llegase a 150 personas por indicaciones de protección civil.",
                "El evento tiene una duración de 4 horas. La hora extra tiene un costo de $1800.00. Al finalizar su evento tiene 25 minutos de consideración para desalojar. En caso de exceder los 30 minutos será considerado hora extra.",
                "No se permite la introducción de envases de vidrio y está PROHIBIDO EL CONSUMO DE ALIMENTOS EN JUEGOS, los desechables y el hielo no están incluidos.",
                "Es OBLIGATORIO el uso de calcetines en los juegos, pueden ser los de su uso personal.",
                "EL CONSUMO DE ALCOHOL queda estrictamente PROHIBIDO dentro de las instalaciones, así como también en el área de estacionamiento. Nos encontramos en monitoreo, evítenos la pena de llamarle la atención.",
                "Los menores deben de estar siempre vigilados por un adulto, no nos hacemos responsables de accidentes o lesiones. Así mismo se deberán de respetar las indicaciones y normas de seguridad de cada juego e instalaciones.",
                "No se permiten el uso de confeti, slime, plumones, pinturas, plastilina o stickers, así como tampoco el uso de espumas, burbujas o pirotecnia.",
                "No se permiten la entrada de MASCOTAS, TABACO, VAPE o grupos de música en vivo.",
                "Nanos Land no se hace responsable por fallas de suministro de energía eléctrica por parte de la Comisión Federal De Electricidad. Nanos Land no cuenta con planta de luz."
            };

            for (String c : clausulas) {
                ListItem item = new ListItem(" " + c, smallFont);
                item.setAlignment(Element.ALIGN_JUSTIFIED);
                list.add(item);
            }
            document.add(list);
            
            Paragraph protesto = new Paragraph("\nPROTESTAMOS LO NECESARIO\n\n\n", subtitleFont);
            protesto.setAlignment(Element.ALIGN_CENTER);
            document.add(protesto);

            // FIRMAS
            PdfPTable tableFirmas = new PdfPTable(2);
            tableFirmas.setWidthPercentage(100);
            
            PdfPCell cellFirmaNanos = new PdfPCell(new Phrase("____________________________________\nSalón de fiestas infantiles Nanos Land", normalFont));
            cellFirmaNanos.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            cellFirmaNanos.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            PdfPCell cellFirmaCliente = new PdfPCell(new Phrase("____________________________________\nNombre y firma del contratante", normalFont));
            cellFirmaCliente.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            cellFirmaCliente.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            tableFirmas.addCell(cellFirmaNanos);
            tableFirmas.addCell(cellFirmaCliente);
            document.add(tableFirmas);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            try { document.close(); } catch (Exception ignored) {}
            throw new CotizacionException("Error al generar el contrato: " + e.getMessage());
        }
    }

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
        PdfPCell c2 = new PdfPCell(new Phrase(value != null ? value : "", valueFont));
        c2.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(c1);
        table.addCell(c2);
    }
}
