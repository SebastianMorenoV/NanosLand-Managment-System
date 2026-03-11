package com.mycompany.persistencia.DAO;


import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class ConexionJPA {
    private static EntityManagerFactory emf;
    private static EntityManager em;

    public static EntityManager getInstance() {
        if (emf == null) {
            emf = Persistence.createEntityManagerFactory("NanosLandPU");
            em = emf.createEntityManager();
        }
        return em;
    }

    public static void cerrar() {
        if (em != null && em.isOpen()) em.close();
        if (emf != null && emf.isOpen()) emf.close();
    }
}