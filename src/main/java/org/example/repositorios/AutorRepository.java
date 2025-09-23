package org.example.repositorios;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.example.modelos.Autor;

public class AutorRepository {
    private final EntityManager em;

    public AutorRepository(EntityManager em) { this.em = em; }

    public Autor findByNombre(String nombre) {
        TypedQuery<Autor> q = em.createQuery(
                "SELECT a FROM Autor a WHERE a.nombre = :n", Autor.class);
        q.setParameter("n", nombre);
        try { return q.getSingleResult(); }
        catch (NoResultException e) { return null; }
    }

    /** Devuelve el existente o crea uno nuevo si no hay */
    public Autor getOrCreate(String nombre) {
        Autor existente = findByNombre(nombre);
        if (existente != null) return existente;
        Autor a = new Autor(nombre);
        em.persist(a);
        return a;
    }
}