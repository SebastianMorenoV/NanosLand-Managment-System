package com.mycompany.persistencia.DAO;

import com.mycompany.persistencia.dominio.Evento;

public class EventoDAO extends GenericDAO<Evento> {
    public EventoDAO() { super(Evento.class); }
}