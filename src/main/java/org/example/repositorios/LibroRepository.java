package org.example.repositorios;

import org.example.modelos.Autor;
import org.example.modelos.Libro;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class LibroRepository {
    private final EntityManager em;

    public LibroRepository(EntityManager em) { this.em = em; }

    public Libro findByTituloYAutor(String titulo, Autor autor) {
        TypedQuery<Libro> q = em.createQuery(
                "SELECT l FROM Libro l WHERE l.titulo = :t AND l.autor = :a", Libro.class);
        q.setParameter("t", titulo);
        q.setParameter("a", autor);
        try { return q.getSingleResult(); }
        catch (NoResultException e) { return null; }
    }

    public Libro getOrCreate(String titulo, Autor autor) {
        Libro l = findByTituloYAutor(titulo, autor);
        if (l != null) return l;
        l = new Libro(titulo, autor);
        em.persist(l);
        return l;
    }

    public List<Libro> findByAutorNombre(String nombreAutor) {
        return em.createQuery(
                        "SELECT l FROM Libro l " +
                                "JOIN FETCH l.autor a " +
                                "LEFT JOIN FETCH l.generos g " +
                                "WHERE a.nombre = :nombreAutor", Libro.class)
                .setParameter("nombreAutor", nombreAutor)
                .getResultList();
    }
}
