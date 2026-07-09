/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.persistencia.repository;

import com.mycompany.persistencia.dominio.CargoExtra;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 *
 * @author skyro
 */
public interface CargoExtraRepository extends JpaRepository<CargoExtra, Long>{
    List<CargoExtra> findByEventoId(Long eventoId);
}
