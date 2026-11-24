package com.mycompany.persistencia.repository;

import com.mycompany.persistencia.dominio.PaquetePrecio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaquetePrecioRepository extends JpaRepository<PaquetePrecio, Long> {

    /** Nombres de los paquetes activos cuya tabla de precios usa la comida. */
    @Query("SELECT DISTINCT pp.paquete.nombre FROM PaquetePrecio pp "
         + "WHERE pp.comida.id = :comidaId AND pp.paquete.activo = true "
         + "ORDER BY pp.paquete.nombre")
    List<String> nombresPaquetesActivosConComida(@Param("comidaId") Long comidaId);
}
