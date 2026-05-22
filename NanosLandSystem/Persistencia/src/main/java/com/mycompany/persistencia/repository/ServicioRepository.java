/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.persistencia.repository;

import com.mycompany.persistencia.dominio.Servicio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author skyro
 */
public interface ServicioRepository extends JpaRepository<Servicio, Long>{
    List<Servicio> findByActivoTrue();
    
}
