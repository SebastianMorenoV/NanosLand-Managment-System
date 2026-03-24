/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.negocio.agenda.usecase;

import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.persistencia.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

/**
 * 
 * @author skyro
 */

@Service
@RequiredArgsConstructor
public class VerificarDisponibilidadTurnoUseCase {

    private final EventoRepository eventoRepository;

    /**
     * Determina el estado de disponibilidad de una fecha para el pintado del calendario.
     * @param fecha La fecha a consultar.
     * @return "LIBRE", "AMARILLO" (un turno ocupado) o "GRIS" (ambos ocupados).
     */
    public String obtenerEstadoDisponibilidad(LocalDate fecha) {
        List<Evento> eventos = eventoRepository.findByFecha(fecha);

        if (eventos.isEmpty()) {
            return "LIBRE"; // Flujo Básico: Día vacío
        }

        if (eventos.size() >= 2) {
            return "GRIS"; // Flujo 2.2.2: Fecha Llena (Bloqueado)
        }

        return "AMARILLO"; // Flujo 2.2.1: Fecha Medio Llena
    }

    /**
     * Verifica si un turno específico está disponible (usado al momento de elegir el ComboBox).
     */
    public boolean esTurnoDisponible(LocalDate fecha, TurnoEvento turno) {
        List<Evento> eventos = eventoRepository.findByFecha(fecha);
        
        // Buscamos si ya existe un evento que coincida con el turno solicitado
        return eventos.stream()
                .noneMatch(evento -> evento.getTurno() == turno);
    }
}