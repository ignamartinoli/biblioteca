package org.example.modelos;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "libro", uniqueConstraints = {
        @UniqueConstraint(name = "uk_libro_titulo_autor", columnNames = {"titulo", "autor_id"})
})
public class Libro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="titulo", nullable = false, length = 300)
    private String titulo;

    @ManyToOne(optional = false)
    @JoinColumn(name="autor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_libro_autor"))
    private Autor autor;

    @OneToMany(
            mappedBy = "libro",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<LibroGenero> libroGeneros = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private Estado estado;


    protected Libro() {}

    public Libro(String titulo, Autor autor, Estado estado) {
        this.titulo = titulo;
        this.autor = autor;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public Autor getAutor() { return autor; }
    public Estado getEstado() { return estado; }

    @Transient
    public Set<Genero> getGeneros() {
        return libroGeneros.stream()
                .map(LibroGenero::getGenero)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setAutor(Autor autor) { this.autor = autor; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public void addGenero(Genero genero) {
        boolean exists = libroGeneros.stream()
                .anyMatch(lg -> lg.getGenero().equals(genero));
        if (!exists) {
            LibroGenero lg = new LibroGenero(this, genero);
            libroGeneros.add(lg);
        }
    }

    public void removeGenero(Genero genero) {
        libroGeneros.removeIf(lg -> lg.getGenero().equals(genero));
    }

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