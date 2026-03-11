package com.mycompany.persistencia.DAO;

import com.mycompany.persistencia.dominio.Usuario;

public class UsuarioDAO extends GenericDAO<Usuario> {

    public UsuarioDAO() {
        super(Usuario.class);
    }
}
