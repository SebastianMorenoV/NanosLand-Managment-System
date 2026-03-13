/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.persistencia.repository;

import com.mycompany.persistencia.dominio.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author skyro
 */
public interface PagoRepository extends JpaRepository<Pago, Long>{
    
}
