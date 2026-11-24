package com.mycompany.common.dtos;

import com.mycompany.persistencia.enums.TipoDescuento;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Descuento automático al precio de los paquetes (ver la entidad {@code Descuento}). */
@Data
@NoArgsConstructor
public class DescuentoDTO {

    private Long id;
    private String nombre;
    private TipoDescuento tipo = TipoDescuento.PORCENTAJE;
    /** Porcentaje (1 a 100) o pesos, según {@link #tipo}. */
    private double valor;
    /** Días de la semana del evento en que aplica. Vacío: todos los días. */
    private Set<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
    /** Temporada: fechas del evento. Nulas: sin límite por ese lado. */
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    /** Último día en que se puede cotizar con este descuento. Nulo: sin límite. */
    private LocalDate cotizarHasta;
    private boolean todosLosPaquetes = true;
    /** Paquetes a los que aplica cuando no es para todos. */
    private List<Long> paqueteIds = new ArrayList<>();
    /** Solo de salida: nombres de esos paquetes, en el mismo orden. */
    private List<String> paqueteNombres = new ArrayList<>();
    private boolean activo = true;
    /** Solo de salida: lo calcula el caso de uso con la fecha de hoy. */
    private Estado estado;

    public enum Estado {
        /** Encendido y todavía puede aplicar a cotizaciones nuevas. */
        ACTIVO("Activo"),
        /** Ya terminó su temporada o su plazo para cotizar. */
        VENCIDO("Vencido"),
        /** Pausado desde la lista (apagado sin borrarlo). */
        DESACTIVADO("Pausado");

        private final String etiqueta;

        Estado(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String getEtiqueta() {
            return etiqueta;
        }
    }
}
