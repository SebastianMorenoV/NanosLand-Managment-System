package com.mycompany.persistencia.repository;

import com.mycompany.persistencia.dominio.Descuento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DescuentoRepository extends JpaRepository<Descuento, Long> {

    /** Descuentos encendidos; las demás condiciones (días, fechas, paquetes) se revisan al cotizar. */
    List<Descuento> findByActivoTrue();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
