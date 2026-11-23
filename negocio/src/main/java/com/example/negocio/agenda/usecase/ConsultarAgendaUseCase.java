/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.negocio.agenda.usecase;

import com.mycompany.persistencia.dominio.Evento;
import com.mycompany.persistencia.enums.TurnoEvento;
import com.mycompany.persistencia.repository.EventoRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author skyro
 */
@Service
@RequiredArgsConstructor
public class ConsultarAgendaUseCase {
    private final EventoRepository eventoRepository;

    @Transactional(readOnly = true)  // Fix #11: evita dirty-checking innecesario
    public List<Evento> obtenerMesCompleto(int anio, int mes) {
        LocalDate inicio = YearMonth.of(anio, mes).atDay(1);
        LocalDate fin = YearMonth.of(anio, mes).atEndOfMonth();
        return eventoRepository.findByFechaBetween(inicio, fin);
    }
}
