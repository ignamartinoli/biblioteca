package org.example.modelos;

import jakarta.persistence.*;

@Entity
@Table(
        name = "libro_genero",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_libro_genero", columnNames = {"libro_id", "genero_id"}
        )
)
public class LibroGenero {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "libro_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_lg_libro")
    )
    private Libro libro;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "genero_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_lg_genero")
    )
    private Genero genero;

    protected LibroGenero() {}

    public LibroGenero(Libro libro, Genero genero) {
        this.libro = libro;
        this.genero = genero;
    }

    public Long getId() { return id; }
    public Libro getLibro() { return libro; }
    public Genero getGenero() { return genero; }

    public void setLibro(Libro libro) { this.libro = libro; }
    public void setGenero(Genero genero) { this.genero = genero; }
}