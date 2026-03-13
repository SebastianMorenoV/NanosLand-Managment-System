/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.negocio.agenda.usecase;

import com.mycompany.persistencia.dominio.enums.TurnoEvento;
import com.mycompany.persistencia.repository.EventoRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 *
 * @author skyro
 */
@Service
@RequiredArgsConstructor
public class ConsultarAgendaUseCase {
    
    private final EventoRepository eventoRepository;
    
    public void obtenerMesCompleto() {
        
    }
    
    public void verificarTurnoDisponible(LocalDate fecha, TurnoEvento turno) {
    }
}
