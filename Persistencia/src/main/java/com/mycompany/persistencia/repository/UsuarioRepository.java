/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.persistencia.repository;

import com.mycompany.persistencia.dominio.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 *
 * @author skyro
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long>{

    Optional<Usuario> findByCorreo(String correo);
    
    Optional<Usuario> findByCorreoAndActivoTrue(String correo);
    
    java.util.List<Usuario> findByActivoTrue();
}
