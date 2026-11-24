package com.mycompany.common.mapper;

import com.mycompany.common.dtos.DescuentoDTO;
import com.mycompany.persistencia.dominio.Descuento;
import com.mycompany.persistencia.dominio.Paquete;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class DescuentoMapper {

    /** Sin el estado: depende de la fecha de hoy y lo asigna el caso de uso. */
    public static DescuentoDTO toDTO(Descuento descuento) {
        if (descuento == null) return null;
        DescuentoDTO dto = new DescuentoDTO();
        dto.setId(descuento.getId());
        dto.setNombre(descuento.getNombre());
        dto.setTipo(descuento.getTipo());
        dto.setValor(descuento.getValor());
        EnumSet<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
        if (descuento.getDias() != null) dias.addAll(descuento.getDias());
        dto.setDias(dias);
        dto.setFechaInicio(descuento.getFechaInicio());
        dto.setFechaFin(descuento.getFechaFin());
        dto.setCotizarHasta(descuento.getCotizarHasta());
        dto.setTodosLosPaquetes(descuento.isTodosLosPaquetes());
        List<Long> ids = new ArrayList<>();
        List<String> nombres = new ArrayList<>();
        if (descuento.getPaquetes() != null) {
            descuento.getPaquetes().stream()
                    .sorted(Comparator.comparing(Paquete::getNombre, String.CASE_INSENSITIVE_ORDER))
                    .forEach(p -> {
                        ids.add(p.getId());
                        nombres.add(p.isActivo() ? p.getNombre() : p.getNombre() + " (ya no se ofrece)");
                    });
        }
        dto.setPaqueteIds(ids);
        dto.setPaqueteNombres(nombres);
        dto.setActivo(descuento.isActivo());
        return dto;
    }
}
