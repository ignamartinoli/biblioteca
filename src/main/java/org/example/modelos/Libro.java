package org.example.modelos;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "libro", uniqueConstraints = {
        // Un libro queda definido por Título + Autor
        @UniqueConstraint(name = "uk_libro_titulo_autor", columnNames = {"titulo", "autor_id"})
})
public class Libro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="titulo", nullable = false, length = 300)
    private String titulo;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name="autor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_libro_autor"))
    private Autor autor;

    @ManyToMany
    @JoinTable(
            name = "libro_genero",
            joinColumns = @JoinColumn(name="libro_id", foreignKey = @ForeignKey(name="fk_lg_libro")),
            inverseJoinColumns = @JoinColumn(name="genero_id", foreignKey = @ForeignKey(name="fk_lg_genero")),
            uniqueConstraints = @UniqueConstraint(name = "uk_libro_genero", columnNames = {"libro_id","genero_id"})
    )
    private Set<Genero> generos = new HashSet<>();

    protected Libro() {}

    public Libro(String titulo, Autor autor) {
        this.titulo = titulo;
        this.autor = autor;
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public Autor getAutor() { return autor; }
    public Set<Genero> getGeneros() { return generos; }

    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setAutor(Autor autor) { this.autor = autor; }

    public void addGenero(Genero g) { this.generos.add(g); }

    // Para evitar duplicados en colecciones en memoria
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Libro)) return false;
        Libro libro = (Libro) o;
        return Objects.equals(titulo, libro.titulo) &&
                Objects.equals(autor != null ? autor.getId() : null,
                        libro.autor != null ? libro.autor.getId() : null);
    }
    @Override public int hashCode() {
        return Objects.hash(titulo, autor != null ? autor.getId() : null);
    }
}