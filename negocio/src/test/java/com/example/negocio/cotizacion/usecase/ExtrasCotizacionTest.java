package com.example.negocio.cotizacion.usecase;

import static com.example.negocio.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.common.dtos.DetalleCotizacionDTO;
import com.mycompany.persistencia.dominio.Cotizacion;
import com.mycompany.persistencia.dominio.Servicio;
import com.mycompany.persistencia.enums.CategoriaServicio;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Los extras van en un renglón con su cantidad, aunque la cotización sea de cuando se guardaba uno por unidad. */
class ExtrasCotizacionTest {

    private static DetalleCotizacionDTO renglon(long servicioId, String nombre, double precio, int cantidad) {
        DetalleCotizacionDTO d = new DetalleCotizacionDTO();
        d.setId(servicioId * 100 + cantidad);
        d.setServicioId(servicioId);
        d.setNombreServicio(nombre);
        d.setPrecioUnitario(precio);
        d.setCantidad(cantidad);
        d.setSubtotal(precio * cantidad);
        return d;
    }

    @Test
    void losRenglonesViejosDeUnaUnidadSeJuntanEnUnoConSuCantidad() {
        List<DetalleCotizacionDTO> viejos = new ArrayList<>();
        for (int i = 0; i < 20; i++) viejos.add(renglon(7, "Bollito", 19, 1));
        viejos.add(renglon(8, "Pastel 15 personas", 470, 1));

        List<DetalleCotizacionDTO> juntos = ExtrasCotizacion.juntarIguales(viejos);

        assertThat(juntos).extracting(d -> d.getNombreServicio() + " x" + d.getCantidad() + " = " + d.getSubtotal())
            .containsExactly("Bollito x20 = 380.0", "Pastel 15 personas x1 = 470.0");
        assertThat(juntos).allMatch(d -> d.getId() == null);
        assertThat(viejos).hasSize(21).allMatch(d -> d.getCantidad() == 1); // no toca los recibidos
    }

    @Test
    void conDatosDeLogisticaDistintosQuedanAparte() {
        DetalleCotizacionDTO vainilla = renglon(7, "Bollito", 19, 20);
        vainilla.setDesgloseOpciones("Vainilla");
        DetalleCotizacionDTO chocolate = renglon(7, "Bollito", 19, 10);
        chocolate.setDesgloseOpciones("Chocolate");
        DetalleCotizacionDTO masVainilla = renglon(7, "Bollito", 19, 5);
        masVainilla.setDesgloseOpciones("  Vainilla ");
        DetalleCotizacionDTO otraHora = renglon(7, "Bollito", 19, 5);
        otraHora.setDesgloseOpciones("Vainilla");
        otraHora.setHoraSugerida(LocalTime.of(17, 0));

        List<DetalleCotizacionDTO> juntos = ExtrasCotizacion.juntarIguales(
            List.of(vainilla, chocolate, masVainilla, otraHora));

        assertThat(juntos).extracting(d -> d.getDesgloseOpciones() + " x" + d.getCantidad())
            .containsExactly("Vainilla x25", "Chocolate x10", "Vainilla x5");
    }

    @Test
    void conOtroPrecioPactadoQuedanAparte() {
        List<DetalleCotizacionDTO> juntos = ExtrasCotizacion.juntarIguales(
            List.of(renglon(7, "Bollito", 19, 10), renglon(7, "Bollito", 21, 10)));

        assertThat(juntos).hasSize(2);
    }

    @Test
    void elTotalEstimadoMultiplicaPrecioPorCantidad() {
        double total = new CalcularTotalCotizacionUseCase().ejecutar(5000,
            List.of(renglon(7, "Bollito", 19, 150), renglon(8, "Pastel 15 personas", 470, 2)));

        assertThat(total).isEqualTo(5000 + 19 * 150 + 470 * 2);
    }

    @Test
    void paraLosDocumentosSeJuntaUnRenglonPorServicioYPrecio() {
        Servicio bollito = servicio(7, "Bollito", 19, CategoriaServicio.EXTRA);
        Servicio pastel = servicio(8, "Pastel 15 personas", 470, CategoriaServicio.EXTRA);
        Cotizacion cot = cotizacion(1, null, null, null, null);
        for (int i = 0; i < 12; i++) extra(cot, bollito, 1, 19);
        extra(cot, pastel, 1, 470);
        extra(cot, bollito, 30, 19).setDesgloseOpciones("Chocolate");

        var renglones = ExtrasCotizacion.paraMostrar(cot.getServiciosExtra());

        assertThat(renglones).extracting(r -> r.servicio().getNombre() + " x" + r.cantidad() + " = " + r.subtotal())
            .containsExactly("Bollito x42 = 798.0", "Pastel 15 personas x1 = 470.0");
    }
}
