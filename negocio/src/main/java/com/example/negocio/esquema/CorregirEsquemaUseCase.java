package com.example.negocio.esquema;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Correcciones al esquema de la base que Hibernate no hace solo: con
 * {@code ddl-auto=update} agrega tablas y columnas nuevas, pero nunca cambia una
 * columna que ya existe. Se ejecutan al arrancar y no hacen nada si ya están aplicadas.
 */
@Service
public class CorregirEsquemaUseCase {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Al eliminar un usuario, sus ventas se conservan sin el dato de quién las registró.
     * En bases creadas antes de poder eliminar usuarios, {@code ventas.usuario_id} es
     * NOT NULL: aquí se vuelve opcional.
     *
     * @return {@code true} si hubo que cambiar la columna
     */
    @Transactional
    public boolean permitirVentasSinUsuario() {
        List<?> resultado = entityManager.createNativeQuery(
                "SELECT IS_NULLABLE FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ventas' AND COLUMN_NAME = 'usuario_id'")
                .getResultList();
        if (resultado.isEmpty() || !"NO".equalsIgnoreCase(String.valueOf(resultado.get(0)))) {
            return false;
        }
        entityManager.createNativeQuery("ALTER TABLE ventas MODIFY COLUMN usuario_id BIGINT NULL").executeUpdate();
        return true;
    }
}
