package com.example.negocio.cotizacion.usecase;

import static com.example.negocio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.mycompany.persistencia.dominio.Cliente;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Pago;
import com.mycompany.persistencia.repository.CotizacionRepository;
import com.mycompany.persistencia.repository.PagoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Nombres sugeridos al guardar contratos y comprobantes. */
@ExtendWith(MockitoExtension.class)
class NombreArchivoPdfTest {

    @Mock CotizacionRepository cotizacionRepository;
    @Mock PagoRepository pagoRepository;

    Cotizacion cotizacion = cotizacion(7, paqueteFijo(1, "Renta de local", 9800), null, null, 9800.0);

    @BeforeEach
    void setUp() {
        cotizacion.setFolio("COT-2026-0012");
        Cliente cliente = new Cliente();
        cliente.setNombre("Juan Pérez Gómez");
        cotizacion.setCliente(cliente);
        lenient().when(cotizacionRepository.findById(7L)).thenReturn(Optional.of(cotizacion));
    }

    private static Pago pago(long id, String folio, LocalDateTime fecha) {
        Pago p = new Pago();
        p.setId(id);
        p.setFolioPago(folio);
        p.setFechaHora(fecha);
        return p;
    }

    @Test
    void elContratoLlevaElFolioYElCliente() {
        String nombre = new GenerarContratoUseCase(cotizacionRepository, pagoRepository).nombreArchivo(7L);

        assertThat(nombre).isEqualTo("Contrato COT-2026-0012 - Juan Pérez Gómez.pdf");
    }

    @Test
    void elComprobanteLlevaElFolioDelUltimoPago() {
        LocalDateTime ayer = LocalDateTime.now().minusDays(1);
        when(pagoRepository.findByCotizacionId(7L)).thenReturn(List.of(
                pago(1, "PAY-2026-0030", ayer),
                pago(2, "PAY-2026-0034", ayer.plusHours(5)),
                pago(3, "PAY-2026-0031", ayer.plusHours(1))));

        String nombre = new GenerarComprobanteUseCase(cotizacionRepository, pagoRepository).nombreArchivo(7L);

        assertThat(nombre).isEqualTo("Comprobante COT-2026-0012 PAY-2026-0034 - Juan Pérez Gómez.pdf");
    }

    @Test
    void sinPagosElComprobanteLlevaSoloElFolioDeLaCotizacion() {
        String nombre = new GenerarComprobanteUseCase(cotizacionRepository, pagoRepository).nombreArchivo(7L);

        assertThat(nombre).isEqualTo("Comprobante COT-2026-0012 - Juan Pérez Gómez.pdf");
    }

    @Test
    void quitaLosCaracteresQueWindowsNoPermite() {
        assertThat(NombreArchivoPdf.de("Contrato COT-1", "Ana / López: \"La Güera\"?  "))
                .isEqualTo("Contrato COT-1 - Ana López La Güera.pdf");
        assertThat(NombreArchivoPdf.de("Contrato COT-1", "Fiesta S.A.")).isEqualTo("Contrato COT-1 - Fiesta S.A.pdf");
        assertThat(NombreArchivoPdf.de(null, " ")).isEqualTo("Documento.pdf");
    }

    @Test
    void recortaLosNombresMuyLargos() {
        String nombre = NombreArchivoPdf.de("Contrato COT-1", "x".repeat(300));
        assertThat(nombre).hasSize(NombreArchivoPdf.LARGO_MAXIMO + ".pdf".length()).endsWith(".pdf");
    }
}
