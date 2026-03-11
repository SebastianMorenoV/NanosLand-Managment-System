package com.mycompany.persistencia.DAO;


import jakarta.persistence.EntityManager;
import java.util.List;

public abstract class GenericDAO<T> {
    
    private final Class<T> entityClass;
    protected EntityManager em;

    public GenericDAO(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.em = ConexionJPA.getInstance();
    }

    public void guardar(T entity) {
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
    }

    public void actualizar(T entity) {
        em.getTransaction().begin();
        em.merge(entity);
        em.getTransaction().commit();
    }

    public void eliminar(Long id) {
        T entity = buscarPorId(id);
        if (entity != null) {
            em.getTransaction().begin();
            em.remove(entity);
            em.getTransaction().commit();
        }
    }

    public T buscarPorId(Long id) {
        return em.find(entityClass, id);
    }

    public List<T> buscarTodos() {
        return em.createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e", entityClass)
                 .getResultList();
    }
}