package com.mycompany.presentacion.utils;

import com.mycompany.common.dtos.ReporteLogisticaSemanalDTO;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

/**
 * Combos para elegir la hora de inicio y la hora de fin de un evento, cada media hora.
 * La hora de fin termina en las 00:00, que se muestra como medianoche.
 */
public final class ComboHoras {

    private static final int MINUTOS_ENTRE_OPCIONES = 30;

    private ComboHoras() {
    }

    /** Opciones de 12:00 am a 11:30 pm. */
    public static void configurarInicio(ComboBox<LocalTime> combo) {
        configurar(combo, horasDelDia(), "Hora de inicio...", false);
    }

    /** Opciones de 12:30 am a 11:30 pm, y al final medianoche. */
    public static void configurarFin(ComboBox<LocalTime> combo) {
        List<LocalTime> horas = horasDelDia();
        horas.remove(LocalTime.MIDNIGHT);
        horas.add(LocalTime.MIDNIGHT);
        configurar(combo, horas, "Hora de fin...", true);
    }

    private static List<LocalTime> horasDelDia() {
        List<LocalTime> horas = new ArrayList<>();
        for (int minuto = 0; minuto < 24 * 60; minuto += MINUTOS_ENTRE_OPCIONES) {
            horas.add(LocalTime.of(minuto / 60, minuto % 60));
        }
        return horas;
    }

    private static void configurar(ComboBox<LocalTime> combo, List<LocalTime> horas, String aviso,
                                   boolean esHoraFin) {
        combo.setItems(FXCollections.observableArrayList(horas));
        combo.setPromptText(aviso);
        combo.setVisibleRowCount(8);
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalTime hora) {
                if (hora == null) return "";
                if (esHoraFin && hora.equals(LocalTime.MIDNIGHT)) {
                    return "12:00 am (medianoche)";
                }
                return ReporteLogisticaSemanalDTO.horaTexto(hora);
            }

            @Override
            public LocalTime fromString(String texto) {
                return null;
            }
        });
    }
}
