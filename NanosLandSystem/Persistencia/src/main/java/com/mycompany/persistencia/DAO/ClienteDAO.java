package com.mycompany.persistencia.DAO;

import com.mycompany.persistencia.dominio.Cliente;

public class ClienteDAO extends GenericDAO<Cliente> {
    public ClienteDAO() { super(Cliente.class); }
}