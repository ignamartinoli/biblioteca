package org.example.repositorios;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.example.modelos.Genero;

public class GeneroRepository {
    private final EntityManager em;

    public GeneroRepository(EntityManager em) { this.em = em; }

    public Genero findByNombre(String nombre) {
        TypedQuery<Genero> q = em.createQuery(
                "SELECT g FROM Genero g WHERE g.nombre = :n", Genero.class);
        q.setParameter("n", nombre);
        try { return q.getSingleResult(); }
        catch (NoResultException e) { return null; }
    }

    public Genero getOrCreate(String nombre) {
        Genero g = findByNombre(nombre);
        if (g != null) return g;
        g = new Genero(nombre);
        em.persist(g);
        return g;
    }
}